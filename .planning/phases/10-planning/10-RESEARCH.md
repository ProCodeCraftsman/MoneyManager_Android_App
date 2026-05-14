# Phase 10: Budgets, Goals, Templates - Research

**Researched:** 2026-04-14
**Domain:** Android Jetpack Compose - Budget tracking, Savings goals, Transaction templates
**Confidence:** HIGH

## Summary

This phase implements budget progress visualization, savings goals enhancements, and transaction templates. The app already has the data layer for all three features, but the UI implementation is incomplete. Budgets need color-coded progress bars and support for savings/investment categories. Goals need manual contributions, savings transaction linking, and deadline countdowns. Templates need a UI screen to view and use them.

**Primary recommendation:** Implement all 6 features from BRIDGE.md using existing data entities, adding UI screens and ViewModel methods. Budgets and Goals need ViewModel updates for dynamic data; Templates needs a new screen.

## Current State

### Budgets (Partial Implementation)
- **BudgetsScreen.kt** - Basic UI with hardcoded progress (50%)
- **BudgetsViewModel** - Filters only `expense` type categories
- **BudgetEntity** - Has categoryId, amount, month fields
- **Missing:** Dynamic progress calculation, color states, savings/investment category support

### Goals (Partial Implementation)
- **GoalsScreen.kt** - Basic goal cards with progress
- **GoalsViewModel** - Only `addGoal()` method
- **GoalEntity** - Has deadline field but not used in UI
- **Missing:** Manual contributions, savings transaction linking, deadline countdown display

### Templates (Backend Ready, UI Missing)
- **TemplateEntity** - Complete: name, type, amount, categoryId, note
- **TemplateDao** - Full CRUD operations
- **TemplateRepository** - Interface and implementation exist
- **Missing:** TemplatesScreen UI, "Use Template" functionality

### Transaction Entity
- Already supports `type = "savings"` for savings transactions
- No goal linking field yet

## Implementation Approach

### 1. Budget Progress Bar with Color States

**Files to modify:**
- `BudgetsViewModel.kt` - Add spending calculation per budget
- `BudgetsScreen.kt` - Update progress bar with dynamic colors

**Implementation:**
- Query transactions by category for current month
- Calculate `spent = SUM(transactions WHERE type='expense' AND categoryId=budget.categoryId)`
- Compute `progress = spent / budget.amount`
- Apply color: `<0.8` = Green, `0.8-1.0` = Amber, `>1.0` = Red

### 2. Savings Targets for Investment Categories

**Files to modify:**
- `BudgetsViewModel.kt` - Include savings/investment categories in budget list

**Implementation:**
- Change category filter from `type == "expense"` to include savings types
- BudgetsViewModel should query all categories where type IN ('expense', 'savings', 'investment')

### 3. Goal Contributions (Manual Add)

**Files to modify:**
- `GoalsViewModel.kt` - Add `addContribution(goalId, amount)` method
- `GoalsScreen.kt` - Add "Add Money" button/dialog to GoalCard
- `GoalDao.kt` - Add update method for currentAmount

**Implementation:**
- Add button on GoalCard to add contribution
- Dialog with amount input
- Update GoalEntity.currentAmount += contribution

### 4. Link Savings Transactions to Goals

**Files to modify:**
- `GoalEntity.kt` - Add optional `linkedGoalId` field (or use TransactionEntity)
- `TransactionEntity.kt` - Add optional `goalId` field
- `GoalsViewModel.kt` - Query and aggregate savings by linked goal

**Implementation:**
- Add `goalId: Long?` to TransactionEntity
- When creating savings transaction, optionally link to a goal
- GoalsViewModel sums all savings transactions linked to each goal
- Update currentAmount automatically

### 5. Goal Target Date with Countdown

**Files to modify:**
- `GoalsScreen.kt` - Update GoalCard to show deadline and countdown

**Implementation:**
- If goal.deadline != null, display formatted date
- Calculate `daysRemaining = (deadline - now) / (1000 * 60 * 60 * 24)`
- Show "X days remaining" or "X days overdue"
- Update AddGoalDialog to include date picker

### 6. Transaction Templates

