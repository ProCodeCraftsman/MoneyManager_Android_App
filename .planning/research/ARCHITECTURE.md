# Architecture Research — Insights Dashboard

**Project:** MoneyManager v2.2 Insights Dashboard
**Researched:** 2026-04-28
**Confidence:** HIGH — based on direct codebase inspection

---

## Component Overview

### New Components to Create

| Component | Package | Type | Purpose |
|-----------|---------|------|---------|
| `InsightsScreen.kt` | `ui/screens/insights/` | Composable | Host — holds HorizontalPager + tab strip |
| `InsightsViewModel.kt` | `ui/screens/insights/` | HiltViewModel | Single ViewModel for all 3 panes |
| `StatusPane.kt` | `ui/screens/insights/` | Composable | Screen 1 — net position, totals |
| `RisksPane.kt` | `ui/screens/insights/` | Composable | Screen 2 — rule-based alerts |
| `TrendsPane.kt` | `ui/screens/insights/` | Composable | Screen 3 — expense %, dominant type, line chart |
| `InsightsCalculator.kt` | `ui/screens/insights/` | Pure object | All aggregation + risk rule logic |
| `InsightsUiState.kt` | `ui/screens/insights/` | Data classes | Single state type consumed by all 3 panes |
| `Screen.Insights` | `MoneyManagerNavHost.kt` | Sealed object | Nav route + bottom nav entry |

### Existing Components to Modify

| File | Change | Risk |
|------|--------|------|
| `MoneyManagerNavHost.kt` | Add `Screen.Insights` sealed object; add to `bottomNavScreens` list; add `composable(Screen.Insights.route)` block | Low — additive only |
| `TransactionDao.kt` | Already has `getTransactionsByDateRange` — no changes needed | None |
| `TransactionRepository` interface | Already has `getTransactionsByDateRange` — no changes needed | None |

No existing ViewModel, Repository, or DAO requires modification. The entire feature is additive.

---

## Data Flow

```
Room (transactions table)
  └─ TransactionDao.getTransactionsByDateRange(currStart, currEnd) → Flow<List<TransactionEntity>>
  └─ TransactionDao.getTransactionsByDateRange(prevStart, prevEnd) → Flow<List<TransactionEntity>>
       └─ InsightsViewModel
            └─ combine(currMonthFlow, prevMonthFlow, preferencesManager.currency)
                 └─ InsightsCalculator.compute(currTxs, prevTxs)
                      └─ InsightsUiState (StateFlow<InsightsUiState>)
                           ├─ StatusPane reads: netPosition, netCashFlow, totalIncome, totalExpense, totalSavings, monthLabel
                           ├─ RisksPane reads: riskAlerts list (max 3)
                           └─ TrendsPane reads: expenseChangePct, dominantActivity, dailyPoints list
```

**Key design decision:** A single `combine()` of two date-range flows fires once per Room emission, feeds one pure calculator, emits one `StateFlow<InsightsUiState>`. All three panes subscribe to the same StateFlow — no duplicate Room queries.

---

## ViewModel Strategy

**Use a single `InsightsViewModel`.**

Rationale grounded in the existing codebase:

1. All three panes consume data from the same two time windows (current month, previous month). Splitting into three ViewModels would spawn three independent Room query subscriptions for identical data — wasteful and creates synchronization risk where panes could briefly show inconsistent values.

2. The existing pattern in this codebase confirms one ViewModel per screen-feature. `DashboardViewModel` serves all `DashboardType` tabs (OVERVIEW, EXPENSE, INCOME, ACCOUNTS, SAVINGS, BUDGET, LENDING) from one `StateFlow<DashboardUiState>`. Insights is the same pattern at smaller scale.

3. `HiltViewModel` lifecycle is scoped to the `NavBackStackEntry` for `Screen.Insights`. One ViewModel survives pager swipes transparently; three separate ViewModels would each need to be hoisted to the same scope anyway, providing no structural benefit.

4. `InsightsUiState` carries all pane data as named fields. Panes destructure only what they need — no prop drilling or state coupling between panes.

**ViewModel constructor — minimal dependencies:**

```kotlin
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel()
```

No `Application` context needed. No account/budget/category/goal repositories needed — Insights is transaction-only by design.

---

## Calculation Layer

**Calculations belong in a dedicated `InsightsCalculator` object, not inline in the ViewModel.**

Rationale drawn from the existing codebase:

- `DashboardViewModel` mixes flow orchestration with inline calculation lambdas. The result is a 1000+ line file that is difficult to reason about and impossible to unit test without a running ViewModel. Insights must not repeat this.
- All Insights calculations are **pure functions**: given two `List<TransactionEntity>` inputs they return deterministic outputs. Pure functions belong outside the ViewModel.
- An `object InsightsCalculator` with pure methods is testable without a `CoroutineScope`, a `Context`, or an Android emulator.

**Shape of the calculator:**

