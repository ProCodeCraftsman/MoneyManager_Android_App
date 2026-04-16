---
phase: 8-dashboard
plan: 1
type: execute
wave: 1
depends_on: []
files_modified:
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt
autonomous: true

must_haves:
  truths:
    - "User can filter dashboard data by Day/Week/Month/Year/All/Custom time periods"
    - "Stats cards (Net Worth, Income, Expense, Net) update reactively when filter changes"
    - "Pie chart shows expense breakdown with category drill-down on click"
    - "Budget widget displays current month budgets with progress indicators"
    - "Reminders widget shows upcoming recurring transactions"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TimeFilterBar.kt"
      provides: "Time filter pills UI"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/BudgetWidget.kt"
      provides: "Budget progress display on dashboard"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/RemindersWidget.kt"
      provides: "Upcoming recurring transactions display"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/CategoryDrilldownPanel.kt"
      provides: "Category transaction list on pie chart click"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/ExpensePieChart.kt"
      provides: "Click handler for category drill-down"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt"
      provides: "Filter state, date range logic, budget/recurring data loading"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/ui/screens/DashboardScreen.kt"
      provides: "Filter bar, widgets integration"
---

<objective>
Implement Phase 8: Dashboard Enhancements - adding time filters, reactive stats, pie chart drill-down, budget widget, and reminders to the dashboard.
</objective>

<context>
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/ExpensePieChart.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/BudgetRepository.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/RecurringRepository.kt
</context>

<tasks>

<task type="viewmodel">
  <name>Task 1: Add TimeFilter state and date range logic to ViewModel</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</files>
  <action>
Add TimeFilter enum and MutableStateFlow for selected filter. Add custom date range state. Implement getDateRangeForFilter() method that calculates start/end dates based on selected filter. Update the uiState combine() to use dynamic date range instead of monthStart/monthEnd.

TimeFilter enum values: DAY, WEEK, MONTH, YEAR, ALL, CUSTOM (from BRIDGE.md locked decision)

Net Worth should remain all-time (no date filter), while Income/Expense/Net respect the selected time filter.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>DashboardUiState includes selectedFilter, customStartDate, customEndDate. Date range calculates correctly for all filter types.</done>
</task>

<task type="ui">
  <name>Task 2: Create TimeFilterBar component</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TimeFilterBar.kt</files>
  <action>
Create TimeFilterBar.kt with TimeFilter enum and horizontal Row of FilterChip components. Use Material3 FilterChip from androidx.compose.material3. Display name mapping: DAY->"Day", WEEK->"Week", MONTH->"Month", YEAR->"Year", ALL->"All", CUSTOM->"Custom".

Add DatePickerDialog for CUSTOM filter when user clicks it - show date range picker to select start and end dates.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>TimeFilterBar renders all 6 filter options with correct display names. Clicking CUSTOM opens date picker.</done>
</task>

<task type="ui">
  <name>Task 3: Update DashboardScreen to integrate TimeFilterBar</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/ui/screens/DashboardScreen.kt</files>
  <action>
Import and add TimeFilterBar to DashboardScreen UI - place at top above stats cards. Wire selectedFilter from uiState to TimeFilterBar and handle onFilterSelected callback to ViewModel. Pass custom dates to ViewModel when CUSTOM filter is selected.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>Dashboard shows filter pills above stats cards. Filter selection updates ViewModel state.</done>
</task>

<task type="viewmodel">
  <name>Task 4: Add category drill-down query to ViewModel</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</files>
  <action>
Add selectedCategory: PieChartEntry? and categoryTransactions: List<TransactionEntity> to DashboardUiState. Add method to query transactions by category + current date range. Create combine() block or separate flow for category drill-down data.

Category mapping: categoryId string -> category name (Food, Transport, Shopping, Bills, Entertainment, Health, Other)
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>ViewModel provides category transactions when a category is selected.</done>
</task>

<task type="ui">
  <name>Task 5: Create CategoryDrilldownPanel component</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/CategoryDrilldownPanel.kt</files>
  <action>
Create CategoryDrilldownPanel.kt - modal bottom sheet or Card showing transactions for selected category. Display: category name header, list of transactions with date, amount, note. Include close/dismiss button. Use existing TransactionEntity from data layer.

Wire to DashboardScreen - show panel when selectedCategory is not null.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>CategoryDrilldownPanel shows transaction list when category is clicked in pie chart.</done>
</task>

<task type="ui">
  <name>Task 6: Update ExpensePieChart with click handler</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/ExpensePieChart.kt</files>
  <action>
Add optional onCategoryClick: ((PieChartEntry) -> Unit)? parameter to ExpensePieChart. Make legend items clickable - add clickable modifier to Row in PieChartLegendItem. When clicked, invoke onCategoryClick callback with the entry.

