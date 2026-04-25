# Budgets Screen - Functional Requirements Document

**Screen:** Budgets  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt`  
**ViewModel:** `BudgetsViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Budgets screen allows users to set monthly spending limits per category and track their progress against those limits.

---

## 2. Features

### 2.1 Budget Operations
| ID | Feature | Description |
|----|---------|-------------|
| BUD-01 | Create Budget | Set monthly budget for a category |
| BUD-02 | Edit Budget | Modify budget amount |
| BUD-03 | Delete Budget | Remove budget |
| BUD-04 | Auto-Create Next Month | Automatically create budget for next month |

### 2.2 Budget Properties
| Property | Type | Description |
|----------|------|-------------|
| id | Long | Unique identifier |
| categoryId | Long | Associated category |
| amount | Double | Budget limit |
| month | String | Month in "YYYY-MM" format |
| isSavingsTarget | Boolean | Mark as savings target |
| createdAt | Long | Creation timestamp |

### 2.3 Time Features
| ID | Feature | Description |
|----|---------|-------------|
| BUD-10 | Month Navigation | Navigate between months |
| BUD-11 | Current Month Display | Shows current month by default |
| BUD-12 | Month Selector | Select specific month |

### 2.4 Progress Tracking
| ID | Feature | Description |
|----|---------|-------------|
| BUD-20 | Per-Category Progress | Track spending vs budget per category |
| BUD-21 | Total Budget | Sum of all category budgets |
| BUD-22 | Total Spent | Sum of expenses in budgeted categories |
| BUD-23 | Budget Utilization | Percentage of budget used |
| BUD-24 | Under Control | Categories within budget |
| BUD-25 | Overrun | Categories exceeded |

---

## 3. Data Dependencies

### 3.1 Entities Used
| Entity | Repository | Purpose |
|--------|------------|---------|
| BudgetEntity | BudgetRepository | Budget data |
| CategoryEntity | CategoryRepository | Category for budget |
| TransactionEntity | TransactionDao | Spending calculation |

### 3.2 Repositories
| Repository | Methods Used |
|------------|--------------|
| BudgetRepository | getAllBudgets(), insertBudget(), updateBudget(), deleteBudget(), getBudgetsByPeriod() |
| CategoryRepository | getAllCategories() |
| TransactionDao | getAllTransactions() |

### 3.3 Preferences
| Key | Type | Purpose |
|-----|------|---------|
| currency | String | Display currency |

---

## 4. State Management

### 4.1 UiState Fields
```kotlin
data class BudgetsUiState(
    val budgetsWithSpending: List<BudgetWithSpending>,
    val categories: List<CategoryEntity>,
    val currencyCode: String,
    val isLoading: Boolean,
    val currentMonth: String,
    val selectedMonth: Calendar
)
```

### 4.2 Data Classes
```kotlin
data class BudgetWithSpending(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spent: Double
)
```

---

## 5. User Interactions

### 5.1 Actions
| Action | Method | Description |
|--------|--------|-------------|
| Add Budget | addBudget() | Create budget for category |
| Update Budget | updateBudget() | Modify budget amount |
| Delete Budget | deleteBudget() | Remove budget |
| Change Month | changeMonth(delta: Int) | Navigate months |

### 5.2 Parameters
| Parameter | Type | Description |
|-----------|------|-------------|
| categoryId | Long | Category to budget |
| amount | Double | Budget amount |
| autoCreateNextMonth | Boolean | Auto-create for next month |

---

## 6. Business Logic

### 6.1 Month Selection
```kotlin
fun changeMonth(delta: Int)
- delta = +1: next month
- delta = -1: previous month
- Updates _selectedMonth StateFlow
```

### 6.2 Budget Creation
```kotlin
fun addBudget(categoryId, amount, autoCreateNextMonth)
- Creates BudgetEntity with current month
- If autoCreateNextMonth: creates same budget for next month
- Validates no duplicate for same category/month
```

### 6.3 Spending Calculation
- Filters transactions by: current month, category matches, not split parent
- Sum of transaction amounts = spent amount
- Progress = spent / budget amount

---

## 7. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| Dashboard | Budget widget | Shows budget summary |
| Reports | Budget progress | Shows budget vs actual |
| Categories | Category picker | Select category for budget |
| Settings | Navigate to | Access budgets |

---

## 8. Edge Cases

| Scenario | Handling |
|----------|----------|
| Delete category with budget | Cascade delete (ForeignKey.CASCADE) |
| No budgets | Show empty state |
| Budget exceeded | Show overrun count, red indicator |
| Future month | Allow budget creation |
| Past month | Allow viewing |

---

## 9. Related Files

| File | Purpose |
|------|---------|
| `BudgetsScreen.kt` | UI composable |
| `BudgetsViewModel.kt` | Business logic |
| `BudgetEntity.kt` | Data entity |
| `BudgetRepository.kt` | Data access interface |
| `BudgetRepositoryImpl.kt` | Data access implementation |
| `BudgetDao.kt` | Database operations |
| `BudgetWidget.kt` | Dashboard widget |

---

## 10. Impact Analysis Reference

When modifying Budgets features, check impact on:
- Dashboard (budget summary, progress)
- Reports (budget progress comparison)
- Categories (category selection)
- Export (budget data)
- Firebase sync