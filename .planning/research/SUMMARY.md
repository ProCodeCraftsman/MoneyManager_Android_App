# Research Summary — Insights Dashboard v2.2

**Synthesized:** 2026-04-28
**Sources:** STACK.md, FEATURES.md, ARCHITECTURE.md, PITFALLS.md
**Overall Confidence:** HIGH — all four research files are grounded in direct codebase inspection of TransactionDao.kt, DashboardViewModel.kt, MoneyManagerNavHost.kt, TransactionEntity.kt, and build.gradle.kts.

---

## Executive Summary

The Insights Dashboard is a 3-screen swipeable feature (STATUS / RISKS / TRENDS) that surfaces current-month financial health. The full feature requires zero new gradle dependencies — everything needed (Room, Hilt, Compose Foundation HorizontalPager, MaterialTheme) is already on the classpath via the Compose BOM 2024.12.01. The only genuinely new component is a custom Canvas-based daily line chart for the TRENDS screen, which is 30-50 lines of Kotlin and stays fully within the existing pure-Compose drawing pattern the codebase has already established.

The architecture is straightforward and additive: one new HiltViewModel (InsightsViewModel) backed by two scoped date-range Room queries, one pure calculation object (InsightsCalculator) that is fully unit-testable without Android runtime, and three pane composables hosted inside a HorizontalPager with a TabRow. No existing ViewModel, DAO, Repository, or navigation route requires modification — Insights is inserted as the 4th bottom nav item.

The critical risk area is data correctness, not technical complexity. The existing codebase has 7 active persistent subscribers to getAllTransactions(), and a naive InsightsViewModel implementation would add an 8th. All aggregation logic must use getTransactionsByDateRange() with explicit month bounds, filter out split children, filter by explicit type inclusion (not exclusion), and guard against division-by-zero when previous month data is absent. These are not hypothetical risks — every one is a demonstrated pattern in the existing DashboardViewModel code.

---

## Stack Additions

### What is new

| Component | Role | Notes |
|-----------|------|-------|
| Custom Canvas composable (DailyLineChart) | TRENDS daily line chart | 30-50 lines; no new dependency; matches existing AccountComparisonChart pattern |
| HorizontalPager + PagerState | 3-screen swipe layout | Ships with androidx.compose.foundation already on classpath via BOM — no gradle change |

### What is reused (no change needed)

| Component | Where Reused |
|-----------|-------------|
| TransactionDao.getTransactionsByDateRange() | Primary data source for both current and previous month |
| java.util.Calendar + SimpleDateFormat | Month boundary calculation and month label formatting |
| PreferencesManager.currency Flow | Currency symbol in all money displays |
| collectAsStateWithLifecycle() | ViewModel state collection in InsightsScreen |
| MaterialTheme.colorScheme | All color decisions — positive/negative indicators, alert severity tints |
| Hilt @HiltViewModel + @Inject constructor | No new Hilt module entries needed |

MPAndroidChart is declared in build.gradle.kts but must NOT be used. It has no existing usage in the codebase, is in maintenance-only mode (last release 2021), and requires an AndroidView wrapper that cannot inherit MaterialTheme colors. Use the custom Canvas composable instead.
---

## Feature Table Stakes (must-have)

### STATUS Screen

| Feature | Complexity | Risk |
|---------|------------|------|
| Hero number: Net Position (income - expense), visually dominant | Low | None — same formula as existing overviewFlow |
| Positive/negative color coding on hero (primary vs error) | Low | None |
| Current month label adjacent to hero (e.g. April 2026) | Low | Reuse SimpleDateFormat(MMMM yyyy) |
| Four supporting figures: Income, Expense, Savings, Net Cash Flow | Low | All must share the same month boundary |
| Currency symbol on all amounts from PreferencesManager.currency | Low | None — already flows through DashboardUiState |
| Empty state when no current-month transactions | Low | Show No activity for [Month] — do not show all-zeros screen |
| Month-to-month delta below each supporting figure | Low | Guard divide-by-zero when prev month is zero; show N/A not +100% |
| Savings rate (savings / income %) | Low | Guard divide-by-zero when income = 0 |

Anti-features to avoid: budget progress bars (no budgets dependency), tappable editable figures, animated count-up, mixed time periods, Net Worth as the hero (already on Dashboard).

### RISKS Screen

