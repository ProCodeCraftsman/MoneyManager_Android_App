---
phase: 260504-dsa-implement-summary-screen
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryUiState.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryViewModel.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryAggregator.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryScreen.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryPieChart.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryFilterSheet.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/NetBalanceCard.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/ExpenseOverviewCard.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/TopCategoriesList.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/TopBudgetUtilizationList.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/SummaryHeader.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt
autonomous: false
requirements:
  - PRD-Header
  - PRD-NetBalance
  - PRD-Tabs
  - PRD-ExpenseOverview
  - PRD-PieChart
  - PRD-Top5Categories
  - PRD-Top5Budget
  - PRD-Filters
  - PRD-Navigation
  - PRD-EmptyState

must_haves:
  truths:
    - "User can navigate to the Summary screen from the bottom navigation"
    - "User sees Income, Expense, and Net Balance for the selected period derived from real transactions (not hardcoded)"
    - "User sees five category-context tabs (Expense, Income, Lending, Transfers, Savings) and Expense is the default active tab"
    - "On the Expense tab, user sees Total Spent, Budget Remaining, a category pie chart, top 5 categories with progress bars, and top 5 budget-utilization rows"
    - "User can change time filter (Day/Week/Month/Custom) and all aggregates update reactively"
    - "User can open a filter modal and constrain by account and category, and the screen updates"
    - "When no transactions exist for the period, an empty state with 'Add Transaction' CTA is shown"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryViewModel.kt"
      provides: "Hilt ViewModel exposing StateFlow<SummaryUiState> with time/account/category/type filters"
      contains: "SummaryViewModel"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryAggregator.kt"
      provides: "Pure-Kotlin aggregation helpers (totals, categoryBreakdown, topCategories, topBudgetUtilization)"
      contains: "object SummaryAggregator"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryScreen.kt"
      provides: "Composable Summary screen with header, net-balance card, tab row, and Expense tab content"
      contains: "fun SummaryScreen"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryPieChart.kt"
      provides: "Canvas-based pie chart composable for category spend distribution"
      contains: "fun SummaryPieChart"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt"
      provides: "Adds Screen.Summary route + composable + bottom-nav entry"
      contains: "Screen.Summary"
  key_links:
    - from: "MoneyManagerNavHost.kt"
      to: "SummaryScreen"
      via: "composable(Screen.Summary.route) { SummaryScreen(viewModel = hiltViewModel()) }"
      pattern: "Screen\\.Summary"
    - from: "SummaryViewModel"
      to: "TransactionRepository.getTransactionsByDateRange"
      via: "filterState.flatMapLatest { params -> transactionRepository.getTransactionsByDateRange(start, end) }"
      pattern: "getTransactionsByDateRange"
    - from: "SummaryViewModel"
      to: "BudgetRepository.getAllBudgets"
      via: "combine(getAllBudgets(), filteredTransactions, categories) { ... } for budget utilization"
      pattern: "getAllBudgets"
    - from: "SummaryAggregator"
      to: "TransactionEntity.isSplitChild"
      via: "filter { !it.isSplitChild } before any sumOf/groupBy"
      pattern: "isSplitChild"
    - from: "SummaryScreen (Expense tab)"
      to: "SummaryPieChart"
      via: "passes uiState.expenseByCategory: List<PieChartEntry>"
      pattern: "SummaryPieChart\\("
---

<objective>
Implement the Summary Screen described in PRD.md: a single consolidated finance overview with header, net balance card, tab navigation (Expense/Income/Lending/Transfers/Savings), and full Expense tab content (overview card, category pie chart, top 5 categories, top 5 budget utilization). Aggregates are derived purely from `TransactionEntity` records (no hardcoded values), respect time/account/category filters, and exclude `isSplitChild=true` rows. Wire the screen into the existing `MoneyManagerNavHost` bottom navigation.

Purpose: Give the user a fast, single-screen financial overview that meets PRD §11 success criteria — identify total spend, top spending categories, and budget status within < 5 seconds.

Output:
- New `com.moneymanager.app.ui.summary` package containing ViewModel, UI state, pure aggregator, screen, pie chart, filter sheet, and section composables
- Updated `MoneyManagerNavHost.kt` registering `Screen.Summary` and exposing it in the bottom NavigationBar
- Build passes (`./gradlew :app:assembleDebug`)
- Human-verified Expense tab visual + filter behaviour
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/quick/260504-dsa-implement-summary-screen-for-personal-fi/PRD.md

