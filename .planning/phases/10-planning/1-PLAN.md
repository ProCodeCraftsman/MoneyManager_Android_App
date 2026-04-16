---
phase: 10-planning
plan: 1
type: execute
wave: 1
depends_on: []
files_modified:
  - MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsViewModel.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsViewModel.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt
files_created:
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesViewModel.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesScreen.kt
autonomous: true
requirements:
  - BGT-01: Budget progress bar with color states (<80% green, 80-100% amber, >100% red)
  - BGT-02: Savings targets for investment categories
  - BGT-03: Goal contributions (manual add)
  - BGT-04: Link savings transactions to goals (goalId field in TransactionEntity)
  - BGT-05: Goal target date with countdown
  - BGT-06: Transaction templates UI + TemplatesScreen

must_haves:
  truths:
    - "Budget progress bars show color-coded state based on spending percentage"
    - "Budgets include savings and investment category types"
    - "Users can manually add money contributions to goals"
    - "Savings transactions can be linked to goals via goalId"
    - "Goals show deadline countdown when deadline is set"
    - "Templates screen displays all saved transaction templates"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt"
      provides: "goalId field for linking savings to goals"
      contains: "goalId: Long? = null"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsViewModel.kt"
      provides: "Dynamic spending calculation per budget"
      computes: "spent = SUM(transactions WHERE type='expense' AND categoryId=budget.categoryId)"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt"
      provides: "Color-coded progress bars"
      color_logic: "<80%=green, 80-100%=amber, >100%=red"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsViewModel.kt"
      provides: "addContribution() method"
      updates: "GoalEntity.currentAmount += contribution"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt"
      provides: "Add Contribution dialog, Deadline countdown"
      shows: "X days remaining, X days overdue"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesScreen.kt"
      provides: "Template management UI"
      features: "List, Add, Use templates"
  key_links:
    - from: "BudgetsViewModel.kt"
      to: "TransactionDao.kt"
      via: "getTransactionsByCategory()"
      pattern: "getTotalExpense()"
    - from: "GoalsViewModel.kt"
      to: "GoalRepository"
      via: "updateGoal()"
      pattern: "goalRepository.updateGoal()"
    - from: "MoneyManagerNavHost.kt"
      to: "TemplatesScreen.kt"
      via: "navController.navigate()"
      route: "templates"
---

<objective>
Implement Phase 10: Budgets, Goals, Templates features.

Purpose: Enhance budget progress visualization with color-coded states, enable goal contributions and savings linking, and add a templates management screen.

Output: 
- TransactionEntity with goalId field
- BudgetsViewModel/BudgetsScreen with dynamic color-coded progress
- GoalsViewModel/GoalsScreen with contributions and countdown
- TemplatesScreen with full CRUD
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/10-planning/10-RESEARCH.md
@MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt
@MoneyManager/app/src/main/java/com/moneymanager/data/entity/GoalEntity.kt
@MoneyManager/app/src/main/java/com/moneymanager/data/entity/BudgetEntity.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsViewModel.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsViewModel.kt
@MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TemplateRepository.kt

<interfaces>
<!-- Key types the executor needs - from current codebase -->

TransactionEntity (needs goalId field added):
```kotlin
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: String, // income, expense, savings, transfer
    val amount: Double,
    val categoryId: Long? = null,
    val goalId: Long? = null,  // NEW - to link savings to goals
    // ... other fields
)
```

GoalEntity (has deadline field - already exists):
```kotlin
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "🎯",
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,  // Already exists - use for countdown
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
```

TemplateEntity (already complete):
```kotlin
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // income, expense, savings
    val amount: Double = 0.0,
    val categoryId: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
```

TemplateRepository (already exists):
```kotlin
interface TemplateRepository {
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    suspend fun getTemplateById(id: Long): TemplateEntity?
    suspend fun insertTemplate(template: TemplateEntity): Long
    suspend fun updateTemplate(template: TemplateEntity)
    suspend fun deleteTemplate(template: TemplateEntity)
}
```
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Add goalId field to TransactionEntity</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt</files>
  <action>
    Add goalId field to TransactionEntity for linking savings transactions to goals.
    
    Modify the TransactionEntity data class to add:
    - `val goalId: Long? = null` - Optional goal ID to link savings to a goal
    
    This enables feature BGT-04: Link savings transactions to goals.
  </action>
  <verify>
    <automated>Ran with --dry-run; verify TransactionEntity has goalId field</automated>
  </verify>
  <done>TransactionEntity includes goalId: Long? = null field</done>
