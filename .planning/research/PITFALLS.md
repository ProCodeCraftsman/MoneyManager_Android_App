# Pitfalls Research — Insights Dashboard

**Domain:** Analytics/Insights screen addition to existing Compose + Room MVVM app
**Researched:** 2026-04-28
**Confidence:** HIGH — all findings grounded in the actual codebase (TransactionDao.kt, DashboardViewModel.kt, TransactionsViewModel.kt, build.gradle.kts)

---

## Performance Pitfalls

### P1: Adding Another getAllTransactions() Fan-out — The Existing Problem Gets Worse

**What goes wrong:** `getAllTransactions()` is a `Flow<List<TransactionEntity>>` backed by `SELECT * FROM transactions ORDER BY date DESC`. Every insert/update/delete fires a Room invalidation, which re-emits the **entire** table to every subscriber. The existing codebase already subscribes to it 6 times concurrently inside `DashboardViewModel` alone (lines 368, 544, 608, 636, 709), plus once in `AccountsViewModel` and once in `BudgetsViewModel`. Adding a new `InsightsViewModel` that calls `getAllTransactions()` adds a 9th live subscriber. Each subscriber receives the full dataset on every mutation.

**Why it happens:** Room's `Flow<List<T>>` on a table-scan query is reactive to the entire table. Any row change — including inserting a new transaction — invalidates and re-emits to every active subscriber simultaneously.

**Consequences:**
- On a device with 2,000+ transactions, all 9 subscribers decode the full list from SQLite on every entry. That is O(n) work repeated 9 times per transaction mutation.
- The Insights aggregations (daily grouping for chart, current/prev month sums, 5 risk rules) run as `map {}` transforms in memory on the main thread or a background coroutine but still process the full list each time.
- Users who add a transaction will see a perceptible lag spike because all ViewModels recalculate simultaneously.

**Prevention:**
- Do NOT call `getAllTransactions()` in InsightsViewModel. The DAO already has `getTransactionsByDateRange(startDate, endDate)` which limits the emission to only the rows you need. For current + previous month, two separate calls or one combined query spanning `prevMonthStart` to `currentMonthEnd` subscribe to a smaller, indexed result.
- Room's `date` column is already indexed (`Index("date")` on `TransactionEntity`). The date-range query hits the index; `getAllTransactions()` does not.
- The `getTransactionsByDateRange` query re-emits only when a row within that date range changes, not on all mutations. This is the critical difference.
- Consider adding a single dedicated DAO query: `SELECT * FROM transactions WHERE date >= :prevMonthStart AND date <= :currentMonthEnd ORDER BY date DESC`. One subscription covers both months.

**Detection warning signs:** Slow UI after adding a transaction when the Insights tab is visible; Android Profiler shows sustained background thread work triggered by DB writes.

---

### P2: Aggregation Computed via In-Memory map{} on UI-Thread-Adjacent Coroutine

**What goes wrong:** The existing DashboardViewModel computes all period summaries inside `combine {}` and `map {}` blocks. Room's `Flow` emits on a background thread, but `combine {}` lambdas execute on whatever dispatcher the collector runs on. StateFlow collectors default to the main thread context unless `flowOn` is explicitly specified. If the aggregation logic (daily grouping for 31 days, 5 rule evaluations, percent-change calculations) runs synchronously in the collector, it blocks the main thread briefly.

**Prevention:**
- Perform all aggregation inside `.flowOn(Dispatchers.Default)` before `stateIn(...)`.
- Example pattern: `twoMonthTransactions.map { txs -> computeInsights(txs) }.flowOn(Dispatchers.Default).stateIn(viewModelScope, ...)`.
- Rule evaluation (5 rules, max 3 alerts) is cheap, but daily line-chart grouping (group by day of month, sum income and expense per day) iterates through up to 31 days x N transactions. Keep this in `Dispatchers.Default`.

---

### P3: Incorrect Use of combine{} With Too Many Flows in InsightsViewModel

**What goes wrong:** DashboardViewModel's `uiState` already combines 24 flows. If InsightsViewModel is incorrectly merged into DashboardViewModel, adding 2-4 more flows makes the 24-flow combine even more unstable (every source emission triggers a full recalculation across all 24+ inputs).

**Prevention:**
- InsightsViewModel must be a **separate ViewModel**, not merged into DashboardViewModel. This is the correct architectural choice.
- InsightsViewModel's own `uiState` should use at most 3 flows combined: two-month transaction range, a currency preference, and possibly a timestamp trigger for recalculation.

---

## HorizontalPager Pitfalls

### H1: rememberPagerState — Page Count Must Be Stable

