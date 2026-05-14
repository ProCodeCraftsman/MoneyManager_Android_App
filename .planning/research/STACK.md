# Stack Research — Insights Dashboard

**Project:** MoneyManager — v2.2 Insights Dashboard
**Researched:** 2026-04-28
**Overall Confidence:** HIGH (based on direct codebase inspection + training knowledge of stable APIs)

---

## Existing Stack (Relevant to Insights Dashboard)

Everything the Insights Dashboard needs is already in the project. The table below lists only what will be actively used, based on direct inspection of build.gradle.kts and source files.

| Component | Version | Role in Insights |
|-----------|---------|-----------------|
| Jetpack Compose BOM | 2024.12.01 | All three screen UIs |
| Material Design 3 | via BOM | Typography, color system, icons |
| Room + room-ktx | 2.8.4 | `getAllTransactions()` Flow source |
| Hilt | 2.59.2 | InsightsViewModel injection |
| lifecycle-viewmodel-compose | 2.10.0 | `collectAsStateWithLifecycle` in screens |
| navigation-compose | 2.9.7 | Adding Insights as a destination |
| Paging 3 | 3.3.0 | NOT needed — Insights loads full month, not paginated |
| java.util.Calendar | stdlib | Date range calculation (already used in DashboardViewModel) |
| SimpleDateFormat | stdlib | Month label formatting (already used in DashboardViewModel) |

**Critical codebase finding:** MPAndroidChart (com.github.PhilJay:MPAndroidChart:v3.1.0) is declared in build.gradle.kts but is **not used anywhere in the codebase**. All existing charts are implemented as pure Compose composables — `AccountComparisonChart` uses Box/Column/fillMaxHeight proportional layout. `PieChartEntry` is a plain data class with no MPAndroidChart import. The project has de facto standardized on custom Compose drawing, not MPAndroidChart.

---

## Recommended Additions

**None.** Zero new library additions are needed.

| Capability | Solution | Source |
|------------|----------|--------|
| Daily line chart (TRENDS screen) | Custom `Canvas`-based Compose composable | Pure Compose, already proven in this codebase |
| Financial calculations (net position, % change, dominant activity) | Pure Kotlin arithmetic | Same pattern as `overviewFlow` and `getMetrics()` in DashboardViewModel |
| Month boundary dates | `java.util.Calendar` + existing `getDateRangeForFilter()` | Already implemented, reusable |
| Month label ("April 2026") | `SimpleDateFormat("MMMM yyyy")` | Already used in `getFilterDisplayDate()` |
| Rule-based alerts (RISKS screen) | Kotlin `when`/`if` logic | No library needed |
| Swipeable 3-screen layout | `HorizontalPager` from `androidx.compose.foundation.pager` | Already in Compose BOM 2024.12.01 |

`HorizontalPager` is the only component that may be new to the implementation team, but it ships with the Compose Foundation library already declared via the BOM — no gradle line required.

---

## Charting Decision

**Recommendation: Custom Canvas composable. Do not use MPAndroidChart for the line chart.**

### MPAndroidChart (v3.1.0 — already declared)

**Verdict: Do not use for this feature.**

- Last release was 2021. The library is in maintenance-only mode with no active development (HIGH confidence — training knowledge confirmed by version in deps being 3.1.0, unchanged for years).
- Requires wrapping in `AndroidView {}` inside a Compose composable. This creates an impedance mismatch: theming must be done imperatively via MPAndroidChart's Java API rather than declaratively via MaterialTheme.
- Cannot inherit `MaterialTheme.colorScheme` colors, `MaterialTheme.typography` text styles, or respond to dark/light mode without manual bridging.
- The line chart needed for TRENDS screen is a simple 28–31 data point dual-series chart (income + expense by calendar day). This is not complex enough to justify a third-party charting library's overhead.
- It is already in the project but **never used** — introducing it now would be a regression against the established pure-Compose pattern.

### Vico (patrykandpatrick/vico)

**Verdict: Worth knowing about, but not needed here.**

- Vico is a Compose-native charting library that supports full Material You theming. It is the leading Compose-native chart library as of training cutoff (Aug 2025).
- It would be the correct choice if the chart requirements were complex: interactive tooltips, animated transitions, scroll, zoom, logarithmic scales.
- The Insights TRENDS chart needs none of these. It is a static, read-only line chart showing daily totals for the current month. Custom Canvas is 30–50 lines of Kotlin and has zero additional APK size.
- If chart requirements grow significantly in a future milestone, Vico is the right addition at that point.

### YCharts (co.yml:ycharts)

**Verdict: Avoid.**

