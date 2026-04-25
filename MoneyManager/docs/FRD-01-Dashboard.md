# Dashboard Screen - Functional Requirements Document

**Screen:** Dashboard  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/DashboardScreen.kt`  
**ViewModel:** `DashboardViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Dashboard is the main screen of the MoneyManager app, providing a comprehensive financial overview with summaries, charts, and quick access to recent transactions.

---

## 2. UI Components

| Component | File | Description |
|-----------|------|-------------|
| ExpensePieChart | `ui/components/ExpensePieChart.kt` | Doughnut chart for expense breakdown |
| CategoryDrilldownPanel | `ui/components/CategoryDrilldownPanel.kt` | Shows transactions for selected category |
| AccountComparisonChart | `ui/components/AccountComparisonChart.kt` | Bar chart comparing accounts |
| BudgetWidget | `ui/components/BudgetWidget.kt` | Budget progress display |
| RemindersWidget | `ui/components/RemindersWidget.kt` | Upcoming recurring reminders |
| TimeFilterBar | `ui/components/TimeFilterBar.kt` | Date range selector |
| TrendLineChart | `ui/components/TrendLineChart.kt` | Spending trends |

---

## 3. Features

### 3.1 Financial Summaries
| ID | Feature | Description |
|----|---------|-------------|
| DASH-01 | Net Worth | Sum of all account balances |
| DASH-02 | Period Income | Total income for selected period |
| DASH-03 | Period Expense | Total expenses for selected period |
| DASH-04 | Period Savings | Total savings for selected period |
| DASH-05 | Lending Summary | Total owed to user, total user owes |
| DASH-06 | Net Borrowing | Lending minus borrowing |

### 3.2 Time Filters
| ID | Feature | Description |
|----|---------|-------------|
| DASH-10 | Day Filter | Show today's data |
| DASH-11 | Week Filter | Current week data |
| DASH-12 | Month Filter | Current month data (default) |
| DASH-13 | Year Filter | Current year data |
| DASH-14 | All Time | All historical data |
| DASH-15 | Custom Range | User-selected date range |

### 3.3 Dashboard Types
| ID | Feature | Description |
|----|---------|-------------|
| DASH-20 | Overview | Combined view of all finances |
| DASH-21 | Expense Summary | Expense-focused view |
| DASH-22 | Income Summary | Income-focused view |
| DASH-23 | Accounts Summary | Account balances view |
| DASH-24 | Savings Summary | Savings goals view |
| DASH-25 | Budget Summary | Budget progress view |
| DASH-26 | Lending Summary | Borrow/lend overview |

### 3.4 Charts & Visualization
| ID | Feature | Description |
|----|---------|-------------|
| DASH-30 | Expense Pie Chart | Category breakdown |
| DASH-31 | Income Pie Chart | Income sources |
| DASH-32 | Savings Breakdown | Savings destinations |
| DASH-33 | Trend Line Chart | Spending over time |
| DASH-34 | Account Comparison | Account comparison bar chart |
| DASH-35 | Chart Drill-down | Tap segment to view transactions |

### 3.5 Budget Features
| ID | Feature | Description |
|----|---------|-------------|
| DASH-40 | Budget Summary | Total budget vs spent |
| DASH-41 | Budget Progress | Per-category progress bars |
| DASH-42 | Under Control Count | Categories within budget |
| DASH-43 | Overrun Count | Categories over budget |

### 3.6 Recurring Features
| ID | Feature | Description |
|----|---------|-------------|
| DASH-50 | Upcoming Reminders | Next 5 recurring transactions |
| DASH-51 | Reminder Widget | Display in dashboard |

---

## 4. Data Dependencies

### 4.1 Entities Used
| Entity | Repository | Purpose |
|--------|------------|---------|
| TransactionEntity | TransactionRepository | All transaction data |
| AccountEntity | AccountRepository | Account balances |
| BudgetEntity | BudgetRepository | Budget limits |
| RecurringEntity | RecurringRepository | Recurring schedules |
| CategoryEntity | CategoryRepository | Category names/colors |
| GoalEntity | GoalRepository | Savings goals |
| PeerContact | PeerContactRepository | Borrow/lend peers |