**What goes wrong:** `HorizontalPager` requires a `pageCount` parameter. If `pageCount` is read from a variable that can change (e.g., derived from UI state), Compose may recreate the PagerState and reset the current page to 0 mid-interaction.

**Prevention:**
- Hardcode `pageCount = 3` as a constant. The Insights pager has exactly 3 fixed pages (STATUS, RISKS, TRENDS).
- Do not derive `pageCount` from data. Keep it a compile-time constant.

---

### H2: Nested Vertical Scroll Conflicts

**What goes wrong:** If any sub-page contains a `LazyColumn` or `verticalScroll` modifier inside a `HorizontalPager` that is itself inside a scrollable container, Compose's nested scroll interop breaks: vertical scroll gestures are consumed before the pager's horizontal gesture recognizer sees them.

**Prevention:**
- For RISKS (3 items max) and STATUS (no scroll needed): use `Column` not `LazyColumn`. 3 items is cheap; no scrollable container is needed.
- For TRENDS: if content exceeds screen height, wrap the inner page in `Column(modifier = Modifier.verticalScroll(rememberScrollState()))` but ensure the `HorizontalPager` is NOT itself inside another vertically-scrollable container.
- Never place `HorizontalPager` inside a `LazyColumn` item.

---

### H3: Recomposition of All Pages When One Page's Data Changes

**What goes wrong:** If `InsightsUiState` is one monolithic data class and STATUS/RISKS/TRENDS all read from it, a change to any field causes all three page composables to recompose even if only one page's data changed.

**Prevention:**
- Decompose the UiState into three sub-states: `StatusUiState`, `RisksUiState`, `TrendsUiState`. Each page composable accepts only its sub-state.
- This allows Compose's structural equality check on `@Stable` data classes to skip recomposition for pages whose sub-state reference did not change.
- Alternatively, use `key(page)` inside the pager content lambda so each page composable is independently keyed.

---

### H4: LaunchedEffect on Pager Pages Without Stable Keys

**What goes wrong:** A `LaunchedEffect(Unit)` inside a pager page composable whose identity is unstable will re-run on every recomposition of that page.

**Prevention:**
- No `LaunchedEffect` should be needed inside pager pages for this feature — all data comes from ViewModel StateFlow. Avoid it entirely.
- If a per-page side effect is needed later, key it `LaunchedEffect(pagerState.currentPage)` not `LaunchedEffect(Unit)`.

---

## Date/Time Pitfalls

### D1: Calendar.getInstance() + Device Timezone — Internally Consistent But Not Portable

**What goes wrong:** `TransactionEntity.date` is stored as `System.currentTimeMillis()` (epoch millis, timezone-neutral). `Calendar.getInstance()` uses the **device's local timezone** to compute month boundaries. This is internally consistent as long as all boundary calculations use the same Calendar approach — which the existing app does.

**The real risk:** Firebase-synced transactions from a device in a different timezone will have epoch values that correspond to different local calendar dates. A transaction recorded at 11pm on Dec 31 in UTC+5:30 is in January in UTC.

**Prevention:**
- Continue the existing `Calendar.getInstance()` pattern in InsightsViewModel. Do NOT switch to UTC-based boundaries while the rest of the app uses local time.
- Do NOT use `Instant.ofEpochMilli(tx.date).atZone(ZoneOffset.UTC).dayOfMonth` — this gives UTC day, not local day.
- If you use `java.time.LocalDate`, specify `ZoneId.systemDefault()` explicitly when converting from epoch.

---

### D2: January/December Edge Case in Previous Month Calculation

**What goes wrong:** `prevBaseDate.add(Calendar.MONTH, -1)` when current month is January correctly yields December of the previous year. `Calendar` handles year rollover correctly. However, manually cloning and mutating Calendar objects in the wrong order can produce subtle bugs (e.g., setting `DAY_OF_MONTH = 1` on a calendar before computing `getActualMaximum(Calendar.DAY_OF_MONTH)` for a different month).

**The existing DashboardViewModel handles this correctly** in `getDateRangeForFilter`. InsightsViewModel must not re-implement this logic from scratch.

**Prevention:**
- Extract `getDateRangeForFilter` to a shared utility object. InsightsViewModel calls the same utility.
- Add unit tests for January->December rollback and verifying `prevMonthEnd` is Dec 31 not Dec 30.

---

### D3: Daily Grouping for Line Chart — UTC vs Local Day

**What goes wrong:** For the TRENDS daily line chart, grouping transactions by day requires extracting the calendar day from a Long timestamp. A naive `timestamp / (24 * 60 * 60 * 1000)` gives a UTC day number, not a local day number. A transaction recorded at 11:30pm local time in UTC+5:30 will appear on the wrong day in the chart.

