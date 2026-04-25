# Reports Screen - Functional Requirements Document

**Screen:** Reports  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt`  
**ViewModel:** `ReportsViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Reports screen provides financial analytics including spending trends, category breakdown, budget progress, and lending summaries.

---

## 2. Features

### 2.1 Time Range Selection
| ID | Feature | Days | Data Points |
|----|---------|------|-------------|
| RPT-01 | Week | 7 | 7 (1 per day) |
| RPT-02 | Month | 30 | 8 (1 per week) |
| RPT-03 | Quarter | 90 | 6 (1 per 15 days) |
| RPT-04 | Year | 365 | 12 (1 per month) |

### 2.2 Financial Summaries
| ID | Feature | Description |
|----|---------|-------------|
| RPT-10 | Total Income | Sum of income transactions |
| RPT-11 | Total Expense | Sum of expense transactions |
| RPT-12 | Net Savings | Income minus expense |
| RPT-13 | Income Change | % change vs previous period |
| RPT-14 | Expense Change | % change vs previous period |

### 2.3 Visualization
| ID | Feature | Description |
|----|---------|-------------|
| RPT-20 | Trend Line Chart | Spending over time (income/expense/net) |
| RPT-21 | Category Breakdown | Pie chart of expenses by category |
| RPT-22 | Budget Progress | Budget vs actual per category |
| RPT-23 | Lending Summary | Outstanding borrow/lend |

### 2.4 Category Analysis
| ID | Feature | Description |
|----|---------|-------------|
| RPT-30 | Category Breakdown | Expenses grouped by category |
| RPT-31 | Category Colors | Predefined colors per category |
| RPT-32 | Category Totals | Amount per category |

### 2.5 Budget Analysis
| ID | Feature | Description |
|----|---------|-------------|
| RPT-40 | Budget Progress | Budgeted vs actual spending |
| RPT-41 | Per-Category Progress | Individual category progress |
| RPT-42 | Over/Under Budget | Categories status |

### 2.6 Lending Analysis
| ID | Feature | Description |
|----|---------|-------------|
| RPT-50 | Total Lent | Sum of money lent |
| RPT-51 | Total Borrowed | Sum of money borrowed |
| RPT-52 | Outstanding Balance | Net lending position |
| RPT-53 | Peer Details | Per-peer outstanding amounts |

---

## 3. Data Dependencies

### 3.1 Entities Used
| Entity | Repository | Purpose |
|--------|------------|---------|
| TransactionEntity | TransactionRepository | All transactions |
| BudgetEntity | BudgetRepository | Budget data |
| PeerContact | PeerContactRepository | Lending peers |

### 3.2 Repositories
| Repository | Methods Used |
|------------|--------------|
| TransactionRepository | getAllTransactions() |
| BudgetRepository | getAllBudgets() |
| PeerContactRepository | getAllPeers() |
| AccountRepository | getAllAccounts() |

### 3.3 Preferences
| Key | Type | Purpose |
|-----|------|---------|
| currency | String | Display currency |

---

## 4. State Management

### 4.1 UiState Fields
```kotlin
data class ReportsUiState(
    val selectedTimeRange: TimeRange,
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val previousIncome: Double,
    val previousExpense: Double,
    val incomeChange: Float,
    val expenseChange: Float,
    val trendData: List<TrendPoint>,
    val categoryBreakdown: List<PieChartEntry>,
    val budgetProgress: List<BudgetProgress>,
    val lendingSummary: List<LendingSummary>,
    val totalLent: Double,
    val totalBorrowed: Double,
    val totalOutstandingLending: Double,
    val currencyCode: String,
    val isLoading: Boolean
)
```

### 4.2 Data Classes
```kotlin
enum class TimeRange(val label: String, val days: Int)

data class TrendPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class BudgetProgress(
    val categoryName: String,
    val budgeted: Double,
    val actual: Double,
    val percentage: Float,
    val color: Color
)

data class LendingSummary(
    val peerId: Long,
    val name: String,
    val totalGiven: Double,
    val totalReceived: Double,
    val outstanding: Double
)
```

---

## 5. User Interactions

### 5.1 Actions
| Action | Method | Description |
|--------|--------|-------------|
| Change Time Range | setTimeRange() | Switch between Week/Month/Quarter/Year |

### 5.2 Time Range Logic
- Current period: last N days
- Previous period: N days before current period
- Change calculation: ((current - previous) / previous) * 100

---

## 6. Business Logic

### 6.1 Trend Data Generation
```
WEEK: 7 points, 1 day each
MONTH: 8 points, ~7 days each
QUARTER: 6 points, 15 days each
YEAR: 12 points, ~30 days each
```

### 6.2 Category Breakdown
- Filter transactions by type = "expense"
- Exclude split parents
- Group by categoryId
- Sum amounts per category

### 6.3 Budget Progress
- Filter budgets by current month
- Calculate actual from transactions in month
- Compare budget vs actual

### 6.4 Lending Summary
- Sum totalGiven from all peers
- Sum totalReceived from all peers
- outstanding = totalGiven - totalReceived

---

## 7. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| Dashboard | Reports link | Navigate to detailed reports |
| Transactions | Drill-down | View transactions for category |
| Budgets | Budget progress | Detailed budget view |
| Peers | Lending details | Peer outstanding amounts |

---

## 8. Edge Cases

| Scenario | Handling |
|----------|----------|
| No transactions | Show empty state, 0 values |
| No previous period data | Show 0% change |
| Category deleted | Show "Uncategorized" |
| Division by zero | Return 0% change |
| Negative change | Show negative percentage |

---

## 9. Related Files

| File | Purpose |
|------|---------|
| `ReportsScreen.kt` | UI composable |
| `ReportsViewModel.kt` | Business logic |
| `TrendLineChart.kt` | Trend chart component |
| `ExpensePieChart.kt` | Category breakdown |
| `PeerContactRepository.kt` | Lending data |

---

## 10. Impact Analysis Reference

When modifying Reports features, check impact on:
- Dashboard (summaries use same calculations)
- Export (reports data export)
- Firebase sync (analytics data)