# Existing patterns to mirror (filter flow + aggregation + Compose screen)
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsScreen.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsCalculator.kt

# Existing reusable components
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/PieChartEntry.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TimeFilterBar.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TransactionFilterSheet.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/trends/TrendsLineChart.kt

# Data layer (do NOT modify)
@MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt
@MoneyManager/app/src/main/java/com/moneymanager/data/entity/BudgetEntity.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/BudgetRepository.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/CategoryRepository.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/AccountRepository.kt

# Theme + navigation
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/AppTheme.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/util/CurrencyUtils.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/util/ColorUtils.kt

<interfaces>
<!-- Contracts already defined in the codebase that this plan consumes. -->
<!-- Executor must use these exactly — do NOT re-derive shapes. -->

From com.moneymanager.data.entity.TransactionEntity:
```kotlin
data class TransactionEntity(
    val id: Long = 0,
    val accountId: Long,
    val type: String,                 // "income" | "expense" | "savings" | "transfer" | "lend" | "receive" | "borrow" | "repay"
    val amount: Double,               // always positive; sign derived from type
    val categoryId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val isSplitChild: Boolean = false,
    val toAccountId: Long? = null,
    // ... other fields
) {
    companion object { val VALID_TYPES = listOf("income","expense","savings","transfer","lend","receive","borrow","repay") }
}
```

From com.moneymanager.data.entity.BudgetEntity:
```kotlin
data class BudgetEntity(
    val id: Long = 0,
    val categoryId: Long,
    val amount: Double,
    val month: String,                // "yyyy-MM" e.g. "2026-05"
    val isSavingsTarget: Boolean = false
)
```

From com.moneymanager.domain.repository.TransactionRepository:
```kotlin
fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
// Note: do NOT use getAllTransactions() per STATE.md architecture decision
```

From com.moneymanager.domain.repository.BudgetRepository:
```kotlin
fun getAllBudgets(): Flow<List<BudgetEntity>>
fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>>   // period = "yyyy-MM"
```

From com.moneymanager.app.ui.components.PieChartEntry:
```kotlin
data class PieChartEntry(
    val label: String,
    val value: Double,
    val color: androidx.compose.ui.graphics.Color,
    val percentage: Double = 0.0
)
```

From com.moneymanager.app.ui.screens (DashboardViewModel.kt) — reuse this enum, do NOT redeclare:
```kotlin
enum class TimeFilter(val displayName: String) { DAY, WEEK, MONTH, YEAR, ALL, CUSTOM }
```

From com.moneymanager.app.ui.MoneyManagerNavHost — pattern to follow when adding Summary:
```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    // ... existing entries
    // NEW entry to add:
    // data object Summary : Screen("summary", "Summary", Icons.Default.Summarize)
}
```

From com.moneymanager.app.ui.util.CurrencyUtils — used for amount formatting throughout.
From com.moneymanager.app.ui.util.parseColor(hex: String): Color — used to convert category hex colors.
</interfaces>

