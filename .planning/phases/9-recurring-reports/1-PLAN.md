---
phase: 9-recurring-reports
plan: 1
type: execute
wave: 1
depends_on: []
files_modified:
  - MoneyManager/app/build.gradle.kts
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt
autonomous: false

must_haves:
  truths:
    - "Recurring transactions auto-generate into actual transactions daily"
    - "User can view all recurring transactions in a list"
    - "User can create new recurring transaction with frequency settings"
    - "User can enable/disable recurring transactions"
    - "Reports screen shows category breakdown as horizontal bar chart"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/worker/RecurringGenerationWorker.kt"
      provides: "Background worker that creates transactions from due recurring entries"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringViewModel.kt"
      provides: "ViewModel managing recurring transactions list and CRUD operations"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringListScreen.kt"
      provides: "UI showing all recurring transactions with enable/disable toggle"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringFormScreen.kt"
      provides: "Form UI for creating/editing recurring transactions"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/CategoryBarChart.kt"
      provides: "Horizontal bar chart component for category breakdown"
  key_links:
    - from: "RecurringGenerationWorker"
      to: "RecurringDao.getDueRecurring()"
      via: "suspend function call"
      pattern: "recurringDao\\.getDueRecurring"
    - from: "RecurringGenerationWorker"
      to: "TransactionDao.insertTransaction()"
      via: "for each due recurring"
      pattern: "transactionDao\\.insertTransaction"
    - from: "RecurringListScreen"
      to: "RecurringViewModel"
      via: "ViewModel injection"
      pattern: "hiltViewModel.*RecurringViewModel"
    - from: "RecurringFormScreen"
      to: "RecurringViewModel"
      via: "saveRecurring() call"
      pattern: "viewModel\\.saveRecurring"
    - from: "CategoryBarChart"
      to: "ReportsScreen"
      via: "CategoriesTab composable"
      pattern: "CategoryBarChart.*entries"
---

<objective>
Implement recurring transactions auto-generation and management screens, plus enhance Reports with category bar chart.

Purpose: Complete the recurring transactions feature (Phase 4 gap) and enhance reporting with additional chart type.
Output: Working recurring auto-generation worker, management screens, and category bar chart in Reports.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/9-recurring-reports/9-RESEARCH.md

# Key existing code patterns:

From MoneyManagerNavHost.kt:
```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector?) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    // ... other screens
    data object Transfer : Screen("transfer", "Transfer", null)
}

composable(Screen.Transfer.route) {
    TransferScreen(viewModel = hiltViewModel(), onNavigateBack = { navController.popBackStack() })
}
```

From TransactionsViewModel.kt:
```kotlin
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    val uiState: StateFlow<TransactionsUiState> = combine(...) { ... }
}
```