### 4.2 Repositories
| Repository | Injected In | Methods Used |
|------------|-------------|--------------|
| TransactionRepository | ViewModel | getAllTransactions(), getTransactionsByGoal() |
| AccountRepository | ViewModel | getAllAccounts(), getTotalAssets() |
| BudgetRepository | ViewModel | getAllBudgets() |
| RecurringRepository | ViewModel | getAllRecurring() |
| CategoryRepository | ViewModel | getAllCategories() |
| GoalRepository | ViewModel | getAllGoals() |
| PeerContactRepository | ViewModel | getAllPeers() |

### 4.3 Preferences
| Key | Type | Purpose |
|-----|------|---------|
| currency | String | Display currency (default: INR) |

---

## 5. State Management

### 5.1 UiState Fields
```kotlin
data class DashboardUiState(
    val netWorth: Double,
    val periodBalance: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val totalSavings: Double,
    val totalLending: Double,
    val totalBorrowing: Double,
    val incomeSummary: PeriodSummary,
    val expenseSummary: PeriodSummary,
    val savingsSummary: PeriodSummary,
    val lendingSummary: PeriodSummary,
    val savingsBreakdown: List<PieChartEntry>,
    val savingsDestinations: List<SavingsDestination>,
    val recentTransactions: List<TransactionEntity>,
    val accounts: List<AccountEntity>,
    val expenseBreakdown: List<PieChartEntry>,
    val incomeBreakdown: List<PieChartEntry>,
    val dashboardType: DashboardType,
    val accountSummaries: List<AccountPeriodSummary>,
    val budgetSummary: BudgetSummary,
    val lendingDashboardSummary: LendingDashboardSummary,
    val isLoading: Boolean,
    val selectedFilter: TimeFilter,
    val customStartDate: Long?,
    val customEndDate: Long?,
    val budgetsWithProgress: List<BudgetWithProgress>,
    val upcomingRecurring: List<RecurringEntity>,
    val currency: String,
)
```

### 5.2 Internal StateFlows
| StateFlow | Type | Purpose |
|-----------|------|---------|
| selectedFilter | TimeFilter | Current date filter |
| dashboardType | DashboardType | Current view type |
| customStartDate | Long? | Custom range start |
| customEndDate | Long? | Custom range end |
| selectedCategoryIds | Set<Long> | Category filter |
| selectedAccountIds | Set<Long> | Account filter |

---

## 6. User Interactions

### 6.1 Navigation Callbacks
| Callback | Parameters | Navigates To |
|----------|------------|--------------|
| onNavigateToAccounts | - | Accounts Screen |
| onNavigateToTransactions | type, accountId, startDate, endDate, goalId, categoryId, peerId | Transactions Screen |
| onNavigateToBorrowLend | - | Borrow/Lend Screen |

### 6.2 Actions
| Action | Trigger | Result |
|--------|---------|--------|
| Add Transaction | Tap FAB | Opens transaction form |
| View Transfer | Tap transfer button | Opens transfer dialog |
| Select Category | Tap pie chart segment | Shows category transactions |
| Change Filter | Tap time filter | Updates data range |
| Change Dashboard Type | Dropdown selection | Changes view |

---

## 7. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| Accounts | onNavigateToAccounts | Navigate to manage accounts |
| Transactions | onNavigateToTransactions | View transaction list |
| Transfer | FAB action | Transfer money between accounts |
| Borrow/Lend | onNavigateToBorrowLend | Track lending |

---

## 8. Edge Cases

| Scenario | Handling |
|----------|----------|
| No accounts | Show empty state, prompt to create |
| No transactions | Show empty state message |
| Budget exceeded | Show warning color (red) |
| Custom date invalid | Fall back to month filter |
| Negative net worth | Display in red |

---

## 9. Related Files

| File | Purpose |
|------|---------|
| `DashboardScreen.kt` | UI composable |
| `DashboardViewModel.kt` | Business logic |
| `MoneyManagerNavHost.kt` | Navigation setup |
| `ExpensePieChart.kt` | Chart component |
| `BudgetWidget.kt` | Budget display |
| `TransferDialog.kt` | Transfer dialog |

---

## 10. Impact Analysis Reference

When modifying Dashboard features, check impact on:
- TransactionRepository (data source)
- AccountRepository (balances)
- BudgetRepository (budget data)
- RecurringRepository (reminders)
- Navigation flow
- Export functionality (includes dashboard data)