<architecture_constraints>
1. **Use `getTransactionsByDateRange(start, end)` ONLY** for the date-window read. NEVER `getAllTransactions()` (STATE.md: 7 active subscribers already; we will not become the 8th).
2. **Exclude `isSplitChild = true`** rows from EVERY aggregation (totals, breakdowns, top-N, budget utilization). Apply once after the date-range fetch.
3. **No new gradle dependencies.** No MPAndroidChart. The pie chart MUST be a `Canvas { drawArc(...) }` composable (mirror `TrendsLineChart.kt`'s Canvas approach).
4. **Reuse existing tokens:** colours from `MaterialTheme.colorScheme`, typography from `MaterialTheme.typography`, spacing in 4/8/16/24 dp steps. No new theme files.
5. **MVVM:** all derivation in ViewModel/Aggregator; composables stateless except for `rememberSaveable` tab index and modal visibility.
6. **Reuse the existing `TimeFilter` enum** from `DashboardViewModel.kt` — do NOT define a second one.
7. **`SummaryAggregator` is a `object` with no Android imports** — pure Kotlin, unit-testable (mirror `InsightsCalculator` pattern called out in STATE.md).
8. **PRD §8.2 edge cases:** missing category → label "Uncategorized"; no budget for a category → omit from utilization list (do NOT show N/A rows); negative amounts → `coerceAtLeast(0.0)` only on display percentages, never on raw sums.
</architecture_constraints>
</context>

<tasks>

<task type="auto" tdd="false">
  <name>Task 1: Data layer — SummaryUiState, SummaryAggregator, SummaryViewModel</name>
  <files>
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryUiState.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryAggregator.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryViewModel.kt
  </files>
  <behavior>
    Pure aggregator (no Android imports) and a Hilt ViewModel that emits a single StateFlow<SummaryUiState>:

    - Given an empty transaction list → totals are 0.0, breakdowns are empty, isEmpty=true.
    - Given mixed transactions including `isSplitChild=true` rows → those rows are excluded everywhere.
    - Given EXPENSE rows totalling 1000 with categories {Food=600, Travel=300, Other=100} → topCategories returns Food, Travel, Other in that order with percentages 60/30/10.
    - Given budgets {Food: 500, Travel: 400} with spent {Food: 600, Travel: 200} → topBudgetUtilization returns Food first (120%), Travel second (50%).
    - Changing TimeFilter from MONTH to WEEK re-emits a new SummaryUiState (reactive).
    - Net balance = sum(type=="income") - sum(type=="expense") for the period.
  </behavior>
  <action>
Create the package `com.moneymanager.app.ui.summary` and add three files.

**(a) `SummaryUiState.kt`** — data classes only. Reuse `TimeFilter` by importing `com.moneymanager.app.ui.screens.TimeFilter`. Reuse `PieChartEntry` from `com.moneymanager.app.ui.components.PieChartEntry`.

```kotlin
package com.moneymanager.app.ui.summary

import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.app.ui.screens.TimeFilter

enum class SummaryTab { EXPENSE, INCOME, LENDING, TRANSFERS, SAVINGS }

data class CategorySpend(
    val categoryId: Long?,
    val name: String,
    val amount: Double,
    val percentOfTotal: Float,   // 0..100
    val color: androidx.compose.ui.graphics.Color
)

data class BudgetUtilizationRow(
    val categoryId: Long,
    val categoryName: String,
    val budgetLimit: Double,
    val spent: Double,
    val utilizationPercent: Float, // 0..>100 allowed (overrun)
    val color: androidx.compose.ui.graphics.Color
)

data class SummaryUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val activeTab: SummaryTab = SummaryTab.EXPENSE,

    // Header / period
    val selectedFilter: TimeFilter = TimeFilter.MONTH,
    val filterDisplayDate: String = "",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,

    // Net balance card
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val prevNetBalance: Double = 0.0,
    val netBalanceTrendPercent: Double = 0.0,

    // Expense tab data
    val totalBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetUtilizationPercent: Float = 0f,
    val expenseByCategory: List<PieChartEntry> = emptyList(),
    val topCategories: List<CategorySpend> = emptyList(),
    val topBudgetUtilization: List<BudgetUtilizationRow> = emptyList(),

    // Filters
    val selectedAccountIds: Set<Long> = emptySet(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val selectedTxnTypes: Set<String> = emptySet(),
    val accounts: List<com.moneymanager.data.entity.AccountEntity> = emptyList(),
    val categories: List<com.moneymanager.data.entity.CategoryEntity> = emptyList(),

    val currency: String = "INR"
)
```

**(b) `SummaryAggregator.kt`** — pure Kotlin object, NO Android imports except `androidx.compose.ui.graphics.Color` (Color is multiplatform-friendly; if linter flags it, move colour resolution to the ViewModel and have the aggregator return hex strings instead). Methods:

```kotlin
package com.moneymanager.app.ui.summary

import androidx.compose.ui.graphics.Color
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TransactionEntity

object SummaryAggregator {
    /** Drops split-child rows. ALWAYS call this first. */
    fun excludeSplitChildren(txs: List<TransactionEntity>): List<TransactionEntity> =
        txs.filter { !it.isSplitChild }

    fun sumByType(txs: List<TransactionEntity>, type: String): Double =
        txs.filter { it.type == type }.sumOf { it.amount }

    /** PieChartEntry list of expense by category, sorted desc, "Top N" + others combined. */
    fun expenseByCategory(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 6,
        parseColor: (String?) -> Color
    ): List<PieChartEntry> { /* group by categoryId, sumOf amount, sort desc, fold tail into "Others" */ }

    /** Top N category spends with percent of total expense. */
    fun topCategories(
        txs: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 5,
        parseColor: (String?) -> Color
    ): List<CategorySpend> { /* ... */ }

    /** Top N budget utilization rows for the budgets of a given "yyyy-MM" period. */
    fun topBudgetUtilization(
        txs: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        categories: List<CategoryEntity>,
        topN: Int = 5,
        parseColor: (String?) -> Color
    ): List<BudgetUtilizationRow> { /* spend per categoryId, divide by budget.amount, sort desc, take topN */ }

    fun netBalance(txs: List<TransactionEntity>): Double =
        sumByType(txs, "income") - sumByType(txs, "expense")
}
```

Implementation rules:
- Missing category → name="Uncategorized", color=Color(0xFF90A4AE).
- Categories with no budget → excluded from `topBudgetUtilization` (do not synthesise N/A rows).
- All sums use `Double` and `sumOf { it.amount }`. Percentages use `Float`.

**(c) `SummaryViewModel.kt`** — `@HiltViewModel class SummaryViewModel @Inject constructor(...) : ViewModel()`. Mirror the filter-pipeline pattern from `DashboardViewModel`:

Inject:
```
private val transactionRepository: TransactionRepository,
private val budgetRepository: BudgetRepository,
private val categoryRepository: CategoryRepository,
private val accountRepository: AccountRepository,
private val preferencesManager: com.moneymanager.data.preferences.PreferencesManager
```

State flows (private MutableStateFlow):
- `selectedFilter: TimeFilter` (default MONTH)
- `currentPeriodDate: Calendar`
- `customStartDate: Long?`, `customEndDate: Long?`
- `selectedAccountIds: Set<Long>`, `selectedCategoryIds: Set<Long>`, `selectedTxnTypes: Set<String>`
- `activeTab: SummaryTab` (default EXPENSE)

Reuse `getDateRangeForFilter` and `getFilterDisplayDate` logic from `DashboardViewModel.kt` — copy into a `private fun` in this VM (do NOT modify DashboardViewModel; do NOT extract to a shared util in this plan — keeps blast radius zero per CLAUDE.md). Per the gitnexus rule, since we are NOT modifying any existing symbol, no impact analysis is required for this file.

Build the data pipeline:
```
val filteredTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { p ->
    val (start, end) = getDateRangeForFilter(p.filter, p.baseDate, p.customStart, p.customEnd)
    transactionRepository.getTransactionsByDateRange(start, end)
        .map { list ->
            SummaryAggregator.excludeSplitChildren(list).filter { tx ->
                (p.accountIds.isEmpty() || tx.accountId in p.accountIds || tx.toAccountId in p.accountIds) &&
                (p.categoryIds.isEmpty() || tx.categoryId in p.categoryIds) &&
                (p.txnTypes.isEmpty() || tx.type in p.txnTypes)
            }
        }
}
```

For prev-period trend: same as DashboardViewModel — clone baseDate, subtract one unit per filter, recompute range, fetch via `getTransactionsByDateRange`, compute prev income−expense.

For budgets: the active "yyyy-MM" string is derived from `params.baseDate` when `filter == MONTH`; for non-MONTH filters use `getAllBudgets()` and accept the imprecision (PRD does not require period-budget reconciliation for week/year filters — note this with a `// PRD: budget rows shown for current MONTH bucket only when filter != MONTH`).

Expose:
- `val uiState: StateFlow<SummaryUiState>` via `combine(filteredTransactions, prevPeriodFlow, budgetRepository.getAllBudgets(), categoryRepository.getAllCategories(), accountRepository.getAllAccounts(), preferencesManager.currency, activeTab, filterState) { ... }.stateIn(viewModelScope, WhileSubscribed(5000), SummaryUiState())`
- Public setters: `setTimeFilter(f)`, `setActiveTab(t)`, `applyFilters(accounts, categories, txnTypes)`, `setCustomDateRange(start, end)`, `navigatePeriod(offset)`, `clearFilters()`.

Use `ColorUtils.parseColor` (already at `com.moneymanager.app.ui.util.parseColor`) for the `parseColor` lambda passed into the aggregator.

Performance: stream operations are O(n) over the month's transactions; PRD §8.3 budget < 500ms is met because we already filter at the DAO date-range level.
  </action>
  <verify>
    <automated>cd MoneyManager && ./gradlew :app:compileDebugKotlin</automated>
  </verify>
  <done>
    - Three new files exist under `com.moneymanager.app.ui.summary`.
    - `./gradlew :app:compileDebugKotlin` succeeds with no warnings about the new package.
    - `SummaryViewModel` has `@HiltViewModel` and constructor-injects the four repositories + PreferencesManager.
    - `SummaryAggregator` is a top-level `object` (not a class). It has no `import android.*` or `import androidx.lifecycle.*` lines.
    - Every aggregator method that takes transactions starts by either (a) being passed already-filtered txs, or (b) calling `excludeSplitChildren` on entry.
    - The VM uses `transactionRepository.getTransactionsByDateRange(...)` and never `getAllTransactions(...)`.
  </done>
</task>

<task type="auto" tdd="false">
  <name>Task 2: UI — SummaryPieChart, section composables, SummaryFilterSheet, SummaryScreen</name>
  <files>
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryPieChart.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryFilterSheet.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/SummaryScreen.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/SummaryHeader.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/NetBalanceCard.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/ExpenseOverviewCard.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/TopCategoriesList.kt,
    MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/components/TopBudgetUtilizationList.kt
  </files>
  <action>
Implement the UI layer. All composables stateless (consume props), placed under `com.moneymanager.app.ui.summary` and `.../summary/components`. Use `MaterialTheme.colorScheme` and `MaterialTheme.typography` exclusively — no hard-coded colours except brand-style accents already used elsewhere (e.g. `Color(0xFFFF7043)` for outflow indicator if needed, mirroring `AccountComparisonChart`). Use `CurrencyUtils.formatAmount(value, currency)` for every monetary value.

**(a) `SummaryPieChart.kt`** — Canvas-based pie chart. Pattern mirrors `TrendsLineChart.kt` (uses `Canvas`, `drawArc`, `drawCircle` for hole, `rememberTextMeasurer` for legend labels). API:

```kotlin
@Composable
fun SummaryPieChart(
    entries: List<PieChartEntry>,         // value > 0; sum used for slice angle
    modifier: Modifier = Modifier,
    holeRatio: Float = 0.55f,             // donut style
    onSliceClick: ((PieChartEntry) -> Unit)? = null
)
```

Implementation outline:
- Compute total = entries.sumOf { it.value }; if total <= 0 → render an empty-state circle with text "No data".
- Iterate entries; for each, `drawArc(color = entry.color, startAngle = currentAngle, sweepAngle = (entry.value/total*360).toFloat(), useCenter = true, ...)`.
- After arcs, draw a `MaterialTheme.colorScheme.surface` filled circle with radius = arcRadius * holeRatio to create donut.
- Below the canvas, render a vertical Column legend: top 5 entries with a colored square + label + formatted percent. Mirror the `LegendItem` private composable from `AccountComparisonChart.kt`.
- Aspect ratio: `Modifier.aspectRatio(1f)` on the Canvas, parent sets max width to ~240.dp.
- Click handling (Material `pointerInput { detectTapGestures }`) is OPTIONAL — implement only if it does not push the file > 200 lines; otherwise leave `onSliceClick` parameter wired but no detection (PRD §8.4 marks tap-slice as enhancement, not required).

**(b) `SummaryFilterSheet.kt`** — `ModalBottomSheet` mirroring `TransactionFilterSheet.kt`. Inputs: accounts, categories, current selections, txn-type set, callbacks. Fields per PRD §7:
- Account multi-select (FilterChip row for each account)
- Category multi-select (FilterChip row for each category)
- Transaction type multi-select with chips: "Expense", "Income", "Others" (Others = savings|transfer|lend|receive|borrow|repay)
- "Apply" + "Clear" buttons. Time filter is NOT in the sheet — it lives in the header bar.

**(c) Section composables under `summary/components/`:**

- `SummaryHeader.kt`:
  - Top row: `Text("Summary", style = MaterialTheme.typography.headlineMedium)` + `IconButton(onClick = onCalendarClick) { Icon(Icons.Default.CalendarToday, ...) }` + `IconButton(onClick = onFilterClick) { Icon(Icons.Default.FilterList, ...) }`
  - Below: a horizontal-scrolling time-filter chip row mirroring `TimeFilterBar.kt` (re-import and use it directly — do NOT duplicate). Display the period label `uiState.filterDisplayDate` between back/forward chevron `IconButton`s that call `onNavigatePeriod(-1)` / `onNavigatePeriod(+1)`.

- `NetBalanceCard.kt`:
  - `Card(modifier = Modifier.fillMaxWidth())`
  - Net balance amount in `headlineLarge`, color = positive→primary, negative→error
  - Row: "Income {amount}" green dot + "Expense {amount}" red dot
  - Trend: if `netBalanceTrendPercent != 0.0`, show ▲/▼ arrow + percent + "vs last period". Hide if both periods are zero.

- `ExpenseOverviewCard.kt`:
  - Three pieces of data in a `Row`: Total Spent (left), Budget Remaining (right). Center: a `CircularProgressIndicator(progress = (utilizationPercent/100f).coerceIn(0f, 1f), modifier = Modifier.size(96.dp), strokeWidth = 8.dp)` overlaid with the percent text via `Box(contentAlignment = Center)`.
  - If totalBudget == 0 → hide the circular progress and show a `Text("No budget set", style = bodySmall)`.

- `TopCategoriesList.kt`:
  - Section header `Text("Top Categories", style = titleMedium)`
  - `Column { rows.forEach { row -> CategoryRow(row) } }` — each row: emoji/name (left), amount (right), then a `LinearProgressIndicator(progress = row.percentOfTotal/100f, color = row.color, modifier = Modifier.fillMaxWidth())` and percent text below.

- `TopBudgetUtilizationList.kt`:
  - Section header `Text("Top Budget Utilization", style = titleMedium)`
  - Rows: category name + "{spent} / {limit}" + `LinearProgressIndicator(progress = (utilizationPercent/100f).coerceIn(0f, 1f), color = if (utilizationPercent > 100f) MaterialTheme.colorScheme.error else row.color)` + percent text. If `utilizationPercent > 100f`, append "(over budget)".
  - If `rows.isEmpty()` → render empty composable (return `Unit`); the screen-level empty state covers it.

**(d) `SummaryScreen.kt`** — the host composable.

```kotlin
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = hiltViewModel(),
    onNavigateToAddTransaction: () -> Unit = {}      // for empty-state CTA
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        SummaryHeader(uiState = uiState, onTimeFilterChange = viewModel::setTimeFilter,
                      onNavigatePeriod = viewModel::navigatePeriod,
                      onCustomDateRange = viewModel::setCustomDateRange,
                      onFilterClick = { showFilterSheet = true })

        if (uiState.isLoading) { Box(Modifier.fillMaxWidth().padding(48.dp), Alignment.Center) { CircularProgressIndicator() } }
        else if (uiState.isEmpty) { SummaryEmptyState(onAdd = onNavigateToAddTransaction) }
        else {
            NetBalanceCard(
                netBalance = uiState.netBalance,
                income = uiState.totalIncome,
                expense = uiState.totalExpense,
                trendPercent = uiState.netBalanceTrendPercent,
                currency = uiState.currency
            )

            // Tab row (5 tabs)
            val tabs = listOf("Expense","Income","Lending","Transfers","Savings")
            ScrollableTabRow(selectedTabIndex = uiState.activeTab.ordinal) {
                tabs.forEachIndexed { i, label ->
                    Tab(selected = uiState.activeTab.ordinal == i,
                        onClick = { viewModel.setActiveTab(SummaryTab.values()[i]) },
                        text = { Text(label) })
                }
            }

            when (uiState.activeTab) {
                SummaryTab.EXPENSE -> {
                    ExpenseOverviewCard(...)
                    SummaryPieChart(entries = uiState.expenseByCategory, ...)
                    TopCategoriesList(rows = uiState.topCategories, currency = uiState.currency)
                    TopBudgetUtilizationList(rows = uiState.topBudgetUtilization, currency = uiState.currency)
                }
                else -> {
                    // Stub for non-Expense tabs — show "Coming soon" placeholder.
                    // PRD primary focus is Expense (§6); other tabs are scaffolded only.
                    Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                        Text("Detail view coming soon", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        SummaryFilterSheet(
            accounts = uiState.accounts,
            categories = uiState.categories,
            selectedAccountIds = uiState.selectedAccountIds,
            selectedCategoryIds = uiState.selectedCategoryIds,
            selectedTxnTypes = uiState.selectedTxnTypes,
            onApply = { accs, cats, types -> viewModel.applyFilters(accs, cats, types); showFilterSheet = false },
            onClear = { viewModel.clearFilters(); showFilterSheet = false },
            onDismiss = { showFilterSheet = false }
        )
    }
}
```

A small private `SummaryEmptyState(onAdd)` composable lives at the bottom of `SummaryScreen.kt` (PRD §8.1): centred Icon + `Text("No transactions available")` + `Button(onClick = onAdd) { Text("Add Transaction") }`.

Accessibility (PRD §10):
- Every `Icon` has a non-null `contentDescription`.
- Touch targets use `Modifier.minimumInteractiveComponentSize()` or rely on `IconButton` defaults (48.dp).
- Pie chart wrap with `Modifier.semantics { contentDescription = "Spending breakdown by category" }`.
  </action>
  <verify>
    <automated>cd MoneyManager && ./gradlew :app:compileDebugKotlin</automated>
  </verify>
  <done>
    - All 8 UI files exist and compile.
    - `SummaryPieChart` uses `Canvas` (not a third-party library).
    - `SummaryScreen` consumes `SummaryViewModel` via `hiltViewModel()` and renders header, net-balance card, 5-tab row, and Expense tab content.
    - Empty state renders when `uiState.isEmpty == true`.
    - Filter sheet opens on filter-icon tap and applies via `viewModel.applyFilters(...)`.
    - No `import com.github.mikephil.*` or any new external chart import.
    - `./gradlew :app:compileDebugKotlin` succeeds.
  </done>
</task>

<task type="auto" tdd="false">
  <name>Task 3: Navigation wiring + full build</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</files>
  <action>
**Pre-edit safety (per CLAUDE.md):**
- Run `gitnexus_impact({target: "MoneyManagerNavHost", direction: "upstream"})` before editing. Report blast radius. Expect LOW risk (only `MainActivity` consumes it as the app entry composable).
- Run `gitnexus_context({name: "Screen"})` to confirm the `Screen` sealed class location.

**Edits to `MoneyManagerNavHost.kt`:**

1. Add new imports near the existing `com.moneymanager.app.ui.screens.*` block:
   ```kotlin
   import androidx.compose.material.icons.filled.Summarize
   import com.moneymanager.app.ui.summary.SummaryScreen
   ```

2. In the `sealed class Screen` block, add the `Summary` data object alongside the existing entries:
   ```kotlin
   data object Summary : Screen("summary", "Summary", Icons.Default.Summarize)
   ```

3. Update `bottomNavScreens` (currently `Dashboard, Transactions, Insights, Settings`) to insert `Summary` between `Transactions` and `Insights`:
   ```kotlin
   val bottomNavScreens = listOf(
       Screen.Dashboard,
       Screen.Transactions,
       Screen.Summary,
       Screen.Insights,
       Screen.Settings
   )
   ```
   This gives 5 bottom-nav items (verify Material3 NavigationBar handles 5 — it does; M3 supports up to 5 destinations).

4. In the `NavHost { ... }` block, add a new `composable(Screen.Summary.route)` entry next to the `Insights` entry:
   ```kotlin
   composable(Screen.Summary.route) {
       SummaryScreen(
           viewModel = hiltViewModel(),
           onNavigateToAddTransaction = {
               navController.navigate(Screen.Transactions.createRoute())
           }
       )
   }
   ```

**Post-edit:**
- Run `gitnexus_detect_changes({scope: "all"})` to confirm changes are scoped to `MoneyManagerNavHost.kt` + the new `summary/` package files.
- Build: `./gradlew :app:assembleDebug` (full build, not just compile, to catch Hilt/KSP errors and resource issues).
- Confirm no new gradle dependencies were added in `MoneyManager/app/build.gradle.kts` (we have not modified it, but verify). Run `git status MoneyManager/app/build.gradle.kts` — it should show no changes.
  </action>
  <verify>
    <automated>cd MoneyManager && ./gradlew :app:assembleDebug</automated>
  </verify>
  <done>
    - `Screen.Summary` exists and resolves.
    - Bottom navigation bar shows 5 items: Dashboard, Transactions, Summary, Insights, Settings.
    - `./gradlew :app:assembleDebug` produces a debug APK with no compile/Hilt errors.
    - `git diff MoneyManager/app/build.gradle.kts` is empty.
    - `gitnexus_detect_changes` reports the modified scope = `MoneyManagerNavHost.kt` + new `summary/*.kt` files only.
  </done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <what-built>
    - New Summary screen accessible from the bottom navigation bar (5th icon set: Dashboard / Transactions / Summary / Insights / Settings)
    - Header with "Summary" title + calendar + filter icons + Day/Week/Month/Custom chips + period navigator
    - Net Balance card with Income, Expense, Net Balance, and trend indicator vs previous period
    - Tab row: Expense (default) / Income / Lending / Transfers / Savings
    - Expense tab content: Total Spent + Budget Remaining + circular progress, Canvas-based pie chart of category spend, Top 5 Categories with progress bars, Top 5 Budget Utilization rows
    - Filter modal sheet for accounts, categories, and transaction types
    - Empty state with "Add Transaction" CTA when no transactions exist for the period
    - All values dynamically derived from `TransactionEntity` records using `getTransactionsByDateRange()` and excluding `isSplitChild=true` rows
  </what-built>
  <how-to-verify>
    Install the debug APK on a device or emulator (`./gradlew :app:installDebug`), then:

    1. **Navigation:** Tap the "Summary" icon in the bottom navigation. Confirm the Summary screen opens.
    2. **Net balance correctness:** Note the displayed Income, Expense, and Net Balance. Open the Dashboard for the same period (Month) and confirm Income/Expense match within rounding (they read the same DB).
    3. **Time filter reactivity:** Tap the "Day" chip — values should change. Tap "Week" — values change again. Tap forward/back chevrons — period label updates and figures update.
    4. **Tabs:** Tap each of the 5 tabs (Expense, Income, Lending, Transfers, Savings) and confirm Expense is the default and the tab indicator moves correctly.
    5. **Expense tab content:** On the Expense tab, confirm:
       - Total Spent matches the Net Balance card's Expense value
       - Pie chart renders with at least one slice if you have any expenses; legend is readable
       - Top 5 Categories list shows up to 5 rows sorted by spend desc, with progress bars and percentages summing to ≤100
       - Top 5 Budget Utilization shows rows only for categories with a budget configured for the active month; a category over budget shows the bar in error/red color
    6. **Filter sheet:** Tap the filter icon → sheet opens. Select one account → tap Apply → all values on the screen recompute. Open again → tap Clear → values reset.
    7. **Empty state:** Switch to a future month with no transactions. Confirm "No transactions available" + "Add Transaction" button is shown. Tap the CTA → Transactions screen should open.
    8. **Theme:** Toggle dark mode in Settings → return to Summary → confirm all text remains readable and contrast is preserved (PRD §3.4, §10).
    9. **Split children excluded:** If you have a split-parent transaction in the active month, verify Total Spent equals the parent amount only (not parent + children).
  </how-to-verify>
  <resume-signal>Type "approved" or describe issues found (which step failed and observed vs expected).</resume-signal>
</task>

</tasks>

<verification>
1. `cd MoneyManager && ./gradlew :app:assembleDebug` produces a debug APK with zero errors.
2. `gitnexus_detect_changes({scope: "all"})` reports only the expected files in scope:
   - `MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt`
   - 11 new files under `MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/`
3. `git diff MoneyManager/app/build.gradle.kts` is empty (no new dependencies).
4. Spot-check by `grep`:
   - `grep -r "getAllTransactions" MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/` returns zero matches.
   - `grep -r "isSplitChild" MoneyManager/app/src/main/java/com/moneymanager/app/ui/summary/` returns at least one match (in `SummaryAggregator.kt`).
   - `grep -r "MPAndroidChart\|com.github.mikephil" MoneyManager/app/src/` returns zero matches anywhere.
5. The human-verify checkpoint passes (steps 1-9 of the checkpoint above).
</verification>

<success_criteria>
- User can navigate to a "Summary" tab in bottom navigation.
- Summary screen renders Income, Expense, Net Balance for current month, derived from real transactions.
- 5 tabs render; Expense is default and shows pie chart + top 5 categories + top 5 budget utilization.
- Time filter (Day/Week/Month/Custom) and account/category/type filter both update the screen reactively.
- Empty state with "Add Transaction" CTA appears when there are no transactions for the period.
- `isSplitChild=true` rows are excluded from every aggregation (verified by code grep + manual split-transaction test).
- `./gradlew :app:assembleDebug` succeeds.
- No new gradle dependencies; pie chart is a Canvas composable.
</success_criteria>

<output>
After completion, create `.planning/quick/260504-dsa-implement-summary-screen-for-personal-fi/260504-dsa-SUMMARY.md` with: files created/modified, key decisions made (e.g. how non-Expense tabs are stubbed), any deviations from this plan, and links to the relevant commits.
</output>