| Feature | Complexity | Risk |
|---------|------------|------|
| Each alert: icon + title + 1-2 line explanation with embedded triggering value | Low | Explanation must include the computed number |
| Negative alerts ordered before positive alerts (WARNING before INFO) | Low | None |
| Explicit No issues detected empty state — not a blank screen | Low | None |
| Maximum 3 alerts displayed (sortedByDescending severity .take(3)) | Low | None |
| Deterministic output — same data always produces same alerts | Low | No randomness in rule evaluation |
| Severity-tinted card background (errorContainer for WARNING, tertiaryContainer for INFO) | Low | None |

The 5 rules (all derived from transaction type + amount + date only):

| Rule | Trigger | Severity |
|------|---------|---------|
| Overspending | currentExpense > currentIncome | WARNING |
| Spending spike | currentExpense > prevExpense * 1.20 | WARNING |
| High borrowing | SUM(borrow) > currentIncome * 0.50 | WARNING |
| Negative net position | currentIncome - currentExpense < 0 | WARNING — consolidate with Overspending (same signal) |
| Savings improvement | currentSavings > prevSavings AND no WARNINGs | INFO |

Rules must be skipped when !hasEnoughHistory (no previous month transactions). Anti-features: swipeable-to-dismiss, push notifications, CTA buttons on alert cards, more than 5 rule types.

### TRENDS Screen

| Feature | Complexity | Risk |
|---------|------------|------|
| Expense % change vs previous month with directional indicator | Low | Guard prevExpense = 0 — show N/A or First month, not +Infinity% |
| Dominant activity: type name + total amount (exclude transfer, repay, receive) | Low | In-memory groupBy + maxByOrNull |
| Daily line chart (income + expense series, current month) | Medium | Chart must handle sparse early-month data; missing days filled with 0.0 |
| Month label as chart title | Low | None |
| Empty state hides chart when no transactions exist | Low | Guard: if all dailyPoints are zero, show placeholder |

Anti-features: pie chart (Dashboard already has one), MPAndroidChart for the chart, month navigation (lock to current month), stacked bars with categories (violates transaction-only constraint).
---

## Architecture Decisions

**Decision 1: Single InsightsViewModel, not one per pane.**
All three panes consume the same two time windows. Splitting spawns three independent Room subscriptions for identical data and risks panes briefly showing inconsistent values. DashboardViewModel confirms the pattern — it serves 7 dashboard types from one ViewModel.

**Decision 2: InsightsCalculator as a separate pure object.**
All aggregation and risk logic lives in object InsightsCalculator, not inline in the ViewModel. The existing DashboardViewModel mixes flow orchestration with inline lambdas in a 1000+ line file that cannot be unit tested without Android runtime. InsightsCalculator is a pure function of two List<TransactionEntity> inputs that returns InsightsData — testable without Hilt, coroutines, or an emulator.

**Decision 3: getTransactionsByDateRange(), not getAllTransactions().**
InsightsViewModel must never call getAllTransactions(). Seven active persistent subscribers already exist. getTransactionsByDateRange(prevMonthStart, currentMonthEnd) hits the indexed date column and re-emits only on mutations within the relevant window.

**Decision 4: Decompose UiState into per-pane sub-states.**
InsightsUiState contains StatusUiState, RisksUiState, and TrendsUiState as named sub-states. Each pane composable accepts only its sub-state, enabling Compose structural equality to skip recomposition for unchanged panes.

**Decision 5: Custom Canvas composable for the line chart.**
MPAndroidChart has never been used in this codebase, requires AndroidView interop, and cannot inherit MaterialTheme colors. The TRENDS chart is a static 28-31 point dual-series display — within the range of a 30-50 line Canvas composable. Zero new dependencies.

**Decision 6: Insights as the 4th bottom nav item.**
Insights is a distinct navigation destination (between Transactions and Settings), not a sub-tab of Dashboard. It has its own ViewModel scope, fixed time context (always current month), and internal 3-pane navigation.

Component map:

| Component | Package | Type |
|-----------|---------|------|
| InsightsScreen.kt | ui/screens/insights/ | Composable host — HorizontalPager + TabRow |
| InsightsViewModel.kt | ui/screens/insights/ | HiltViewModel |
| StatusPane.kt | ui/screens/insights/ | Composable |
| RisksPane.kt | ui/screens/insights/ | Composable |
| TrendsPane.kt | ui/screens/insights/ | Composable |
| InsightsCalculator.kt | ui/screens/insights/ | Pure object |
| InsightsUiState.kt | ui/screens/insights/ | Data classes |
| DailyLineChart.kt | ui/components/ | Reusable Canvas composable |
| MoneyManagerNavHost.kt | existing | Add Screen.Insights route — additive only |
---

