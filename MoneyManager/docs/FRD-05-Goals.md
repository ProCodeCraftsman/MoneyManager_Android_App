# Goals Screen - Functional Requirements Document

**Screen:** Goals  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt`  
**ViewModel:** `GoalsViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Goals screen allows users to create and track savings goals with target amounts and deadlines.

---

## 2. Features

### 2.1 Goal Operations
| ID | Feature | Description |
|----|---------|-------------|
| GOAL-01 | Create Goal | Add savings goal with name, target, deadline |
| GOAL-02 | Edit Goal | Modify goal details |
| GOAL-03 | Delete Goal | Remove goal |
| GOAL-04 | Add Contribution | Add funds to goal |
| GOAL-05 | Mark Complete | Mark goal as achieved |
| GOAL-06 | View Goals | List all goals with progress |

### 2.2 Goal Properties
| Property | Type | Description |
|----------|------|-------------|
| id | Long | Unique identifier |
| name | String | Goal name |
| emoji | String | Goal icon |
| targetAmount | Double | Target amount to save |
| currentAmount | Double | Manual contributions |
| deadline | Long? | Target completion date |
| isCompleted | Boolean | Completion status |
| createdAt | Long | Creation timestamp |

### 2.3 Progress Tracking
| ID | Feature | Description |
|----|---------|-------------|
| GOAL-10 | Progress Percentage | (manualAmount + linkedAmount) / targetAmount |
| GOAL-11 | Manual Amount | Direct contributions to goal |
| GOAL-12 | Linked Amount | Transactions linked to goal |
| GOAL-13 | Total Amount | Sum of manual and linked |
| GOAL-14 | Deadline Tracking | Days remaining to deadline |
| GOAL-15 | Completion Status | Is goal achieved |

---

## 3. Data Dependencies

### 3.1 Entities Used
| Entity | Repository | Purpose |
|--------|------------|---------|
| GoalEntity | GoalRepository | Goal data |
| TransactionEntity | TransactionRepository | Linked transactions |

### 3.2 Repositories
| Repository | Methods Used |
|------------|--------------|
| GoalRepository | getAllGoals(), insertGoal(), updateGoal(), deleteGoal(), getGoalById() |
| TransactionRepository | getTransactionsByGoal() |

### 3.3 Preferences
| Key | Type | Purpose |
|-----|------|---------|
| currency | String | Display currency |

---

## 4. State Management

### 4.1 UiState Fields
```kotlin
data class GoalsUiState(
    val goals: List<GoalWithProgress>,
    val currencyCode: String,
    val isLoading: Boolean
)
```

### 4.2 Data Classes
```kotlin
data class GoalWithProgress(
    val goal: GoalEntity,
    val manualAmount: Double,
    val linkedAmount: Double,
    val totalAmount: Double
)
```

---

## 5. User Interactions

### 5.1 Actions
| Action | Method | Parameters |
|--------|--------|------------|
| Add Goal | addGoal() | name, emoji, targetAmount, deadline |
| Add Contribution | addContribution() | goalId, amount |
| Update Goal | updateGoal() | GoalEntity |
| Delete Goal | deleteGoal() | goalId |

---

## 6. Business Logic

### 6.1 Goal Creation
```kotlin
fun addGoal(name: String, emoji: String, targetAmount: Double, deadline: Long?)
- Creates GoalEntity
- Inserts via goalRepository.insertGoal()
- currentAmount defaults to 0.0
- isCompleted defaults to false
```

### 6.2 Contribution
```kotlin
fun addContribution(goalId: Long, amount: Double)
- Fetches goal by ID
- Adds amount to currentAmount
- Updates goal in repository
- Marks as completed if currentAmount >= targetAmount
```

### 6.3 Progress Calculation
- manualAmount: goal.currentAmount (direct additions)
- linkedAmount: sum of transactions where goalId matches
- totalAmount: manualAmount + linkedAmount
- Percentage: (totalAmount / targetAmount) * 100

---

## 7. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| Dashboard | Savings summary | Shows goal progress |
| Transactions | Goal link | Link transactions to goal |
| Recurring | Goal link | Link recurring to goal |
| Settings | Navigate to | Access goals |

---

## 8. Edge Cases

| Scenario | Handling |
|----------|----------|
| Goal achieved | Mark isCompleted = true |
| Contribution exceeds target | Allow, show 100%+ |
| Delete goal with transactions | Keep transactions, unlink goal |
| No deadline | Show "No deadline" |
| Past deadline | Show "Overdue" indicator |

---

## 9. Related Files

| File | Purpose |
|------|---------|
| `GoalsScreen.kt` | UI composable |
| `GoalsViewModel.kt` | Business logic |
| `GoalEntity.kt` | Data entity |
| `GoalRepository.kt` | Data access interface |
| `GoalRepositoryImpl.kt` | Data access implementation |
| `GoalDao.kt` | Database operations |

---

## 10. Impact Analysis Reference

When modifying Goals features, check impact on:
- Dashboard (savings summary, goal progress)
- Transactions (goal linking)
- Recurring (goal linking)
- Reports (savings analysis)
- Export (goal data)
- Firebase sync