From RecurringEntity.kt:
```kotlin
data class RecurringEntity(
    val id: Long = 0,
    val accountId: Long,
    val type: String, // income, expense, savings
    val amount: Double,
    val categoryId: Long? = null,
    val note: String = "",
    val frequency: String, // daily, weekly, biweekly, monthly, yearly
    val nextDate: Long,
    val isActive: Boolean = true,
    val reminderEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

From RecurringDao.kt:
```kotlin
@Query("SELECT * FROM recurring WHERE isActive = 1 AND nextDate <= :date")
suspend fun getDueRecurring(date: Long): List<RecurringEntity>
```

From TransactionEntity.kt:
```kotlin
val isRecurring: Boolean = false,
val recurringId: Long? = null,
```

Existing dependencies in build.gradle.kts:
- Hilt 2.52
- Room 2.6.1
- Compose BOM 2024.12.01
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add WorkManager dependency to build.gradle.kts</name>
  <files>MoneyManager/app/build.gradle.kts</files>
  <action>
    Add WorkManager dependencies to the dependencies block:
    
    ```kotlin
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")
    ```
  </action>
  <verify>
    <automated>grep -l "work-runtime-ktx" MoneyManager/app/build.gradle.kts</automated>
  </verify>
  <done>WorkManager dependency added to build.gradle.kts</done>
</task>

<task type="auto">
  <name>Task 2: Create RecurringGenerationWorker</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/worker/RecurringGenerationWorker.kt</files>
  <action>
    Create the WorkManager worker that auto-generates transactions from recurring entries.

    Implementation:
    - Extend CoroutineWorker
    - Inject RecurringDao and TransactionDao via application context
    - Query getDueRecurring(System.currentTimeMillis())
    - For each due recurring:
      - Create TransactionEntity with isRecurring=true and recurringId set
      - Update RecurringEntity.nextDate based on frequency
    - Handle frequency: daily, weekly, biweekly, monthly, yearly
    - Return Result.success()

    Use existing TransactionEntity model - it already has isRecurring and recurringId fields.
  </action>
  <verify>
    <automated>test -f MoneyManager/app/src/main/java/com/moneymanager/app/worker/RecurringGenerationWorker.kt</automated>
  </verify>
  <done>Worker created at worker/RecurringGenerationWorker.kt with doWork() that creates transactions</done>
</task>

<task type="auto">
  <name>Task 3: Add Recurring routes to navigation</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</files>
  <action>
    Add to Screen sealed class:
    - data object Recurring : Screen("recurring", "Recurring", Icons.Default.Repeat)
    - data object RecurringForm : Screen("recurring_form?recurringId={recurringId}", "Recurring Form", null)

    Add composable entries:
    - Recurring screen -> RecurringListScreen(viewModel = hiltViewModel())
    - RecurringForm screen -> RecurringFormScreen(viewModel = hiltViewModel(), onNavigateBack = { navController.popBackStack() })

    Add Recurring to bottom nav screens list.
  </action>
  <verify>
    <automated>grep -c "Screen.Recurring" MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</automated>
  </verify>
  <done>Navigation includes Recurring and RecurringForm routes with bottom nav item</done>
</task>

<task type="auto">
  <name>Task 4: Create RecurringViewModel</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringViewModel.kt</files>
  <action>
    Create ViewModel with:
    - Inject RecurringRepository, AccountRepository, CategoryRepository
    - uiState: StateFlow<List<RecurringEntity>> from getAllRecurring()
    - accounts: StateFlow<List<AccountEntity>>
    - categories: StateFlow<List<CategoryEntity>>
    - saveRecurring(recurring: RecurringEntity)
    - deleteRecurring(recurring: RecurringEntity)
    - toggleActive(recurring: RecurringEntity) - updates isActive
    - getRecurringById(id: Long): RecurringEntity?

    Based on TransactionsViewModel and BudgetsViewModel patterns.
  </action>
  <verify>
    <automated>test -f MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringViewModel.kt</automated>
  </verify>
  <done>RecurringViewModel created with all CRUD operations</done>
</task>

<task type="auto">
  <name>Task 5: Create RecurringListScreen</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringListScreen.kt</files>
  <action>
    Create list screen:
    - Scaffold with TopAppBar title "Recurring Transactions"
    - FAB to navigate to RecurringFormScreen (empty recurringId)
    - LazyColumn of recurring items showing:
      - Note/name, amount (formatted), frequency label
      - Next date ("Next: [date]")
      - Switch to toggle isActive
    - Tap item -> navigate to RecurringFormScreen with recurringId
    - Swipe to delete with confirm dialog

    Use existing Card, Row, Column patterns from other screens.
  </action>
  <verify>
    <automated>test -f MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringListScreen.kt</automated>
  </verify>
  <done>RecurringListScreen shows all recurring with toggle and navigation to form</done>
</task>

<task type="auto">
  <name>Task 6: Create RecurringFormScreen</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringFormScreen.kt</files>
  <action>
    Create form screen:
    - Fields: amount (TextField), type (SegmentedButton: income/expense/savings),
      account (Dropdown), category (Dropdown, nullable), note (TextField),
      frequency (Dropdown: daily/weekly/biweekly/monthly/yearly),
      start date (DatePicker), reminder toggle
    - If recurringId provided -> load existing, otherwise new
    - Save button validates and calls viewModel.saveRecurring()
    - Delete button for editing mode

    Follow TransactionFormScreen patterns for form layout.
  </action>
  <verify>
    <automated>test -f MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringFormScreen.kt</automated>
  </verify>
  <done>RecurringFormScreen allows create/edit with all required fields</done>
</task>

<task type="auto">
  <name>Task 7: Create CategoryBarChart component</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/CategoryBarChart.kt</files>
  <action>
    Create horizontal bar chart component:
    - Composable function taking List<PieChartEntry> (reuse from ExpensePieChart)
    - Use LinearProgressIndicator for each category bar
    - Show category name, percentage bar, and amount
    - Calculate total and percentages

    Similar to budget progress bars in BudgetsTab.
  </action>
  <verify>
    <automated>test -f MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/CategoryBarChart.kt</automated>
  </done>
  <done>CategoryBarChart component created with horizontal bars</done>
</task>

<task type="auto">
  <name>Task 8: Add CategoryBarChart to Reports CategoriesTab</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt</files>
  <action>
    Import CategoryBarChart component.

    In CategoriesTab, add CategoryBarChart below the ExpensePieChart:
    - After the pie chart Card, add a section "Breakdown" with CategoryBarChart
    - Pass uiState.categoryBreakdown as entries
  </action>
  <verify>
    <automated>grep -c "CategoryBarChart" MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt</automated>
  </verify>
  <done>Reports screen CategoriesTab shows horizontal bar chart below pie chart</done>
</task>

<task type="checkpoint:human-verify" gate="blocking">
  <name>Task 9: Verify recurring transactions and reports feature</name>
  <files>All implemented files from Tasks 1-8</files>
  <what-built>Complete recurring feature implementation (worker, list screen, form screen) and category bar chart</what-built>
  <how-to-verify>
    1. Compile: ./gradlew assembleDebug
    2. Navigate to Recurring in bottom nav
    3. Create new recurring: set amount, type, account, frequency, start date
    4. Verify recurring appears in list with active toggle
    5. Toggle off -> verify switch updates
    6. Go to Reports -> Categories tab
    7. Verify bar chart appears below pie chart
  </how-to-verify>
  <action>Manual verification of complete feature implementation</action>
  <verify>
    <automated>Manual UI test by user</automated>
  </verify>
  <done>All features working: recurring worker, list, form, bar chart</done>
  <resume-signal>Type "approved" or describe issues</resume-signal>
</task>

</tasks>

<verification>
- [ ] build.gradle.kts includes WorkManager
- [ ] RecurringGenerationWorker.kt creates transactions from due recurring
- [ ] Navigation has Recurring and RecurringForm routes
- [ ] RecurringViewModel has all CRUD operations
- [ ] RecurringListScreen shows list with toggle
- [ ] RecurringFormScreen creates/edits recurring
- [ ] CategoryBarChart component renders horizontal bars
- [ ] Reports CategoriesTab includes bar chart
</verification>

<success_criteria>
- User can view all recurring transactions in list
- User can create recurring with amount, type, account, category, frequency, start date
- User can toggle recurring active/inactive
- WorkManager worker auto-creates transactions daily from due recurring
- Reports Categories tab shows horizontal bar chart for category breakdown
</success_criteria>

<output>
After completion, create `.planning/phases/9-recurring-reports/phase-1-SUMMARY.md`
</output>