## Top Pitfalls to Avoid

**Pitfall 1: getAllTransactions() as data source (CRITICAL)**
Do not call getAllTransactions() in InsightsViewModel. Seven active persistent subscribers already exist. Every transaction mutation fires O(n) re-emission to all subscribers simultaneously. Use getTransactionsByDateRange(prevMonthStart, currentMonthEnd) — it hits the indexed date column and re-emits only on mutations within the date window. The DAO already has this method.

**Pitfall 2: Division by zero in percentage calculations (HIGH)**
Four places divide by a previous-month value that can legally be zero: expense % change, income % change, savings rate, and the high-borrowing risk rule. The existing calculatePeriodSummary returns 100.0 when previous is zero — this misleads users. Use a sealed class PercentageChange with Known(value: Double) and Unavailable cases. When previous month is zero, display N/A or First month. Guard the high-borrowing rule with if (currentIncome > 0).

**Pitfall 3: Split child double-counting (HIGH)**
A split transaction creates one parent row (full amount) and N child rows (partial amounts). Summing without filtering counts both, overstating all monthly totals and misfiring risk rules. Every aggregation in InsightsCalculator must begin with .filter { !it.isSplitChild }. This is the existing pattern in DashboardViewModel throughout.

**Pitfall 4: Transfer transactions inflate income and expense (HIGH)**
Every internal transfer creates two rows: one OUT, one IN. A naive sum treats these as both income and expense. Use explicit type inclusion for every aggregate — for example, filter to type == income before summing income totals. Do not use negation filters — they break when new types are added.

**Pitfall 5: Risk rules misfire with no previous month history (HIGH)**
When the user has less than one full month of history, prevMonthExpense = 0.0 and prevMonthIncome = 0.0. Comparison-based rules produce false alarms: any current expense is a spending spike, any borrowing exceeds 50% of zero income. Compute hasEnoughHistory = prevMonthTxs.isNotEmpty(), expose it in InsightsUiState, and skip all comparison-based rules when it is false. Show: Insights improve after your first full month.

Additional documented pitfalls (see PITFALLS.md for full detail):
- UTC vs local day for daily chart grouping: use Calendar.get(DAY_OF_MONTH), not epoch division
- HorizontalPager pageCount must be hardcoded as 3, not derived from state
- Nested vertical scroll: use Column (not LazyColumn) inside pager pages to avoid scroll interop conflicts
- AndroidView update lambda runs on every recomposition: memoize chart data with remember(key = entries.hashCode())
- Single-entry LineDataSet with CUBIC_BEZIER mode crashes: use Mode.LINEAR, pad to minimum 2 entries
---

## Open Questions (need decisions before coding)

**Q1: Does SAVINGS include investment type transactions?**
VALID_TYPES includes savings but the presence of an investment type in production data is unconfirmed. Confirm before writing InsightsCalculator.aggregate().

**Q2: Net Position formula — include lend/borrow or not?**
ARCHITECTURE.md proposes income - expense - savings + borrowing - lending. FEATURES.md uses income - expense. These produce different numbers. Confirm the canonical formula before writing InsightsCalculator.

**Q3: Savings rate denominator when income = 0.**
The formula totalSavings / totalIncome is undefined when income = 0 but borrow > 0. Decide whether to show N/A when income = 0 or include borrowing in the denominator.

**Q4: Chart — custom Canvas or MPAndroidChart via AndroidView?**
STACK.md recommends custom Canvas. ARCHITECTURE.md and PITFALLS.md reference MPAndroidChart (pitfalls M1-M5). This is an inconsistency. Confirm before Phase 7. Custom Canvas is the recommended path.

**Q5: Consolidate overspending and negative net position into one rule?**
FEATURES.md notes these are the same signal and recommends one alert. With a 3-alert cap, emitting both wastes one slot. Confirm: implement as one rule.

---

## Recommended Build Order