**Prevention:**
- Use `Calendar.getInstance().apply { timeInMillis = tx.date }.get(Calendar.DAY_OF_MONTH)` to get the local day of month.
- Alternatively: `SimpleDateFormat("d", Locale.getDefault()).format(Date(tx.date)).toInt()`.
- Group by this local day value, not by epoch-divided day.

---

### D4: Division by Zero in Percentage Change Calculations

**What goes wrong:** TRENDS screen shows "Expense change % vs previous month." If `prevMonthExpense == 0.0`, the formula divides by zero, producing `Double.POSITIVE_INFINITY` or `NaN`, which renders as "Infinity%" or "NaN%" in a Text composable.

**The existing DashboardViewModel has a mitigation in `calculatePeriodSummary` (line 704):** when prev=0 and current>0 it returns 100.0, which is a convention but can mislead. InsightsViewModel should be more explicit.

**Prevention:**
- Use a sealed class for percentage change: `sealed class PercentageChange { data class Known(val value: Double) : ...; object Unavailable : ... }`.
- When previous month is zero, show "N/A" or "First month" not "+100%".
- Never pass `Double.NaN` or `Double.POSITIVE_INFINITY` to a Text composable.
- Also guard income division: "high borrowing >50% of income" rule must check `income > 0` before dividing.

---

## Empty State Pitfalls

### E1: Zero Transactions — Distinct States Needed Per Screen

**What goes wrong:** When there are 0 transactions for the current month:
- STATUS: all values are 0.0 — displayable, not a crash.
- RISKS: all 5 rules evaluate to false — this is an "all clear" positive state, NOT a generic error.
- TRENDS: the daily line chart has an empty `List<Entry>` — MPAndroidChart with an empty `LineDataSet` renders a blank frame (if `setNoDataText` is configured) or crashes if entries list is null.

**Prevention:**
- Guard every chart construction: `if (entries.isEmpty()) { show placeholder composable } else { show AndroidView chart }`.
- RISKS with zero alerts needs a distinct "All clear" composable, not the same generic "No data" message used elsewhere.
- STATUS with all-zeros is fine to display with a subtle "No activity this month" subtitle.

---

### E2: Only One Month of Data — Comparison Rules Misfire

**What goes wrong:** If the user has only used the app for 3 weeks (current month only), `prevMonthIncome = 0.0` and `prevMonthExpense = 0.0`. Comparison-based RISKS rules will produce nonsensical results:
- "spending spike >20% vs prev month": prev=0, current>0 → spike is infinite → rule fires spuriously.
- "high borrowing >50% of income": if income=0 → division by zero.
- "overspending": current expense > 0 but prev = 0 means any expense is technically "overspending vs nothing".

**Prevention:**
- Track `hasEnoughHistory: Boolean = prevMonthHasAnyTransactions`. Expose in `InsightsUiState`.
- Skip comparison-based rules (overspending, spending spike) when `!hasEnoughHistory`.
- Keep absolute rules (negative net position, no savings recorded) active regardless of history.
- Show "Insights improve after your first full month" disclaimer on RISKS and TRENDS when `!hasEnoughHistory`.

---

### E3: Split Transaction Double-Counting

**What goes wrong:** `TransactionEntity` has `isSplitParent` and `isSplitChild` flags. A split transaction has one parent row (e.g., 1000 total) and N child rows (e.g., 400 + 600). Summing `amount` without filtering out split children counts the parent AND all children, double-counting the total.

**The existing DashboardViewModel consistently filters `!it.isSplitChild`** throughout (lines 725, 730, 851, etc.).

**Prevention:**
- Every aggregation in InsightsViewModel must filter `!tx.isSplitChild` before summing.
- Add this as an explicit unit test: a 1000 split-parent with two children (400+600) should contribute 1000 to the total, not 2000.

---

### E4: Transfer Transactions Inflate Income and Expense

**What goes wrong:** `type = "transfer"` transactions appear in the table. Summing all amounts without type-filtering counts transfers as both income (to-account) and expense (from-account), inflating totals. The existing app creates **two** transaction rows for every transfer (one OUT, one IN), so an internal transfer of 1000 adds 2000 to a naive sum.

**Prevention:**
- For STATUS income: `txs.filter { it.type == "income" }.sumOf { it.amount }`. Explicit inclusion, not exclusion.
- For STATUS expense: `txs.filter { it.type == "expense" }.sumOf { it.amount }`.
- For net cash flow: define the formula explicitly — `income - expense`. Do not try to include transfer/savings/lend unless intentional.
- For TRENDS dominant activity: decide in advance whether `transfer`, `lend`, `borrow` are included, then code only those types.

