# Transactions Screen - Functional Requirements Document

**Screen:** Transactions  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt`  
**ViewModel:** `TransactionsViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Transactions screen displays and manages all financial transactions with filtering, search, and CRUD operations.

---

## 2. UI Components

| Component | File | Description |
|-----------|------|-------------|
| TransactionFilterSheet | `ui/components/TransactionFilterSheet.kt` | Filter bottom sheet |
| TransferDialog | `ui/components/TransferDialog.kt` | Transfer dialog |

---

## 3. Features

### 3.1 Transaction Types
| ID | Feature | Description |
|----|---------|-------------|
| TXN-01 | Income | Money received |
| TXN-02 | Expense | Money spent |
| TXN-03 | Savings | Money saved |
| TXN-04 | Transfer | Between accounts |
| TXN-05 | Borrow | Money borrowed from peer |
| TXN-06 | Lend | Money lent to peer |
| TXN-07 | Repay | Repayment of borrow |
| TXN-08 | Receive | Repayment of lend |

### 3.2 CRUD Operations
| ID | Feature | Description |
|----|---------|-------------|
| TXN-10 | Create Transaction | Add new transaction |
| TXN-11 | Edit Transaction | Modify existing |
| TXN-12 | Delete Transaction | Remove transaction |
| TXN-13 | View Details | Full transaction details |

### 3.3 Filtering
| ID | Feature | Description |
|----|---------|-------------|
| TXN-20 | Filter by Type | income/expense/savings/transfer |
| TXN-21 | Filter by Account | Specific account |
| TXN-22 | Filter by Category | Category or subcategory |
| TXN-23 | Filter by Tags | Tag-based filter |
| TXN-24 | Filter by Goal | Associated goal |
| TXN-25 | Filter by Peer | Borrow/lend peer |
| TXN-26 | Filter by Date | Start/end date range |
| TXN-27 | Clear Filters | Reset all filters |

### 3.4 Search
| ID | Feature | Description |
|----|---------|-------------|
| TXN-30 | Search by Note | Text search |
| TXN-31 | Search by Amount | Numeric search |

### 3.5 Special Features
| ID | Feature | Description |
|----|---------|-------------|
| TXN-40 | Split Transaction | Divide across categories |
| TXN-41 | Link to Goal | Associate with savings goal |
| TXN-42 | Attach Receipt | Store receipt image |
| TXN-43 | Transfer | Move between accounts |

---

## 4. Data Dependencies

### 4.1 Entities Used
| Entity | Repository | Purpose |
|--------|------------|---------|
| TransactionEntity | TransactionRepository | Transaction data |
| CategoryEntity | CategoryRepository | Categories for picker |
| AccountEntity | AccountRepository | Accounts for picker |
| GoalEntity | GoalRepository | Goals for linking |
| TagEntity | CategoryRepository | Tags for filtering |
| PeerContact | PeerContactRepository | Peers for borrow/lend |

### 4.2 Repositories
| Repository | Methods Used |
|------------|--------------|
| TransactionRepository | getAllTransactions(), insertTransaction(), updateTransaction(), deleteTransaction(), getSplitChildren() |
| CategoryRepository | getAllCategories(), getAllTags() |
| AccountRepository | getAllAccounts() |
| GoalRepository | getAllGoals() |
| PeerContactRepository | getAllPeers() |

---

## 5. State Management

### 5.1 UiState Fields
```kotlin
data class TransactionsUiState(
    val transactions: List<TransactionEntity>,
    val isLoading: Boolean,
    val searchQuery: String,
    val filterType: String,
    val filterAccountId: Long?,
    val filterCategoryId: Long?,
    val filterTagId: Long?,
    val filterGoalId: Long?,
    val filterPeerId: Long?,
    val filterStartDate: Long?,
    val filterEndDate: Long?,
    val allTags: List<TagEntity>,
    val allCategories: List<CategoryEntity>,
    val allAccounts: List<AccountEntity>,
    val allGoals: List<GoalEntity>,
    val allPeers: List<PeerContact>,
    val currency: String,
)
```

### 5.2 Internal StateFlows
| StateFlow | Type | Purpose |
|-----------|------|---------|
| _searchQuery | String | Search text |
| _filters | FilterState | All filter criteria |

---

## 6. User Interactions

### 6.1 Actions
| Action | Trigger | Result |
|--------|---------|--------|
| Add Transaction | Tap FAB | Opens transaction form |
| Edit Transaction | Tap transaction | Opens edit form |
| Delete Transaction | Swipe or long press | Deletes transaction |
| Search | Type in search bar | Filters list |
| Filter | Tap filter button | Opens filter sheet |
| Transfer | From dashboard | Opens transfer dialog |

### 6.2 Filter Methods
| Method | Purpose |
|--------|---------|
| setSearchQuery() | Update search text |
| setTypeFilter() | Set transaction type |
| setAccountFilter() | Filter by account |
| setCategoryFilter() | Filter by category |
| setTagFilter() | Filter by tag |
| setGoalFilter() | Filter by goal |
| setPeerFilter() | Filter by peer |
| setDateRangeFilter() | Filter by date range |
| clearAllFilters() | Reset all filters |

---

## 7. Business Logic

### 7.1 Transaction Creation
```kotlin
addTransaction(transaction: TransactionEntity)
- Insert transaction
- Adjust account balance (increase for income, decrease for expense)
- Update peer balance for borrow/lend transactions
```

### 7.2 Split Transaction
```kotlin
addSplitTransaction(parent: TransactionEntity, children: List<TransactionEntity>)
- Insert parent with isSplitParent=true
- Insert children with isSplitChild=true and parentTransactionId
- Adjust balance only once for parent total amount
```

### 7.3 Transfer
```kotlin
addTransfer(fromAccountId, toAccountId, amount, note, date)
- Create OUT transaction for source account
- Create IN transaction for destination account
- Update both account balances
```

### 7.4 Balance Adjustment
```kotlin
adjustBalance(transaction, reverse)
- Income/Receive/Borrow: increase balance
- Expense/Lend/Repay: decrease balance
- reverse=true: undo the adjustment
```

---

## 8. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| Dashboard | onNavigateToTransactions | Passes filter params |
| Categories | Category picker | Select category for transaction |
| Accounts | Account picker | Select account for transaction |
| Goals | Goal picker | Link transaction to goal |
| Tags | Tag selection | Apply tags to transaction |
| Peers | Peer picker | Select peer for borrow/lend |

---

## 9. Edge Cases

| Scenario | Handling |
|----------|----------|
| Delete category | Set categoryId to null, preserve transaction |
| Delete account | Cascade delete all transactions |
| Split parent deletion | Also delete split children |
| Receipt file not found | Skip file deletion, log warning |
| Zero amount | Validate and reject |
| Future date | Allow (for scheduled transactions) |

---

## 10. Related Files

| File | Purpose |
|------|---------|
| `TransactionsScreen.kt` | UI composable |
| `TransactionsViewModel.kt` | Business logic |
| `TransactionEntity.kt` | Data entity |
| `TransactionRepository.kt` | Data access |
| `TransactionDao.kt` | Database operations |
| `TransferDialog.kt` | Transfer UI |

---

## 11. Impact Analysis Reference

When modifying Transactions features, check impact on:
- Dashboard (recent transactions, summaries)
- Reports (category breakdown, trends)
- Budget calculations (expense tracking)
- Goals (linked transactions)
- Export functionality
- Firebase sync (all transaction data)