**Phase 1 — Data contracts and pure logic (no Android runtime required)**
Create InsightsUiState.kt (all data classes: InsightsUiState, StatusUiState, RisksUiState, TrendsUiState, InsightsData, MonthMetrics, RiskAlert, DailyPoint, PercentageChange sealed class) and InsightsCalculator.kt (pure object with all aggregation formulas and 5 risk rules). Write unit tests covering split child filtering, transfer exclusion, division-by-zero guards, January/December rollover, hasEnoughHistory flag behavior. Zero Android dependencies — establishes the contract everything else depends on.

**Phase 2 — ViewModel (depends on Phase 1 + existing Repository)**
Create InsightsViewModel.kt: inject TransactionRepository and PreferencesManager; wire two getTransactionsByDateRange flows; combine with currency flow; call InsightsCalculator.compute(); emit StateFlow<InsightsUiState> with .flowOn(Dispatchers.Default) before stateIn. Expose hasEnoughHistory in state.

**Phase 3 — Navigation plumbing (depends on Phase 2)**
Add Screen.Insights sealed object to MoneyManagerNavHost.kt; add to bottomNavScreens list; add composable block with temporary placeholder. Verify tab appears, ViewModel correctly Hilt-injected, back stack matches other bottom nav items.

**Phase 4 — InsightsScreen shell (depends on Phase 3)**
Create InsightsScreen.kt with HorizontalPager + TabRow wired to pagerState; panes as empty Box placeholders. Validate swipe behavior, tab sync, and ViewModel state collection. Hardcode pageCount = 3.

**Phase 5 — StatusPane (depends on Phase 1 state shape)**
Create StatusPane.kt: hero number with positive/negative color, month label, four supporting figures with month-over-month deltas, savings rate, currency symbol, empty state. Simplest pane — good integration smoke test.

**Phase 6 — RisksPane (depends on Phase 1 risk rules being finalized)**
Create RisksPane.kt: renders RisksUiState.alerts (max 3) as Card items with icon + title + explanation; severity-tinted backgrounds; No issues detected empty state; Not enough history disclaimer when !hasEnoughHistory.

**Phase 7 — TrendsPane + DailyLineChart (depends on Phase 1 DailyPoint shape)**
Create DailyLineChart.kt as a reusable Canvas composable (income + expense series, MaterialTheme colors). Create TrendsPane.kt: expense change % card, dominant activity card, DailyLineChart, empty state when no transactions. Pad DailyPoint list to full month length (missing days = 0.0). Guard empty list before rendering chart.

---

## Confidence Assessment

| Area | Confidence | Basis |
|------|------------|-------|
| Stack | HIGH | Direct inspection of build.gradle.kts and all relevant source files |
| Architecture | HIGH | Direct inspection of TransactionDao.kt, DashboardViewModel.kt, MoneyManagerNavHost.kt, TransactionEntity.kt |
| Features | MEDIUM-HIGH | Codebase analysis for data compatibility; UX conventions from training data |
| Pitfalls | HIGH | Every pitfall grounded in specific line numbers in existing source files |
| Chart recommendation | HIGH | STACK.md and PITFALLS.md converge on Canvas; ARCHITECTURE.md references MPAndroidChart — inconsistency flagged in Q4 |

Gaps requiring resolution before implementation: investment type confirmation (Q1), Net Position formula (Q2), chart approach final decision (Q4), overspending rule consolidation (Q5).

---

## Sources (aggregated)

- TransactionDao.kt — getTransactionsByDateRange signature, getAllTransactions subscription count
- TransactionRepositoryImpl.kt — delegate pattern confirmed
- DashboardViewModel.kt — flow orchestration patterns, calculatePeriodSummary, isSplitChild exclusion, getDateRangeForFilter, 7 active getAllTransactions subscribers
- MoneyManagerNavHost.kt — bottom nav structure, sealed class pattern
- TransactionEntity.kt — type string values, isSplitChild/isSplitParent fields, VALID_TYPES
- build.gradle.kts — Compose BOM 2024.12.01, MPAndroidChart v3.1.0, material-icons-extended
- AccountComparisonChart.kt — pure Compose drawing pattern confirmed; MPAndroidChart not used in project
- RepositoryModule.kt — Hilt bindings confirmed; no new modules needed
- PreferencesManager.kt — currency as Flow<String>
- Material Design 3 guidelines (training data) — HorizontalPager + TabRow pattern, color roles
- Finance app UX conventions (training data) — hero number prominence, color coding, alert ordering
