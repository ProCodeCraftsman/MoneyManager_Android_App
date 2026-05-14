# Accounts Screen - Functional Requirements Document

**Screen:** Accounts  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/AccountsScreen.kt`  
**ViewModel:** `AccountsViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Accounts screen allows users to manage financial accounts including bank accounts, cash, credit cards, savings, and investments.

---

## 2. Features

### 2.1 Account Types
| ID | Feature | Type Value | Description |
|----|---------|------------|-------------|
| ACCT-01 | Bank Account | `bank` | Traditional bank accounts |
| ACCT-02 | Cash | `cash` | Physical cash on hand |
| ACCT-03 | Credit Card | `credit` | Credit card accounts |
| ACCT-04 | Savings | `savings` | Savings accounts |
| ACCT-05 | Investment | `investment` | Investment portfolios |
| ACCT-06 | Peer | `peer` | Peer-based accounts (for lending) |

### 2.2 CRUD Operations
| ID | Feature | Description |
|----|---------|-------------|
| ACCT-10 | Create Account | Add new account with name, type, initial balance, emoji, color |
| ACCT-11 | Edit Account | Modify account details |
| ACCT-12 | Delete Account | Remove account (cascades transactions) |
| ACCT-13 | View Accounts | List all accounts with current balances |

### 2.3 Account Properties
| Property | Type | Description |
|----------|------|-------------|
| name | String | Account name |
| type | String | Account type (bank/cash/credit/savings/investment/peer) |
| initialBalance | Double | Starting balance |
| balance | Double | Current calculated balance |
| currency | String | Currency code (default: INR) |
| emoji | String | Account icon |
| color | String | Hex color code |
| peerContactId | Long? | Link to peer if type is "peer" |
| createdAt | Long | Creation timestamp |
| updatedAt | Long | Last update timestamp |

### 2.4 Visualization
| ID | Feature | Description |
|----|---------|-------------|
| ACCT-20 | Account List | Sorted list of all accounts |
| ACCT-21 | Total Assets | Sum of all account balances |
| ACCT-22 | Account Comparison | Bar chart comparing accounts |
| ACCT-23 | Inflow/Outflow | Per-account transaction totals |

---

## 3. Data Dependencies

### 3.1 Entities Used
| Entity | Repository | Purpose |
|--------|------------|---------|
| AccountEntity | AccountRepository | Account data |
| TransactionEntity | TransactionRepository | Transaction totals per account |

### 3.2 Repositories
| Repository | Methods Used |
|------------|--------------|
| AccountRepository | getAllAccounts(), insertAccount(), updateAccount(), deleteAccount(), updateAccountBalance(), getTotalAssets() |
| TransactionRepository | getAllTransactions() |

### 3.3 Preferences
| Key | Type | Purpose |
|-----|------|---------|
| currency | String | Display currency |

---

## 4. State Management

### 4.1 UiState Fields
```kotlin
data class AccountsUiState(
    val accounts: List<AccountEntity>,
    val totalAssets: Double,
    val currencyCode: String,
    val isLoading: Boolean,
    val accountComparisonData: List<AccountBarData>
)
```

### 4.2 Data Classes
```kotlin
data class AccountBarData(
    val accountName: String,
    val inflow: Double,
    val outflow: Double
)
```

---

## 5. User Interactions

### 5.1 Actions
| Action | Method | Description |
|--------|--------|-------------|
| Add Account | addAccount() | Create new account |
| Update Account | updateAccount() | Modify existing account |

### 5.2 Events
| Event | Type | Description |
|-------|------|-------------|
| AccountEvent.Success | sealed class | Success message |
| AccountEvent.Error | sealed class | Error message |

---

## 6. Business Logic

### 6.1 Account Creation
```kotlin
fun addAccount(name: String, type: String, emoji: String, balance: Double)
- Creates AccountEntity with auto-generated id
- Inserts via accountRepository.insertAccount()
- Emits success/error event
```

### 6.2 Balance Calculation
- Total Assets = Sum of all account balances
- Inflow = Sum of income/receive/borrow transactions
- Outflow = Sum of expense/lend/repay transactions

---

## 7. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| Dashboard | Accounts summary | Shows total assets |
| Transactions | Account filter | Filter by account |
| Settings | Navigate to | Access accounts |
| Transfer | Source/Target | Select accounts for transfer |

---

## 8. Edge Cases

| Scenario | Handling |
|----------|----------|
| Delete account with transactions | Cascade delete transactions |
| Negative balance | Display warning indicator |
| Credit card type | Show negative balance as owed |
| No accounts | Show empty state, prompt to create |

---

## 9. Related Files

| File | Purpose |
|------|---------|
| `AccountsScreen.kt` | UI composable |
| `AccountsViewModel.kt` | Business logic |
| `AccountEntity.kt` | Data entity |
| `AccountRepository.kt` | Data access interface |
| `AccountRepositoryImpl.kt` | Data access implementation |
| `AccountDao.kt` | Database operations |

---

## 10. Impact Analysis Reference

When modifying Accounts features, check impact on:
- Dashboard (net worth, account summaries)
- Transactions (account filter, balance)
- Transfer (account selection)
- Reports (account-based analysis)
- Budget (per-account budget)
- Export (account data)
- Firebase sync