**New Files:**
- `TemplatesScreen.kt` - List all templates
- `TemplatesViewModel.kt` - Manage template operations
- Modify `TransactionsScreen.kt` - Add "Use Template" option

**Implementation:**
- Create TemplatesScreen with LazyColumn of template cards
- FAB to add new template
- On TransactionsScreen, add button to load template and pre-fill AddTransactionDialog

## Files to Modify

| File | Action | Changes |
|------|--------|---------|
| BudgetsViewModel.kt | Modify | Add spending calculation, include savings categories |
| BudgetsScreen.kt | Modify | Dynamic progress, color states |
| GoalsViewModel.kt | Modify | Add contribution method, link savings |
| GoalsScreen.kt | Modify | Add contribution dialog, deadline countdown |
| GoalEntity.kt | Modify | Add goalId field for linking |
| TransactionEntity.kt | Modify | Add goalId field |
| MoneyManagerNavHost.kt | Modify | Add TemplatesScreen route |

## New Files to Create

| File | Purpose |
|------|---------|
| TemplatesScreen.kt | List and manage templates |
| TemplatesViewModel.kt | Template CRUD operations |

## Dependencies

- All required dependencies (Room, Hilt, Compose) already in project
- No new dependencies needed

## Common Pitfalls

### Budget Color States
- **Pitfall:** Forgetting to handle >100% (cap at 1.0 for display, but show "over budget" text)
- **Fix:** Use `progress.coerceIn(0f, 1f)` for bar, show actual percentage for text

### Goal Date Handling
- **Pitfall:** Not handling null deadlines
- **Fix:** Check `deadline != null` before showing countdown

### Template Linking
- **Pitfall:** Not updating transaction after selecting template
- **Fix:** Pass template data as parameters to AddTransactionDialog

## Architecture Patterns

### MVVM with Hilt
- ViewModels handle business logic
- Repositories handle data access
- Compose UI observes StateFlow

### StateFlow Pattern
```kotlin
// Example: BudgetsViewModel
val uiState: StateFlow<BudgetsUiState> = combine(
    budgetRepository.getBudgetsByPeriod(currentMonth),
    transactionRepository.getTransactionsByDateRange(monthStart, monthEnd),
    categoryRepository.getAllCategories()
) { budgets, transactions, categories ->
    // Calculate spending per budget
    // Map to UI state with progress
}.stateIn(...)
```

## Code Examples

### Color-coded Progress Indicator
```kotlin
@Composable
fun BudgetProgressBar(spent: Double, budget: Double) {
    val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = if (budget > 0) (spent / budget * 100) else 0.0
    
    val color = when {
        percentage < 80 -> Color(0xFF4CAF50)  // Green
        percentage < 100 -> Color(0xFFFFC107) // Amber
        else -> Color(0xFFF44336)              // Red
    }
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth(),
        color = color,
        trackColor = color.copy(alpha = 0.2f)
    )
}
```

### Goal Countdown
```kotlin
@Composable
fun GoalCountdown(deadline: Long?) {
    if (deadline == null) return
    
    val daysRemaining = ((deadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val text = when {
        daysRemaining > 0 -> "$daysRemaining days remaining"
        daysRemaining == 0 -> "Due today!"
        else -> "${-daysRemaining} days overdue"
    }
    
    Text(text = text, color = if (daysRemaining < 0) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant)
}
```

## Open Questions

1. **Goal linking strategy:** Should savings transactions auto-link to goals based on category, or require manual selection?
   - Recommendation: Add optional goal picker in AddTransactionDialog when type="savings"

2. **Budget period for savings:** Should savings budgets track differently (cumulative vs monthly)?
   - Recommendation: Treat same as expense budgets (monthly) for simplicity first

## Sources

### Primary (HIGH confidence)
- Code inspection: BudgetsScreen.kt, GoalsScreen.kt, TemplateEntity.kt
- Room entities: BudgetEntity, GoalEntity, TransactionEntity, TemplateEntity

### Secondary (MEDIUM confidence)
- Previous phase implementations: Phase 7-9 patterns for screen/viewmodel structure

### Tertiary (LOW)
- N/A - Direct code analysis sufficient

---

**Research date:** 2026-04-14
**Valid until:** 90 days (stable feature set)