</task>

<task type="auto">
  <name>Task 2: Update BudgetsViewModel with dynamic spending calculation</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsViewModel.kt</files>
  <action>
    Update BudgetsViewModel to:
    1. Query transactions to calculate actual spending per budget
    2. Include ALL category types (expense, savings, investment) - not just expense
    3. Compute progress for each budget

    Modify the uiState to include spending data:
    ```kotlin
    data class BudgetWithSpending(
        val budget: BudgetEntity,
        val category: CategoryEntity?,
        val spent: Double  // Calculated from transactions
    )
    
    data class BudgetsUiState(
        val budgetsWithSpending: List<BudgetWithSpending> = emptyList(),
        val categories: List<CategoryEntity> = emptyList(),
        val isLoading: Boolean = true,
        val currentMonth: String = "yyyy-MM"
    )
    ```

    Use SimpleDateFormat to compute month date range (start of month to end of month).
    Query TransactionDao for: `getTotalExpense(startDate, endDate)` filtered by categoryId.
  </action>
  <verify>
    <automated>Ran with --dry-run; verify BudgetsUiState includes spending calculations</automated>
  </verify>
  <done>BudgetsViewModel computes spending per budget from transactions</done>
</task>

<task type="auto">
  <name>Task 3: Update BudgetsScreen with color-coded progress bars</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt</files>
  <action>
    Update BudgetsScreen to display dynamic progress with color states:
    
    1. For each budget, calculate: progress = spent / budget.amount, percentage = (spent/budget.amount)*100
    2. Apply color based on percentage:
       - < 80% → Green (0xFF4CAF50)
       - 80-100% → Amber (0xFFFFC107)  
       - > 100% → Red (0xFFF44336)
    3. Show actual percentage text: "65% used", "95% used", "120% over budget"
    
    Use the code from 10-RESEARCH.md as reference:
    ```kotlin
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = if (budget.amount > 0) (spent / budget.amount * 100) else 0.0
    
    val color = when {
        percentage < 80 -> Color(0xFF4CAF50)  // Green
        percentage < 100 -> Color(0xFFFFC107)   // Amber
        else -> Color(0xFFF44336)              // Red
    }
    ```

    Import Color from androidx.compose.ui.graphics.
  </action>
  <verify>
    <automated>Ran with --dry-run; verify BudgetsScreen shows color-coded progress</automated>
  </verify>
  <done>Budget progress bars show green (<80%), amber (80-100%), red (>100%)</done>
</task>

<task type="auto">
  <name>Task 4: Update GoalsViewModel with addContribution method</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsViewModel.kt</files>
  <action>
    Update GoalsViewModel to add contribution functionality:
    
    1. Add `addContribution(goalId: Long, amount: Double)` method that:
       - Fetches the goal from repository
       - Updates goal.currentAmount += amount
       - Saves to repository
    
    2. Update uiState to include spending calculation:
       - Query transactions linked to each goal (type='savings', goalId=goal.id)
       - Sum linked transactions for total contribution display
    
    The GoalRepository should have update capability via GoalDao.
    Use viewModelScope.launch for async operations.
  </action>
  <verify>
    <automated>Ran with --dry-run; verify GoalsViewModel has addContribution() method</automated>
  </verify>
  <done>GoalsViewModel provides addContribution(goalId, amount) method</done>
</task>

