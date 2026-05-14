---
phase: 11-budget-fix
plan: 1
goal: Fix BudgetsViewModel to include savings and investment categories
requirements:
  - BGT-02
tasks:
  - name: Update transaction filter in BudgetsViewModel
    files:
      - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsViewModel.kt
    action: Change line 55 filter to include type IN ("expense", "savings", "investment")
  - name: Update category filter in BudgetsViewModel
    files:
      - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsViewModel.kt
    action: Change line 67 filter to include savings and investment types
---

# Phase 11: Budget Filter Fix - Plan 1

## Gap Closure

This phase closes the BGT-02 gap from the milestone audit:
- Budgets exclude savings and investment categories

## Tasks

### Task 1: Update transaction filter
- File: `BudgetsViewModel.kt`
- Line 55: Change filter from `type == "expense"` to `type in listOf("expense", "savings", "investment")`

### Task 2: Update category filter  
- File: `BudgetsViewModel.kt`
- Line 67: Change filter from `type == "expense"` to `type in listOf("expense", "savings", "investment")`

## Verification

After completion, verify:
1. Build compiles: `./gradlew :app:assembleDebug`
2. Budgets show transactions of all types (expense, savings, investment)