---

## MPAndroidChart in Compose

### M1: MPAndroidChart Is Not a Compose Library — AndroidView Wrapper is Mandatory

**What goes wrong:** MPAndroidChart v3.1.0 (confirmed in build.gradle.kts) is a traditional View-based library. Attempting to use `LineChart` directly as a Composable causes compilation errors. The `AccountComparisonChart.kt` in this codebase actually does NOT use MPAndroidChart — it uses custom Compose drawing. There is no existing MPAndroidChart-in-AndroidView usage to reference in this codebase.

**This means the Insights line chart will be the first MPAndroidChart AndroidView usage in this project.**

**Prevention:**
- Use `AndroidView(factory = { context -> LineChart(context).apply { /* one-time setup */ } }, update = { chart -> /* data updates */ })`.
- All one-time configuration (axis styling, legend setup, touch disable) goes in `factory`.
- All data updates (setting `chart.data`, calling `chart.invalidate()`) go in `update`.

---

### M2: AndroidView update Lambda Runs on Every Recomposition — Chart Flickers

**What goes wrong:** The `update` lambda of `AndroidView` runs every time the parent composable recomposes, even if the underlying chart data has not changed. Calling `chart.invalidate()` every recomposition causes visible chart flickering and unnecessary redraw work.

**Prevention:**
- Memoize the `LineDataSet` using `remember(key = chartDataHash)` where `chartDataHash` is a stable key (e.g., `chartEntries.hashCode()` or `Pair(chartEntries.size, chartEntries.lastOrNull()?.y)`).
- Only pass a new `LineDataSet` to `chart.data` when the memoized key actually changes.
- Do not call `chart.animateX(duration)` in the `update` lambda — it will re-animate on every recomposition. Call it only in `factory` or guarded by a one-shot `LaunchedEffect`.

---

### M3: MPAndroidChart Listener Memory Leaks

**What goes wrong:** `OnChartValueSelectedListener` and `OnChartGestureListener` hold strong references. If these listeners capture composable lambdas or ViewModel references, they can prevent garbage collection after the composable leaves the composition.

**Prevention:**
- Per the Insights spec, the line chart is display-only (no user interaction). Do not set any listeners on the chart.
- If interactivity is added later: set the listener in `factory`, and use `DisposableEffect { onDispose { chart.setOnChartValueSelectedListener(null) } }` to clean up.

---

### M4: chart.invalidate() Called Off Main Thread

**What goes wrong:** `chart.invalidate()` is a View method and must be called on the main thread. If it is somehow invoked from a background coroutine (which can happen if someone restructures the AndroidView to call chart methods from a flow collector running on Dispatchers.IO), it throws `CalledFromWrongThreadException` or silently fails.

**Prevention:**
- The `AndroidView update` lambda always executes on the main thread. Keeping all chart mutations inside `update` guarantees thread safety.
- Never launch a coroutine from inside `AndroidView update`. Prepare data in the ViewModel on Dispatchers.Default and pass the final, ready-to-render data object to the composable.

---

### M5: LineDataSet With a Single Entry Crashes With CUBIC_BEZIER Mode

**What goes wrong:** If only 1 day in the current month has transactions, the daily `LineDataSet` has a single `Entry`. MPAndroidChart `LineDataSet.Mode.CUBIC_BEZIER` requires at least 2 points; with one point it either crashes or draws incorrectly.

**Prevention:**
- Always pad the dataset to a minimum of 2 entries. Add a synthetic Entry(0f, 0f) for day 0 (before the first real entry) if the real dataset has only 1 point.
- Use `LineDataSet.Mode.LINEAR` as the default for this chart — it is safe with any number of entries >= 1.
- Guard before constructing the chart: `if (entries.isEmpty()) return@AndroidView` and show a placeholder composable.

---

## getAllTransactions() Risk

### G1: Confirmed Callers at Runtime — The Fan-out Is Already at 7

**Current active subscribers of `getAllTransactions()`** confirmed from source inspection:

| Location | Line | Purpose |
|----------|------|---------|
| `DashboardViewModel` | 368 | `budgetsWithProgressFlow` |
| `DashboardViewModel` | 544 | `savingsDestinationsFlow` |
| `DashboardViewModel` | 608 | `savingsStatsFlow` |
| `DashboardViewModel` | 636 | `lendingDashboardSummaryFlow` |
| `DashboardViewModel` | 709 | `overviewFlow` |
| `AccountsViewModel` | 33 | Account comparison chart data |
| `BudgetsViewModel` | 64 | Budget spend tracking (via DAO directly) |
| `ExportRepository` | 211, 292, 384 | One-shot `.first()` on export only — NOT persistent subscriptions |