<task type="auto">
  <name>Task 5: Update GoalsScreen with contribution dialog and countdown</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt</files>
  <action>
    Update GoalsScreen in two places:
    
    **A. Add Contribution Dialog to GoalCard:**
    - Add an IconButton with "Add Money" icon (Icons.Default.Add) on each GoalCard
    - When tapped, show dialog with amount input field
    - On confirm: call viewModel.addContribution(goal.id, amount)
    
    **B. Add Deadline Countdown to GoalCard:**
    - If goal.deadline != null, display days remaining
    - Calculate: daysRemaining = (deadline - now) / (1000 * 60 * 60 * 24)
    - Display text:
      - daysRemaining > 0: "X days remaining"
      - daysRemaining == 0: "Due today!"
      - daysRemaining < 0: "X days overdue" (color red)
    
    Use code from 10-RESEARCH.md:
    ```kotlin
    val daysRemaining = ((deadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val text = when {
        daysRemaining > 0 -> "$daysRemaining days remaining"
        daysRemaining == 0 -> "Due today!"
        else -> "${-daysRemaining} days overdue"
    }
    ```

    **C. Update AddGoalDialog:**
    - Add optional date picker for deadline field
    - Use OutlinedTextField with read-only and DatePickerDialog
  </action>
  <verify>
    <automated>Ran with --dry-run; verify GoalsScreen has contribution button and countdown</automated>
  </verify>
  <done>Goals show "Add Money" button and deadline countdown</done>
</task>

<task type="auto">
  <name>Task 6: Create TemplatesViewModel</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesViewModel.kt</files>
  <action>
    Create TemplatesViewModel for managing transaction templates.
    
    Structure similar to other ViewModels:
    ```kotlin
    @HiltViewModel
    class TemplatesViewModel @Inject constructor(
        private val templateRepository: TemplateRepository,
    ) : ViewModel() {
    
        val uiState: StateFlow<TemplatesUiState> = templateRepository.getAllTemplates()
            .map { templates ->
                TemplatesUiState(templates = templates, isLoading = false)
            }
            .stateIn(...)
        
        fun addTemplate(name: String, type: String, amount: Double, categoryId: Long?, note: String) {
            viewModelScope.launch {
                templateRepository.insertTemplate(
                    TemplateEntity(name=name, type=type, amount=amount, categoryId=categoryId, note=note)
                )
            }
        }
        
        fun deleteTemplate(template: TemplateEntity) {
            viewModelScope.launch {
                templateRepository.deleteTemplate(template)
            }
        }
    }
    
    data class TemplatesUiState(
        val templates: List<TemplateEntity> = emptyList(),
        val isLoading: Boolean = true
    )
    ```
    
    Include methods for: addTemplate, deleteTemplate.
  </action>
  <verify>
    <automated>Ran with --dry-run; verify TemplatesViewModel created</automated>
  </verify>
  <done>TemplatesViewModel provides template CRUD operations</done>
</task>

<task type="auto">
  <name>Task 7: Create TemplatesScreen</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesScreen.kt, MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</files>
  <action>
    Create TemplatesScreen UI and add route to NavHost.
    
    **TemplatesScreen.kt:**
    - Scaffold with TopAppBar title "Templates"
    - LazyColumn listing all templates
    - Each template card shows: name, type icon, amount, category, note
    - FAB to add new template
    - Dialog for adding/editing templates
    
    Use similar structure to other screens (GoalsScreen, BudgetsScreen).
    
    **MoneyManagerNavHost.kt:**
    - Add new route: `data object Templates : Screen("templates", "Templates", Icons.Default.Description)`
    - Add composable: `composable(Screen.Templates.route) { TemplatesScreen(...) }`
    - Add to bottom nav (or as secondary nav item)
  </action>
  <verify>
    <automated>Ran with --dry-run; verify TemplatesScreen displays templates</automated>
  </verify>
  <done>TemplatesScreen shows template list with add/delete functionality</done>
</task>

</tasks>

<verification>
- [ ] TransactionEntity has goalId field
- [ ] BudgetsViewModel calculates spending per budget
- [ ] BudgetsScreen shows color-coded progress bars
- [ ] GoalsViewModel has addContribution method
- [ ] GoalsScreen shows contribution dialog and deadline countdown
- [ ] TemplatesViewModel exists with CRUD
- [ ] TemplatesScreen exists and is navigable
- [ ] Build compiles without errors
</verification>

<success_criteria>
1. Budget progress bars display:
   - Green color when < 80% spent
   - Amber color when 80-100% spent
   - Red color when > 100% spent
   
2. Budgets include savings and investment category types

3. Goals have "Add Money" button that opens contribution dialog

4. Savings transactions can be linked to goals via goalId field

5. Goals with deadlines show countdown ("X days remaining" or "X days overdue")

6. TemplatesScreen displays all saved templates with add/edit capability
</success_criteria>

<output>
After completion, create `.planning/phases/10-planning/10-01-SUMMARY.md`
</output>