```kotlin
object InsightsCalculator {

    fun compute(
        currTxs: List<TransactionEntity>,
        prevTxs: List<TransactionEntity>
    ): InsightsData { ... }

    private fun aggregate(txs: List<TransactionEntity>): MonthMetrics { ... }
    private fun evaluateRisks(curr: MonthMetrics, prev: MonthMetrics): List<RiskAlert> { ... }
    private fun buildDailyPoints(txs: List<TransactionEntity>): List<DailyPoint> { ... }
}

data class MonthMetrics(
    val income: Double,
    val expense: Double,
    val savings: Double,
    val lending: Double,   // sum of type="lend"
    val borrowing: Double  // sum of type="borrow"
)

data class InsightsData(
    val netPosition: Double,      // income - expense - savings + borrowing - lending
    val netCashFlow: Double,      // income - expense
    val totalIncome: Double,
    val totalExpense: Double,
    val totalSavings: Double,
    val expenseChangePct: Double, // ((curr - prev) / prev) * 100; guard against prev=0
    val dominantType: String,     // type label with highest total amount
    val dominantAmount: Double,
    val riskAlerts: List<RiskAlert>,
    val dailyPoints: List<DailyPoint>,
    val isEmpty: Boolean          // true when both months have zero transactions
)

data class RiskAlert(val severity: AlertSeverity, val title: String, val explanation: String)
data class DailyPoint(val dayOfMonth: Int, val income: Double, val expense: Double)
```

**ViewModel orchestration — calculator is called inside the combine lambda:**

```kotlin
private val currMonthFlow = transactionRepository.getTransactionsByDateRange(currStart, currEnd)
private val prevMonthFlow = transactionRepository.getTransactionsByDateRange(prevStart, prevEnd)

val uiState: StateFlow<InsightsUiState> = combine(
    currMonthFlow, prevMonthFlow, preferencesManager.currency
) { curr, prev, currency ->
    val data = InsightsCalculator.compute(curr, prev)
    InsightsUiState(data = data, currency = currency, isLoading = false)
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InsightsUiState(isLoading = true))
```

**Transaction type mapping** — `TransactionEntity.type` uses: `income`, `expense`, `savings`, `transfer`, `lend`, `receive`, `borrow`, `repay`. Milestone spec terms map as:

| Spec Term | Entity Type(s) |
|-----------|---------------|
| INCOME | `"income"` |
| EXPENSE | `"expense"` |
| SAVINGS | `"savings"` (consider whether `"investment"` is included — confirm before implementing) |
| LENDING | `"lend"` (outflow); `"receive"` is repayment inflow — net = lend - receive |
| BORROWING | `"borrow"` (inflow as debt); `"repay"` is repayment outflow — net = borrow - repay |

---

## Navigation Integration

### Adding the Insights Tab

The bottom nav currently has 3 items: Dashboard, Transactions, Settings (lines 223-226 in `MoneyManagerNavHost.kt`).

Add Insights as the **4th item**, positioned between Transactions and Settings:

```kotlin
// Sealed class addition
data object Insights : Screen("insights", "Insights", Icons.Default.Lightbulb)

// bottomNavScreens update
val bottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Transactions,
    Screen.Insights,   // new
    Screen.Settings
)

// composable block
composable(Screen.Insights.route) {
    InsightsScreen(viewModel = hiltViewModel())
}
```

**Icon:** `Icons.Default.Lightbulb` is available via `material-icons-extended` (already in `build.gradle.kts` line 84). Fallback option: `Icons.Default.TrendingUp` or `Icons.Default.Analytics`.

No deep-link parameters are needed — Insights always shows the current month.

Insights is a **leaf destination** — no internal navigation from the panes. The back stack behavior matches other bottom nav items (`launchSingleTop = true`, `restoreState = true`).

---

## HorizontalPager / Swipe Implementation

**Use `HorizontalPager` from `androidx.compose.foundation.pager`.**

This API is included in the Compose BOM `2024.12.01` via the `foundation` artifact. The project already declares `androidx.compose.ui:ui` and `material3` in the BOM block — `foundation` is a transitive dependency of both, so no new `build.gradle.kts` entry is needed.

**Implementation pattern:**

```kotlin
@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            listOf("Status", "Risks", "Trends").forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> StatusPane(uiState = uiState)
                1 -> RisksPane(uiState = uiState)
                2 -> TrendsPane(uiState = uiState)
                else -> Unit
            }
        }
    }
}
```

**Why `HorizontalPager` + `TabRow`:**

- `TabRow` + `HorizontalPager` is the standard Material 3 pattern for swipeable tab content in Compose — used throughout Material 3 documentation and consistent with this codebase's Material 3 usage.
- Bidirectional sync: `pagerState.currentPage` drives `TabRow` selection; `Tab.onClick` drives `pagerState.animateScrollToPage()`. No manual sync state needed.
- 3 fixed pages with no dynamic tab count — `rememberPagerState(pageCount = { 3 })` is sufficient.
- Custom swipe implementation adds no value at 3 fixed pages.