Add clickable modifier from androidx.compose.foundation to legend items.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>Clicking a category in the pie chart legend triggers onCategoryClick callback.</done>
</task>

<task type="viewmodel">
  <name>Task 7: Add budget loading with progress calculation to ViewModel</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</files>
  <action>
Inject BudgetRepository into DashboardViewModel. Add budgetsWithProgress: List<BudgetWithProgress> to DashboardUiState. Create BudgetWithProgress data class with budget, categoryName, spent, percentage.

In combine(): getActiveBudgets() from BudgetRepository, get current month transactions, calculate spent per category. Progress colors: <80% green, 80-100% amber (0xFFB8860B), >100% red (MaterialTheme colorScheme.error).

Current month dates: use monthStart/monthEnd from existing logic for budget calculation.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>DashboardUiState includes budgetsWithProgress with correct percentage calculations.</done>
</task>

<task type="ui">
  <name>Task 8: Create BudgetWidget component</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/BudgetWidget.kt</files>
  <action>
Create BudgetWidget.kt - Card showing budget list with progress bars. Display: "Budgets - [Current Month]" title, category name, spent/budget amount, LinearProgressIndicator with color based on percentage.

Progress colors: >100% -> MaterialTheme.colorScheme.error, >=80% -> Color(0xFFB8860B) (amber), else -> Color(0xFF2A6049) (green). Use currency format for amounts.

Place below pie chart in DashboardScreen.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>BudgetWidget displays all active budgets with progress bars in correct colors.</done>
</task>

<task type="viewmodel">
  <name>Task 9: Add reminders loading to ViewModel</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</files>
  <action>
Inject RecurringRepository into DashboardViewModel. Add upcomingRecurring: List<RecurringEntity> to DashboardUiState. Query active recurring where nextDate <= (current time + 7 days). Order by nextDate ascending, limit 5.

Use existing RecurringEntity from data layer.
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>DashboardUiState includes upcoming recurring transactions within 7 days.</done>
</task>

<task type="ui">
  <name>Task 10: Create RemindersWidget component</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/RemindersWidget.kt</files>
  <action>
Create RemindersWidget.kt - gold-themed Card showing upcoming recurring items. Use CardDefaults.cardColors with Color(0xFFF5ECD0) background.

Display: "UPCOMING" label (labelSmall, Color(0xFFB8860B)), list of recurring items with note, formatted next date (e.g., "Tomorrow", "Apr 20"), amount.

Show up to 5 items. If list is empty, show nothing (empty composable). Format nextDate: if tomorrow -> "Tomorrow", else -> "MMM dd".
  </action>
  <verify>
  <automated>./gradlew :app:compileDebugKotlin --quiet 2>&1 | head -20</automated>
  </verify>
  <done>RemindersWidget shows upcoming recurring with gold styling when data exists.</done>
</task>

<task type="ui">
  <name>Task 11: Final DashboardScreen integration</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/ui/screens/DashboardScreen.kt</files>
  <action>
Update DashboardScreen to use all new components in correct order:
1. TimeFilterBar at top
2. Stats cards (Net Worth, Income, Expense, Net) below filter
3. ExpensePieChart with onCategoryClick -> sets selectedCategory
4. CategoryDrilldownPanel (ModalBottomSheet) when selectedCategory != null
5. BudgetWidget below pie chart
6. RemindersWidget at bottom

Wire all callbacks and state. Use LazyColumn for scrollable content.
  </action>
  <verify>
  <automated>./gradlew :app:assembleDebug --quiet 2>&1 | tail -30</automated>
  </verify>
  <done>Complete dashboard with all 5 features working: filter pills, reactive stats, clickable pie chart, budget widget, reminders.</done>
</task>

</tasks>

<verification>
- [ ] Time filter pills (Day/Week/Month/Year/All/Custom) visible on dashboard
- [ ] Selecting Week filter updates income/expense to last 7 days
- [ ] Selecting Year filter shows full year data
- [ ] Clicking pie chart category opens drill-down panel
- [ ] Budget widget shows <80% green, 80-100% amber, >100% red
- [ ] Reminders widget shows gold-themed upcoming items
- [ ] Build compiles successfully
</verification>

<success_criteria>
- Filter changes update all stats cards within 200ms
- All 6 time filters (Day/Week/Month/Year/All/Custom) functional
- Pie chart click opens category drill-down with transaction list
- Budget progress colors match spec (<80% green, 80-100% amber, >100% red)
- Reminders show upcoming within 7 days with gold styling
</success_criteria>

<output>
After completion, create `.planning/phases/8-dashboard/8-01-SUMMARY.md`
</output>