- Previous STACK.md suggested YCharts 2.1.0. However, YCharts is a smaller community library with narrower maintenance bandwidth compared to Vico. For a line chart, it offers no meaningful advantage over a Canvas composable. Do not add it.

### Custom Canvas Composable (Recommended)

The codebase already demonstrates competency with custom Compose drawing. `AccountComparisonChart` implements a proportional bar chart using only `Box`, `fillMaxHeight()`, and `weight()`. A daily line chart uses the same principle with `Canvas`, `drawLine()`, and coordinate math.

**Implementation sketch:**

```kotlin
@Composable
fun DailyLineChart(
    incomePoints: List<Float>,   // index = day-of-month - 1
    expensePoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    Canvas(modifier = modifier) {
        // normalize to canvas height, draw polylines
    }
}
```

MaterialTheme colors flow in naturally. No `AndroidView`, no interop, no extra dependency.

---

## What NOT to Add

| Library | Why Not |
|---------|---------|
| MPAndroidChart (actual usage) | Already in deps but never used. Continues to not be used — pure Compose maintains the established pattern. |
| Vico | Justified only for interactive/animated charts. TRENDS chart is static and simple. |
| YCharts | Previous research artifact. Narrower ecosystem than Vico; no advantage over Canvas for this use case. |
| kotlinx-datetime | Overkill. The project uses `java.util.Calendar` + `SimpleDateFormat` consistently throughout DashboardViewModel. Introducing a second date abstraction creates inconsistency. |
| Any "financial math" library | All calculations are simple arithmetic: sums, percent change = `(current - prev) / prev * 100`. No library adds value here. |
| Paging 3 (for Insights) | Insights aggregates data, not pages through it. Load all transactions for current + previous month into memory — same pattern as `overviewFlow`. |

---

## Integration Notes

### Data Layer

`InsightsViewModel` should follow the exact pattern of `overviewFlow` in `DashboardViewModel`:

1. Inject `TransactionRepository`.
2. Collect `transactionRepository.getAllTransactions()` as a Flow.
3. Filter in-memory for current month (day 1 → last day) and previous month using `java.util.Calendar` — reuse `getDateRangeForFilter()` or extract it to a shared utility.
4. Emit a sealed `InsightsUiState` with STATUS/RISKS/TRENDS data classes.

The ViewModel does not need to reach into Room with a new DAO query. `getAllTransactions()` already returns the full dataset as a `Flow<List<TransactionEntity>>`. The filtering and aggregation happens in the ViewModel operator chain.

### UI Layer

The 3-screen swipeable layout maps directly to `HorizontalPager` + `PagerState`:

```kotlin
val pagerState = rememberPagerState(pageCount = { 3 })
HorizontalPager(state = pagerState) { page ->
    when (page) {
        0 -> StatusScreen(uiState.status)
        1 -> RisksScreen(uiState.risks)
        2 -> TrendsScreen(uiState.trends)
    }
}
```

`HorizontalPager` is in `androidx.compose.foundation` which is already on the classpath via the BOM.

### Transaction Type Mapping for Insights

Based on `TransactionEntity.VALID_TYPES = ["income", "expense", "savings", "transfer", "lend", "receive", "borrow", "repay"]`:

- **Net Position** = `income + receive + repay - expense - savings - lend - borrow`  
  (or simpler: sum of account balances via `accountRepository.getTotalBalance()` — already computed in DashboardViewModel)
- **Net Cash Flow** = `income - expense` (current month)
- **Total Income** = type == "income"
- **Total Expense** = type == "expense"
- **Total Savings** = type == "savings"
- **Dominant activity** = whichever of {income, expense, savings, lend, borrow} has the highest `.sumOf { it.amount }`

### Alerts Logic (RISKS screen)

All five rules from the spec are pure Kotlin comparisons on the aggregated values from STATUS. No library, no ML, no external data. Implement as a `List<InsightAlert>` computed from a single `fun computeAlerts(status: StatusData, prevStatus: StatusData): List<InsightAlert>` function in the ViewModel or a domain use case.

---

## Summary

The Insights Dashboard requires **zero new gradle dependencies**. The full feature is buildable from:
- Existing Room Flow (`getAllTransactions()`)
- Existing `java.util.Calendar` date math pattern
- Compose `HorizontalPager` (already on classpath via BOM)
- Custom `Canvas` composable for the line chart (30–50 lines, matches codebase pattern)
- Pure Kotlin arithmetic for all financial metrics

Confidence: HIGH for all decisions above. The existing stack evidence is derived from direct file inspection of build.gradle.kts and all relevant .kt source files.