**Vertical scrolling within panes:** StatusPane (5 numbers + label) and RisksPane (up to 3 alert cards) fit comfortably in one screen without scrolling. TrendsPane has a line chart plus 2 summary cards — wrap in `Column` with `verticalScroll` only if the chart requires a minimum height that causes overflow on small screens.

---

## Build Order

Build in this sequence — each step compiles independently of the next:

**Step 1 — Pure data and logic (no Android runtime required)**
- `InsightsUiState.kt`: define `InsightsUiState`, `InsightsData`, `MonthMetrics`, `RiskAlert`, `DailyPoint`
- `InsightsCalculator.kt`: implement all 5 aggregation formulas and 5 risk rules as a pure `object`
- *Why first:* these have zero Android dependencies, zero Compose dependencies, and define the contract everything else depends on

**Step 2 — ViewModel (depends on Step 1 + existing Repository/Preferences)**
- `InsightsViewModel.kt`: wire two `getTransactionsByDateRange` flows, combine, call calculator, emit StateFlow
- *Why second:* ViewModel is the glue between data layer and UI; must exist before screen can receive state

**Step 3 — Navigation plumbing (depends on Step 2 for `hiltViewModel()`)**
- Add `Screen.Insights` to sealed class in `MoneyManagerNavHost.kt`
- Add `Screen.Insights` to `bottomNavScreens`
- Add `composable(Screen.Insights.route)` with a temporary placeholder `InsightsScreen`
- *Why third:* confirms the tab appears and ViewModel is correctly injected before any pane UI exists

**Step 4 — InsightsScreen shell (depends on Step 3)**
- `InsightsScreen.kt`: `HorizontalPager` + `TabRow` wired to `pagerState`, panes as empty `Box` placeholders
- Validates swipe behavior and tab sync end-to-end

**Step 5 — StatusPane (depends on Step 1 for state shape)**
- `StatusPane.kt`: pure layout, no chart, net position as hero number, 4 summary rows below
- Simplest pane — good integration smoke test

**Step 6 — RisksPane (depends on Step 1 calculator risk rules being finalized)**
- `RisksPane.kt`: renders `uiState.data.riskAlerts` list (max 3) as `Card` items with icon + title + explanation
- Empty state: "No risks detected this month"

**Step 7 — TrendsPane (depends on Step 1 `DailyPoint` shape + MPAndroidChart)**
- `TrendsPane.kt`: expense change % card, dominant activity card, `LineChart` in `AndroidView`
- Reuse the `AndroidView` + `LineChart` pattern from `AccountComparisonChart.kt`
- `DailyPoint` list → `Entry(dayOfMonth.toFloat(), expense.toFloat())` for the expense line; repeat for income line

---

## Key Constraints Confirmed by Codebase Inspection

- `getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>` exists in both DAO (line 14) and Repository interface (line 12) and impl (line 31). No new DAO query needed.

- The project uses `collectAsStateWithLifecycle()` (from `lifecycle-runtime-compose:2.10.0`) — use this, not `collectAsState()`, consistent with `DashboardScreen.kt` line 39.

- `PreferencesManager.currency` is a `Flow<String>` — include it in the `combine()` so currency symbol updates reach Insights without restart.

- No new Hilt module entries needed. `TransactionRepository` is bound in `RepositoryModule`; `PreferencesManager` is provided in `PreferencesModule`. `InsightsViewModel` uses `@Inject constructor` + `@HiltViewModel` — Hilt picks it up automatically.

- `DashboardViewModel` already calculates identical metrics (income, expense, savings, lending, borrowing, period-over-period deltas) from `getAllTransactions()` filtered in Kotlin. Insights should use `getTransactionsByDateRange()` with explicit date bounds instead — this is more efficient (Room-level filter vs full table load) and appropriate since Insights has a fixed two-month window.

- `TransactionEntity.isSplitChild` flag exists — exclude split children (`!tx.isSplitChild`) from all aggregations, consistent with how `DashboardViewModel` handles them.

---

## Sources

- Direct codebase inspection (HIGH confidence):
  - `TransactionDao.kt` — confirmed `getTransactionsByDateRange` signature
  - `TransactionRepositoryImpl.kt` — confirmed delegate pattern
  - `TransactionRepository.kt` (interface) — confirmed method availability
  - `DashboardViewModel.kt` — confirmed flow orchestration patterns, calculation patterns, `isSplitChild` exclusion
  - `MoneyManagerNavHost.kt` — confirmed bottom nav structure, sealed class pattern, composable block pattern
  - `TransactionEntity.kt` — confirmed `type` string values and `isSplitChild` field
  - `build.gradle.kts` — confirmed Compose BOM 2024.12.01, MPAndroidChart v3.1.0, material-icons-extended
  - `RepositoryModule.kt` — confirmed Hilt bindings, no new modules needed
  - `AccountComparisonChart.kt` — confirmed MPAndroidChart + AndroidView pattern exists in project