**Active persistent subscriptions when Dashboard is open: 7.**

Adding an InsightsViewModel `getAllTransactions()` call makes it 8. Every time ANY transaction is added, all 8 subscribers receive the full table list simultaneously. On a low-end Android 8 device (minSdk = 26) with 500+ transactions, this produces a measurable frame drop.

**The correct pattern for InsightsViewModel:**
- One DAO query: `getTransactionsByDateRange(prevMonthStart, currentMonthEnd)`.
- This query subscribes to a narrow date window. Adding a transaction outside this window (e.g., a transaction dated 6 months ago) does NOT trigger a re-emission.
- If a new DAO method is more convenient: `@Query("SELECT * FROM transactions WHERE date BETWEEN :start AND :end ORDER BY date ASC") fun getTransactionsForInsights(start: Long, end: Long): Flow<List<TransactionEntity>>`.

### G2: Do Not Modify getAllTransactions() — 7 Active Call Sites

**What goes wrong:** If `getAllTransactions()` is changed (new parameter, different return type, renamed), all 7 active callers break at compile time. The signature is a stable contract.

**Prevention:**
- Add new DAO methods for Insights rather than modifying existing ones.
- If a shared utility for "two-month range" is useful across ViewModels, add it as an additional repository method, not a modification of `getAllTransactions()`.

---

## Prevention Summary

| Pitfall | Prevention | Phase |
|---------|-----------|-------|
| P1: getAllTransactions() fan-out | Use `getTransactionsByDateRange()` spanning both months; never `getAllTransactions()` | Phase 1: ViewModel setup |
| P2: Aggregation on main thread | `.flowOn(Dispatchers.Default)` before `stateIn()` | Phase 1: ViewModel setup |
| P3: Merging into DashboardViewModel | InsightsViewModel must be a separate ViewModel | Phase 1: Architecture decision |
| H1: PagerState instability | Hardcode `pageCount = 3` as constant | Phase 2: Screen scaffold |
| H2: Nested scroll conflicts | Use `Column` (not `LazyColumn`) in all 3 pager pages | Phase 2: Screen scaffold |
| H3: Monolithic UiState recompositions | Decompose into StatusUiState / RisksUiState / TrendsUiState | Phase 1: UiState design |
| H4: LaunchedEffect in pager pages | Do not use LaunchedEffect inside pager pages | Phase 2: Screen scaffold |
| D1: Timezone inconsistency | Reuse Calendar.getInstance() pattern; do not switch to UTC | Phase 1: Date utilities |
| D2: January/December edge case | Extract shared date-range utility; do not re-implement Calendar math | Phase 1: Date utilities |
| D3: Daily grouping uses UTC day | Use `Calendar.get(Calendar.DAY_OF_MONTH)` not epoch division | Phase 3: TRENDS chart data |
| D4: Division by zero in % change | PercentageChange sealed class; show "N/A" when prev month is zero | Phase 1: UiState design |
| E1: Zero transactions crash | Guard chart on empty entries; distinct "All clear" state for RISKS | Phase 2 + Phase 3 |
| E2: One month of data misfires | `hasEnoughHistory` flag; skip comparison-based RISKS rules | Phase 3: Risk rules |
| E3: Split child double-counting | Filter `!tx.isSplitChild` in every aggregation | Phase 1: ViewModel setup |
| E4: Transfer inflation | Filter by explicit type inclusion, never by negation | Phase 1: ViewModel setup |
| M1: No MPAndroidChart Compose native | Always use AndroidView wrapper; first usage in this codebase | Phase 3: TRENDS chart |
| M2: AndroidView update flickers | Memoize LineDataSet with remember(key); no animateX in update | Phase 3: TRENDS chart |
| M3: Listener memory leak | No listeners (display-only chart per spec) | Phase 3: TRENDS chart |
| M4: invalidate() off main thread | Keep all chart mutations inside AndroidView update lambda | Phase 3: TRENDS chart |
| M5: Single-entry CUBIC_BEZIER crash | Use Mode.LINEAR; pad to min 2 entries; guard empty list | Phase 3: TRENDS chart |
| G1: getAllTransactions() 7 active callers | Use narrow date-range query in InsightsViewModel | Phase 1: Architecture |
| G2: getAllTransactions() signature stability | Add new DAO methods; never modify existing getAllTransactions() | All phases |
