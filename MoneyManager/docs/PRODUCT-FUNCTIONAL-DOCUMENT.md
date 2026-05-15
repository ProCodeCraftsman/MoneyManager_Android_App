# MoneyManager — Product & Functional Document

**Version:** 2.2 (Insights Dashboard milestone)  
**Platform:** Android (Native)  
**Last Updated:** April 2026  
**Intended Use:** User training material creation, workflow understanding, implementation reference, limitation awareness

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Technical Stack](#2-technical-stack)
3. [Architecture](#3-architecture)
4. [Navigation & Screen Map](#4-navigation--screen-map)
5. [Feature Inventory](#5-feature-inventory)
6. [Entity & Data Model](#6-entity--data-model)
7. [Workflows & User Journeys](#7-workflows--user-journeys)
8. [Reusable UI Components](#8-reusable-ui-components)
9. [Security & Privacy](#9-security--privacy)
10. [Data Import/Export](#10-data-importexport)
11. [Cloud Sync (Firebase)](#11-cloud-sync-firebase)
12. [Theme System](#12-theme-system)
13. [Background Work](#13-background-work)
14. [Limitations & Known Issues](#14-limitations--known-issues)
15. [Edge Cases & Error Handling Reference](#15-edge-cases--error-handling-reference)
16. [Design Conventions for Training Material](#16-design-conventions-for-training-material)

---

## 1. Product Overview

### 1.1 What It Is

MoneyManager is a native Android personal finance manager that lets users track income, expenses, savings, transfers, and borrowing/lending — all offline-first with optional cloud backup. Originally converted from an HTML/JavaScript web app to a modern Jetpack Compose + Material Design 3 Android app.

### 1.2 Core Value Proposition

- **Local-first:** All data stored on-device via Room database (SQLite). No account required for core functionality.
- **8 transaction types:** Covers comprehensive personal finance scenarios including borrowing/lending between peers.
- **Insights-driven dashboard:** Summary screen with 5 tabs (Expense, Income, Lending, Transfers, Savings) and time-filtered analytics.
- **Recurring transactions:** Auto-generate transactions on schedules.
- **Budgeting & Goals:** Monthly category budgets + savings goals with progress tracking.
- **Full data portability:** CSV/JSON import and export for backup and analysis.
- **Security:** PIN lock + biometric authentication.

### 1.3 Target Users

- Individuals tracking personal finances
- Users needing borrow/lend tracking with peers
- Users wanting offline-first financial management
- Privacy-conscious users wanting local data control

---

## 2. Technical Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Kotlin (Jetpack Compose) | — |
| Min SDK | 26 (Android 8.0) | — |
| Target SDK | 36 (Android 14+) | — |
| Architecture | MVVM + Clean Architecture (3-layer) | — |
| UI | Jetpack Compose + Material Design 3 | BOM 2024.12.01 |
| DI | Dagger Hilt | 2.59.2 |
| Database | Room (SQLite) | 2.8.4 |
| Navigation | Navigation Compose | 2.9.7 |
| Paging | Paging 3 Compose | 3.3.0 |
| Cloud Sync | Firebase Auth + Firestore | BOM 34.12.0 |
| Charts | MPAndroidChart | 3.1.0 |
| Biometric | AndroidX Biometric | 1.1.0 |
| Widgets | Glance AppWidget | 1.1.1 |
| Background | WorkManager + Hilt | 2.11.2 |
| Preferences | DataStore Preferences | 1.1.1 |
| Image Loading | Coil Compose | 2.7.0 |

### Dependency Injection: Hilt Modules

| Module | Scope | Provides |
|--------|-------|----------|
| `DatabaseModule` | Singleton | Room database, all 9 DAOs |
| `RepositoryModule` | Singleton | All 8 repository implementations |
| `FirebaseModule` | Singleton | FirebaseAuth, FirebaseFirestore |
| `PreferencesModule` | Singleton | PreferencesManager |

---

## 3. Architecture

### 3.1 Clean Architecture (3-Layer)

```
┌──────────────────────────────────────────────────┐
│  UI LAYER (com.moneymanager.app.ui)              │
│  ├─ Screens (Composables + ViewModels)           │
│  ├─ Components (Reusable Composables)            │
│  ├─ Dialogs, Summary, Insights, Theme, Auth      │
│  └─ Widget (QuickAddWidget)                      │
├──────────────────────────────────────────────────┤
│  DOMAIN LAYER (com.moneymanager.domain)          │
│  └─ Repository interfaces (8 contracts)          │
├──────────────────────────────────────────────────┤
│  DATA LAYER (com.moneymanager.data)              │
│  ├─ Entities (9 Room @Entity)                    │
│  ├─ DAOs (9 interfaces)                          │
│  ├─ Repository implementations                   │
│  ├─ Security (Biometric, PIN/PBKDF2)             │
│  ├─ Sync (Firebase Auth + Firestore)             │
│  ├─ Worker (RecurringGenerationWorker)           │
│  └─ Seed (CategorySeeder)                        │
└──────────────────────────────────────────────────┘
```

### 3.2 Data Flow Pattern

```
User Action → Composable → ViewModel (StateFlow)
                                    │
                                    ▼
                        Repository Interface
                                    │
                                    ▼
                        Repository Impl → Room DAO → SQLite
                                    │
                        (Optional)  ▼
                        FirebaseSyncManager → Firestore
```

### 3.3 Concurrency & State

- **ViewModels** use `viewModelScope` with `StateFlow` for reactive UI
- **Flows** use `flatMapLatest` for pipeline composition
- **State exposure:** Single `SummaryUiState` data class consumed via `collectAsStateWithLifecycle()`
- **Summary Filter pipeline:** `filterState → getDateRangeForFilter → getTransactionsByDateRange → SummaryAggregator → SummaryUiState`
- **Paging:** Transactions screen uses Paging 3 for large lists

---

## 4. Navigation & Screen Map

### 4.1 Bottom Navigation (3 tabs)

```
┌─────────────┬──────────────┬──────────────┐
│  Summary    │ Transactions │  Settings     │
│ (Dashboard) │  (P2P List)  │  (Profile)    │
├─────────────┼──────────────┼──────────────┤
│ SpaceDashboard Icon│ ReceiptLong Icon    │ Settings Icon │
└─────────────┴──────────────┴──────────────┘
```

### 4.2 Screen Hierarchy

```
MoneyManagerNavHost
├── Summary (Dashboard) — main landing
│   ├── Expense tab (pie + top categories)
│   ├── Income tab (sources + breakdown)
│   ├── Lending tab (overview + people list)
│   ├── Transfers tab (summary + account-wise)
│   └── Savings tab (goals + accounts)
├── Transactions — paginated list with search/filter
├── Settings
│   ├── Accounts (sub-screen)
│   ├── Categories (sub-screen)
│   ├── Tags (sub-screen)
│   ├── Peers (sub-screen)
│   ├── Budgets (sub-screen)
│   ├── Goals (sub-screen)
│   ├── Recurring (sub-screen)
│   │   └── RecurringForm (add/edit recurring)
│   ├── Templates (sub-screen)
│   ├── Transfer (quick transfer)
│   └── Insights
│       ├── Status pane
│       ├── Risks pane [INCOMPLETE — stub]
│       └── Trends pane [INCOMPLETE — stub]
├── AddTransaction (deep-linkable)
└── AppLockScreen (conditional overlay)
```

### 4.3 Deep Links

| URI Pattern | Screen |
|-------------|--------|
| `moneymanager://transactions?...` | Transactions with filters |
| `moneymanager://transfer` | Transfer screen |
| `moneymanager://add_transaction?type={type}` | Add transaction (pre-filled type) |

### 4.4 Navigation Parameters (Transactions)

The Transactions route accepts optional filter parameters passed from other screens:

`transactions?type={type}&accountId={id}&startDate={ms}&endDate={ms}&goalId={id}&categoryId={id}&peerId={id}`

---

## 5. Feature Inventory

### 5.1 Transactions (8 types, CRUD, Filter, Search)

| Feature | ID | Status | Notes |
|---------|-----|--------|-------|
| Income | TXN-01 | ✅ | Money received |
| Expense | TXN-02 | ✅ | Money spent |
| Savings | TXN-03 | ✅ | Money saved |
| Transfer | TXN-04 | ✅ | Between accounts |
| Borrow | TXN-05 | ✅ | Money borrowed from peer |
| Lend | TXN-06 | ✅ | Money lent to peer |
| Repay | TXN-07 | ✅ | Repayment of borrow |
| Receive | TXN-08 | ✅ | Repayment of lend |
| Create Transaction | TXN-10 | ✅ | Via FAB or AddTransactionScreen |
| Edit Transaction | TXN-11 | ✅ | Tap existing transaction |
| Delete Transaction | TXN-12 | ✅ | With balance reversal |
| View Details | TXN-13 | ✅ | Bottom sheet detail |
| Filter by Type | TXN-20 | ✅ | Dropdown selector |
| Filter by Account | TXN-21 | ✅ | Account picker |
| Filter by Category | TXN-22 | ✅ | Category picker |
| Filter by Tags | TXN-23 | ✅ | Tag selector |
| Filter by Goal | TXN-24 | ✅ | Goal picker |
| Filter by Peer | TXN-25 | ✅ | Peer selector |
| Filter by Date | TXN-26 | ✅ | Date range picker |
| Clear Filters | TXN-27 | ✅ | Reset all |
| Search by Note | TXN-30 | ✅ | Text search |
| Search by Amount | TXN-31 | ✅ | Numeric search |
| Split Transaction | TXN-40 | ✅ | Parent-child hierarchy |
| Link to Goal | TXN-41 | ✅ | goalId on transaction |
| Attach Receipt | TXN-42 | ✅ | Base64 receipt path |
| Transfer | TXN-43 | ✅ | Two-transaction pair |

### 5.2 Accounts (6 types)

| Feature | ID | Status | Notes |
|---------|-----|--------|-------|
| Bank Account | ACCT-01 | ✅ | bank type |
| Cash | ACCT-02 | ✅ | cash type |
| Credit Card | ACCT-03 | ✅ | credit type |
| Savings | ACCT-04 | ✅ | savings type |
| Investment | ACCT-05 | ✅ | Not in VALID_TYPES enum |
| Peer | ACCT-06 | ✅ | peer type |
| Create Account | ACCT-10 | ✅ | Name, type, balance, emoji, color |
| Edit Account | ACCT-11 | ✅ | |
| Delete Account | ACCT-12 | ✅ | Cascade deletes transactions |
| View Accounts | ACCT-13 | ✅ | List with balances |
| Total Assets | ACCT-21 | ✅ | Sum of all balances |
| Account Comparison | ACCT-22 | ✅ | Bar chart |
| Inflow/Outflow | ACCT-23 | ✅ | Per-account |

### 5.3 Budgets (Monthly per-category)

| Feature | ID | Status | Notes |
|---------|-----|--------|-------|
| Create Budget | BUD-01 | ✅ | |
| Edit Budget | BUD-02 | ✅ | |
| Delete Budget | BUD-03 | ✅ | |
| Auto-Create Next Month | BUD-04 | ✅ | Optional on creation |
| Month Navigation | BUD-10 | ✅ | Forward/backward |
| Current Month Display | BUD-11 | ✅ | Default view |
| Month Selector | BUD-12 | ✅ | Select specific month |
| Per-Category Progress | BUD-20 | ✅ | Bar or progress indicator |
| Total Budget Sum | BUD-21 | ✅ | |
| Total Spent | BUD-22 | ✅ | |
| Budget Utilization | BUD-23 | ✅ | % used |
| Under/Over Budget | BUD-24/25 | ✅ | |

### 5.4 Goals (Savings targets)

| Feature | ID | Status | Notes |
|---------|-----|--------|-------|
| Create Goal | GOAL-01 | ✅ | Name, emoji, target, deadline |
| Edit Goal | GOAL-02 | ✅ | |
| Delete Goal | GOAL-03 | ✅ | |
| Add Contribution | GOAL-04 | ✅ | Manual amount addition |
| Mark Complete | GOAL-05 | ✅ | Auto when target reached |
| View Goals | GOAL-06 | ✅ | |
| Progress % | GOAL-10 | ✅ | (manual + linked) / target |
| Manual Amount | GOAL-11 | ✅ | Direct contributions |
| Linked Amount | GOAL-12 | ✅ | Transactions with goalId |
| Total Amount | GOAL-13 | ✅ | manual + linked |
| Deadline Tracking | GOAL-14 | ✅ | Days remaining |
| Completion Status | GOAL-15 | ✅ | |

### 5.5 Reports (Legacy — being replaced by Insights/Summary)

| Feature | ID | Status | Notes |
|---------|-----|--------|-------|
| Week/Month/Quarter/Year | RPT 1-4 | ⚠️ | Being replaced by Summary screen |
| Trend Line Chart | RPT-20 | ⚠️ | |
| Category Pie Chart | RPT-21 | ⚠️ | |
| Budget Progress | RPT-22 | ⚠️ | |
| Lending Summary | RPT-23 | ⚠️ | |

**Note:** The Reports screen from FRD-06 has been largely superseded by the Summary screen. The Insights screen (STATUS/RISKS/TRENDS) is the new direction.

### 5.6 Settings

| Feature | ID | Status | Notes |
|---------|-----|--------|-------|
| Dark Mode | SET-01 | ✅ | Toggle |
| Currency | SET-02 | ✅ | 10 currencies |
| PIN Lock | SET-10 | ✅ | 4-digit PIN |
| PIN Setup | SET-11 | ✅ | Create/change |
| Biometric Auth | SET-12 | ✅ | Fingerprint/face |
| Auto Lock Timer | SET-13 | ✅ | 1/5/15/30 min |
| Lock on Background | SET-14 | ✅ | Via AppLockManager |
| Google Sign-In | SET-20 | ✅ | Firebase Auth |
| Sync Status | SET-21 | ✅ | Idle/Syncing/Success/Error |
| Last Sync Time | SET-22 | ✅ | |
| Manual Sync | SET-23 | ✅ | Button trigger |
| Sign Out | SET-24 | ✅ | |
| Export CSV | SET-30 | ✅ | Per-type or ALL |
| Import CSV | SET-31 | ✅ | With validation |
| Export JSON | SET-32 | ✅ | Full backup |
| Import JSON | SET-33 | ✅ | Full restore |
| Clear Data | SET-34 | ✅ | Confirm dialog |
| Storage Usage | SET-35 | ✅ | DB file size |
| Account navigation | SET-40 | ✅ | |
| Category navigation | SET-41 | ✅ | |
| Tags | SET-42 | ✅ | |
| Peers | SET-43 | ✅ | |
| Budgets | SET-44 | ✅ | |
| Goals | SET-45 | ✅ | |
| Recurring | SET-46 | ✅ | |
| Templates | SET-47 | ✅ | |

### 5.7 Summary Dashboard (v2.2 flagship)

| Feature | Status | Notes |
|---------|--------|-------|
| Time Filter (Day/Week/Month/Year/All/Quarter/etc.) | ✅ | 12 time filters |
| Period Navigation | ✅ | Forward/back arrows |
| Custom Date Range | ✅ | From DateRangePickerSheet |
| Net Balance Hero Card | ✅ | income - expense + trend % |
| Expense Tab (Pie + Breakdown) | ✅ | Category & account breakdown |
| Income Tab (Sources + Breakdown) | ✅ | Category & account breakdown |
| Lending Tab (Overview + People) | ✅ | Net position, settled count, per-person |
| Transfers Tab | ✅ | Count, total amount, account-wise |
| Savings Tab (Goals + Accounts) | ✅ | Growth %, linked amounts |
| Empty State | ✅ | "No transactions yet" + CTA |
| Pagination Dots | ✅ | For 5 tabs in HorizontalPager |

### 5.8 Insights Screen (Milestone v2.2 — In Progress)

| Feature | Status | Notes |
|---------|--------|-------|
| Status Pane | ✅ | Hero number, 4 figure cards (current month) |
| Risks Pane | ❌ | Placeholder: "coming in Phase 30" |
| Trends Pane | ❌ | Placeholder: "coming in Phase 31" |

### 5.9 Additional Features

| Feature | Status | Notes |
|---------|--------|-------|
| Recurring Transactions | ✅ | daily/weekly/biweekly/monthly/yearly |
| Transaction Templates | ✅ | Pre-filled forms |
| Tags (CRUD + assignment) | ✅ | Color-coded labels |
| Categories (with sub-categories) | ✅ | parentId field |
| Category Seeder | ✅ | Default categories on first launch |
| Peer Contacts | ✅ | Borrow/lend people management |
| Quick Add Widget | ✅ | Home screen widget (income/expense/transfer) |
| Split Transactions | ✅ | Parent-child hierarchy |
| Scroll-to-Top FAB | ✅ | Global utility |
| Receipt Attachments | ✅ | Base64 data URL storage |

---

## 6. Entity & Data Model

### 6.1 Entity Relationship Diagram (Logical)

```
AccountEntity ──┐
                ├── TransactionEntity ──┐
CategoryEntity ─┘                      │
                                        ├── BudgetEntity
TagEntity (standalone)                  │
                                        ├── GoalEntity
PeerContact ────────────────────────────┤
                                        ├── RecurringEntity
                                        └── TemplateEntity
```

### 6.2 Entity Details

#### AccountEntity (`accounts`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | |
| type | String | `bank`, `cash`, `credit`, `savings`, `peer` |
| initialBalance | Double | Starting balance |
| balance | Double | Current calculated balance |
| currency | String | Default: `"INR"` |
| emoji | String | Account icon |
| iconType | String | `"emoji"`, `"material"`, `"image"` |
| color | String | Hex color (default: `"#2a6049"`) |
| peerContactId | Long? | For `peer` type accounts |
| createdAt | Long | Timestamp |
| updatedAt | Long | Timestamp |

#### TransactionEntity (`transactions`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| accountId | Long (FK → Account) | CASCADE on delete |
| type | String | `income`, `expense`, `savings`, `transfer`, `lend`, `receive`, `borrow`, `repay` |
| amount | Double | Always positive |
| categoryId | Long? (FK → Category) | SET_NULL on delete |
| subCategoryId | Long? | |
| goalId | Long? | Linked savings goal |
| peerContactId | Long? | For lend/receive/borrow/repay |
| tagIds | String | Comma-separated IDs |
| date | Long | Transaction date timestamp |
| description | String | |
| note | String | Free-text |
| receiptPath | String? | Base64 data URL |
| isRecurring | Boolean | |
| recurringId | Long? | Link to RecurringEntity |
| isSplitParent | Boolean | |
| isSplitChild | Boolean | |
| parentTransactionId | Long? (FK → Transaction) | CASCADE on delete |
| isTransfer | Boolean | |
| toAccountId | Long? | For transfers |
| investmentPlatform | String? | |
| expectedReturnDate | Long? | For investments |
| createdAt | Long | Timestamp |

**Indices:** accountId, categoryId, subCategoryId, date, note, tagIds, parentTransactionId

#### CategoryEntity (`categories`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | |
| emoji | String | Default: `"📁"` |
| iconType | String | `"emoji"`, `"material"`, `"image"` |
| color | String | Hex color (default: `"#90A4AE"`) |
| type | String | `expense`, `income`, `savings` |
| parentId | Long? | For sub-categories |
| isCustom | Boolean | User-created vs seeded |
| isArchived | Boolean | Hidden from pickers |
| createdAt | Long | Timestamp |

#### BudgetEntity (`budgets`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| categoryId | Long (FK → Category) | CASCADE on delete |
| amount | Double | Budget limit |
| month | String | `"YYYY-MM"` format |
| isSavingsTarget | Boolean | |
| createdAt | Long | Timestamp |

**Indices:** categoryId, month

#### GoalEntity (`goals`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | |
| emoji | String | Default: `"🎯"` |
| iconType | String | |
| targetAmount | Double | |
| currentAmount | Double | Manual contributions |
| deadline | Long? | Target date |
| isCompleted | Boolean | |
| createdAt | Long | Timestamp |

#### RecurringEntity (`recurring`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| accountId | Long (FK → Account) | CASCADE on delete |
| type | String | `income`, `expense`, `savings` |
| amount | Double | |
| categoryId | Long? (FK → Category) | SET_NULL on delete |
| subCategoryId | Long? | |
| goalId | Long? | |
| note | String | |
| frequency | String | `daily`, `weekly`, `biweekly`, `monthly`, `yearly` |
| startDate | Long | |
| nextDate | Long | Next generation date |
| isActive | Boolean | |
| reminderEnabled | Boolean | |
| reminderDays | Int | Days before to remind |
| investmentApp | String? | |
| createdAt | Long | Timestamp |

#### TemplateEntity (`templates`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | |
| type | String | `income`, `expense`, `savings` |
| amount | Double | Pre-filled amount |
| accountId | Long? | Pre-filled account |
| categoryId | Long? (FK → Category) | SET_NULL on delete |
| subCategoryId | Long? | |
| note | String | Pre-filled note |
| createdAt | Long | Timestamp |

#### TagEntity (`tags`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| name | String | |
| color | String | Hex color (default: `"#c8420a"`) |

#### PeerContact (`peer_contacts`)
| Field | Type | Notes |
|-------|------|-------|
| id | Long (PK) | Auto-generated |
| displayName | String | |
| phoneNumber | String | |
| email | String | |
| description | String | |
| photoUri | String? | |
| totalGiven | Double | Cumulative lent |
| totalReceived | Double | Cumulative received |
| createdAt | Long | Timestamp |
| updatedAt | Long | Timestamp |
| *(computed)* outstandingBalance | Double | totalGiven - totalReceived |

### 6.3 Database Migrations

| Migration | From → To | Changes |
|-----------|-----------|---------|
| MIGRATION_2_3 | 2 → 3 | Split transaction fields, transfer fields, investment platform |
| MIGRATION_5_6 | 5 → 6 | Peer contacts table, peerContactId on transactions & accounts |
| MIGRATION_6_7 | 6 → 7 | iconType field on categories, accounts, goals |

---

## 7. Workflows & User Journeys

### 7.1 Core Loop: Record an Expense

```
1. User opens app → lands on Summary screen
2. Taps FAB → AddTransactionScreen (or taps Transactions → FAB)
3. Selects type = "Expense"
4. Picks account, category, enters amount, optional description/note/tags
5. Taps Save
   → ViewModel.insertTransaction()
   → TransactionRepositoryImpl.insertTransaction()
   → TransactionDao.insert() + AccountDao.updateBalance() (decrease)
   → UI refreshes via StateFlow
6. User sees new transaction in list, balance updated on Accounts
7. (Optional) If category has a budget → budget utilization recalculated
```

### 7.2 Transfer Between Accounts

```
1. User opens Transfer screen (from Settings nav or deep link)
2. Selects source account, target account, amount, date
3. Taps Transfer
   → ViewModel.addTransfer()
   → Creates OUT transaction on source account (type=transfer, amount)
   → Creates IN transaction on destination account (type=transfer, amount)
   → Updates both account balances
   → source: balance -= amount
   → destination: balance += amount
```

### 7.3 Split Transaction

```
1. In Add/Edit Transaction → enables split mode
2. Sets total amount (parent transaction amount)
3. Adds split rows with category + amount (children sum = parent amount)
4. Taps Save
   → ViewModel.addSplitTransaction()
   → Inserts parent: isSplitParent=true
   → Inserts children: isSplitChild=true, parentTransactionId=parent.id
   → Adjusts account balance once (parent total)
5. Viewing: parent shown in list, expandable to show children
6. Deleting parent → CASCADE deletes children
```

### 7.4 Borrow-Lend Workflow

```
Borrow:
1. Create/select PeerContact (if new)
2. Add Transaction type = "Borrow"
3. Select peer, account, amount
4. Save
   → Transaction inserted (type=borrow)
   → Account balance increases (money received)
   → PeerContact.totalReceived += amount

Lend:
1. Create/select PeerContact
2. Add Transaction type = "Lend"
3. Select peer, account, amount
4. Save
   → Transaction inserted (type=lend)
   → Account balance decreases (money given)
   → PeerContact.totalGiven += amount

Repay/Receive:
→ Opposite effect: Repay reduces what you owe
→ Receive reduces what is owed to you
```

### 7.5 Recurring Transaction Generation

```
1. User creates recurring transaction (daily/weekly/monthly)
2. RecurringGenerationWorker (WorkManager) runs on app start
3. Worker checks all active recurring entries
4. For each: if nextDate ≤ now → generates TransactionEntity copy
5. Updates nextDate → advanceOnePeriod(frequency)
6. (Runs via enqueueUniqueWork("RecurringGeneration", KEEP))
```

### 7.6 Budget Tracking Flow

```
1. User creates budget: select category + amount + month
2. Summary screen shows budget utilization:
   → Total budget (sum of all budgets in active period)
   → Total spent (sum of expense transactions for budgeted categories)
   → Budget remaining (totalBudget - totalExpense)
   → Top categories by utilization (budget vs actual)
3. Budget spinner shows green/amber/red depending on utilization
4. Month navigation to see historical budgets
```

### 7.7 Goal Progress Flow

```
1. User creates goal: name, target amount, optional deadline
2. Two ways to contribute:
   a) Manual: addContribution(goalId, amount)
      → goal.currentAmount += amount
      → If currentAmount >= targetAmount → marks completed
   b) Linked transactions: any transaction with goalId set
      → linkedAmount = sum of transaction.amount for that goal
3. Total progress = (currentAmount + linkedAmount) / targetAmount * 100
4. Summary Savings tab shows all goals with progress bars
```

### 7.8 Summary Dashboard Workflow

```
1. User lands on Summary (default screen)
2. Sees NetBalanceCard (income - expense for selected period)
3. Uses time filter pills to change period:
   Day / Week / Month / Year / All / Today / Last Month / etc.
4. Period navigation arrows to move forward/backward
5. Swipes horizontally between 5 tabs:
   Expense → Income → Lending → Transfers → Savings
6. Each tab shows pies, breakdowns, lists computed from transactions
7. Custom date range via DateRangePickerSheet
```

### 7.9 Import/Export Workflow

```
Export:
1. Settings → Export CSV (or JSON)
2. Select entity type (or "All Data")
3. Android SAF file picker → user selects save location
4. ExportRepository generates CSV/JSON → written to URI

Import:
1. Settings → Import CSV (or JSON)
2. Android SAF file picker → user selects file
3. ExportRepository reads, parses, validates, inserts
4. Results shown with counts per entity type
```

### 7.10 PIN Lock & Biometric Workflow

```
1. Settings → Enable PIN → setup dialog (4 digits)
   → PIN hashed with PBKDF2-SHA256 + random salt
   → hash & salt stored in DataStore preferences
2. (Optional) Enable biometric → requires PIN first
3. On app background: AppLockManager (ActivityLifecycleCallbacks) starts timer
4. On app foreground after auto-lock minutes:
   → AppLockScreen shown (PIN entry)
   → If biometric enabled: BiometricPrompt shown first
   → On success: appLockManager.unlock() → NavHost renders
5. Wrong attempts tracked → can enforce lockout
```

---

## 8. Reusable UI Components

| Component | File | Purpose |
|-----------|------|---------|
| NumericKeypad | `components/NumericKeypad.kt` | Number input pad |
| CategoryIcon | `components/CategoryIcon.kt` | Emoji/Material icon display |
| PieChartEntry | `components/PieChartEntry.kt` | Pie chart data + rendering |
| ScrollToTopModifier | `components/ScrollToTopModifier.kt` | Scroll-to-top FAB utility |
| TransactionCardDense | `components/TransactionCardDense.kt` | Compact transaction row |
| TransactionDetailSheet | `components/TransactionDetailSheet.kt` | Full detail bottom sheet |
| SplitTransactionCard | `components/SplitTransactionCard.kt` | Split transaction display |
| SplitRowCard | `components/SplitRowCard.kt` | Individual split row |
| TransferDialog | `components/TransferDialog.kt` | Transfer between accounts |
| TransactionsFilterControlsSheet | `components/TransactionsFilterControlsSheet.kt` | Filter bottom sheet |
| AddEditTransactionDialog | `dialogs/AddEditTransactionDialog.kt` | Transaction form (add/edit) |
| TransactionFormConfig | `dialogs/TransactionFormConfig.kt` | Form configuration per type |
| StatusPaneHero | `insights/status/StatusPaneHero.kt` | Hero number display |
| StatusPaneFigure | `insights/status/StatusPaneFigure.kt` | Metric figure card |
| StatusPaneFigureGrid | `insights/status/StatusPaneFigureGrid.kt` | Grid of figure cards |
| AccountComparisonChart | `components/AccountComparisonChart.kt` | Bar chart for accounts |
| SummaryHeader | `summary/components/SummaryHeader.kt` | Time filter header |
| NetBalanceCard | `summary/components/NetBalanceCard.kt` | Balance hero card |
| ExpenseBreakdownCard | `summary/components/ExpenseBreakdownCard.kt` | Expense pie + list |
| IncomeBreakdownCard | `summary/components/IncomeBreakdownCard.kt` | Income pie + list |
| TopCategoriesCard | `summary/components/TopCategoriesCard.kt` | Top budget categories |
| TopIncomeSourcesCard | `summary/components/TopIncomeSourcesCard.kt` | Top income sources |
| LendingOverviewCard | `summary/components/LendingOverviewCard.kt` | Lending summary |
| LendingPeopleList | `summary/components/LendingPeopleList.kt` | Per-person lending |
| SavingsComponents | `summary/components/SavingsComponents.kt` | Savings goals + accounts |
| TransferOverview | `summary/components/TransferOverview.kt` | Transfer summary |
| DateRangePickerSheet | `summary/components/DateRangePickerSheet.kt` | Date range picker |
| AppLockScreen | `auth/AppLockScreen.kt` | PIN/biometric lock screen |

---

## 9. Security & Privacy

### 9.1 PIN Protection

- **Algorithm:** PBKDF2 with HMAC-SHA256
- **Salt:** Random 256-bit salt per user, stored separately in DataStore
- **Verification:** Hash input PIN + stored salt, compare against stored hash
- **Attempt tracking:** Wrong attempt counter in DataStore

### 9.2 Biometric Authentication

- **API:** AndroidX BiometricPrompt
- **Fallback:** PIN entry if biometric fails or not enrolled
- **Prerequisite:** PIN must be set before enabling biometric

### 9.3 Auto-Lock

- **Mechanism:** `AppLockManager` registered as `ActivityLifecycleCallbacks`
- **Trigger:** App goes to background → timer starts
- **Duration:** Configurable (1, 5, 15, 30 minutes) in Settings
- **Reset:** Timer resets on successful unlock

### 9.4 Data Storage

- **Local:** Room (SQLite) database — no encryption at rest
- **Cloud:** Firebase Firestore — data encrypted in transit (TLS) and at rest (AES-256)
- **Backup:** `android:allowBackup="true"` in manifest (cloud backup enabled)
- **Relevant permissions:** `INTERNET`, `USE_BIOMETRIC`

---

## 10. Data Import/Export

### 10.1 Supported Formats

| Format | Direction | Scope | Detail |
|--------|-----------|-------|--------|
| CSV | Export | Per-type or ALL | Simple columnar format |
| CSV | Import | Per-type or ALL | With name→ID resolution |
| JSON | Export | All entities | Full data backup |
| JSON | Import | All entities | Full data restore |

### 10.2 CSV Format Details

Each entity type has its own CSV structure with a header row:

- **Transactions CSV:** 24 columns (id, account_id, category_id, sub_category_id, goal_id, peer_contact_id, tag_ids, date, amount, type, note, description, receipt_path, recurring_id, split_data, investment_platform, expected_return_date, created_at, is_recurring, is_split_parent, is_split_child, parent_transaction_id, is_transfer, to_account_id)
- **Accounts CSV:** 5 columns (name, type, balance, currency, color)
- **Categories CSV:** 6 columns (name, type, emoji, icon_type, parent_id, is_custom)
- **Budgets CSV:** 4 columns (category_id, amount, month, is_savings_target)
- **Goals CSV:** 7 columns (name, emoji, icon_type, target_amount, current_amount, deadline, is_completed)
- **Tags CSV:** 3 columns (id, name, color)
- **Peers CSV:** 6 columns (displayName, phoneNumber, email, description, totalGiven, totalReceived)
- **Recurring CSV:** 14 columns (account_id, type, amount, category_id, sub_category_id, goal_id, note, frequency, start_date, next_date, is_active, reminder_enabled, reminder_days, investment_app)
- **Templates CSV:** 7 columns (name, type, amount, account_id, category_id, sub_category_id, note)

### 10.3 Import Behavior

- Duplicate detection: On account name (skips existing), on category name+type, on transaction ID (updates if exists)
- Resolves account/category names to numeric IDs during import
- "ALL" CSV import writes each section to its respective table

### 10.4 ExportRepository (Implementation)

- Injected with all 9 DAOs + ApplicationContext
- Runs on `Dispatchers.IO`
- Uses Android SAF (Storage Access Framework) via `ActivityResultContracts`
- No external storage permissions needed (SAF handles access)

---

## 11. Cloud Sync (Firebase)

### 11.1 Architecture

| Component | Tech | Purpose |
|-----------|------|---------|
| Authentication | Firebase Auth (Google Sign-In) | User identity |
| Database | Firebase Firestore | Remote data storage |
| Sync Manager | FirebaseSyncManager | Bidirectional sync |
| DI Module | FirebaseModule | Provides auth + firestore instances |

### 11.2 Sync Flow

```
Local Change → queueChange() → PendingChange list
                                │
                    Online? ─── YES ───→ sync()
                      │                    │
                      │               pull() from Firestore
                      │               push() pending changes
                      │                    │
                      NO              update SyncState
                      │              save lastSyncTime
                  Wait for network
                  (ConnectivityManager callback)
```

### 11.3 Sync States

| State | Meaning |
|-------|---------|
| `SyncStatus.Idle` | No sync in progress |
| `SyncStatus.Syncing` | Sync in progress |
| `SyncStatus.Success` | Last sync succeeded |
| `SyncStatus.Error(msg)` | Last sync failed |
| `SyncStatus.Offline(pending)` | No network, N pending changes |

### 11.4 Limitations

- **Push is placeholder:** `push()` processes delete operations only — create/update data is not being sent to Firestore
- **Pull is placeholder:** Reads remote collections but does not merge with local data
- **No conflict resolution:** Last-write-wins approach (implementation pending)
- **No offline queue persistence:** Pending changes in memory only (lost on process death)
- **Collections:** `users/{userId}/{accounts|transactions|categories|budgets|goals|tags}`
- **Retry:** Exponential backoff (1s, 2s, 4s, 8s), max 3 retries

### 11.5 Authentication

- **Provider:** Google Sign-In
- **UI Flow:** Settings → Sign In → Google account picker
- **State:** Exposed via `AuthState` sealed class (`SignedOut` | `SignedIn(user)`)
- **Persistence:** Firebase Auth session persists across app restarts

---

## 12. Theme System

### 12.1 Theme Options

| Theme | Display Name | Primary Color |
|-------|-------------|---------------|
| COCO_BROWN | "Coco Brown" | Brown palette |
| CALM_GREEN | "Calm Green" | Green palette (default) |
| MIDNIGHT_BLUE | "Midnight Blue" | Blue palette |

### 12.2 Dark Mode

- **User setting:** Stored in DataStore (`dark_mode` boolean)
- **Default:** Follows system setting (if user hasn't set preference)
- **Theme selection:** Stored in DataStore (`selected_theme` enum)
- **First launch:** System dark mode detected via `isSystemInDarkTheme()`
- **Once user sets theme:** `hasUserSetTheme` flag toggles to manual mode

### 12.3 Theme Architecture

```
MoneyManagerApp
  └── MainActivity
        └── MoneyManagerTheme(appTheme, isDarkMode)
              ├── Theme.kt (color schemes per theme)
              ├── Type.kt (typography)
              └── AppTheme.kt (theme enum)
```

---

## 13. Background Work

### 13.1 RecurringGenerationWorker

| Property | Value |
|----------|-------|
| Class | `RecurringGenerationWorker` |
| Dependency | Hilt-worker integration |
| Trigger | App start (`MainActivity.onCreate`) |
| Work Policy | `ExistingWorkPolicy.KEEP` (only one) |
| Work Name | `"RecurringGeneration"` |
| Function | Checks all active recurring entries, generates transactions where `nextDate ≤ now` |

### 13.2 App Lock

| Property | Value |
|----------|-------|
| Class | `AppLockManager` |
| Mechanism | `ActivityLifecycleCallbacks` (registered in `MoneyManagerApp.onCreate`) |
| Behavior | Starts lock timer on background, locks UI on foreground if timer exceeded |

### 13.3 Widget

| Property | Value |
|----------|-------|
| Class | `QuickAddWidget` (Glance AppWidget) |
| Receiver | `QuickAddWidgetReceiver` |
| Actions | Quick-add Income, Expense, or Transfer |
| Layout | Custom drawable backgrounds (light + night variants) |

---

## 14. Limitations & Known Issues

### 14.1 ⚠️ Cloud Sync (Firebase) — PARTIALLY IMPLEMENTED

- **Push is incomplete:** Only DELETE operations are pushed to Firestore. CREATE/UPDATE data is not synced.
- **Pull is incomplete:** Data is read from Firestore but NOT merged back into the local Room database.
- **No conflict resolution:** No mechanism to handle concurrent edits.
- **Pending changes in memory only:** Not persisted — app restart loses queued changes.
- **No selective entity sync:** Tries to sync all entity types equally, no prioritization.

### 14.2 ⚠️ Insights Screen — INCOMPLETE

| Pane | Status |
|------|--------|
| Status Pane | ✅ Implemented |
| Risks Pane | ❌ Placeholder text only |
| Trends Pane | ❌ Placeholder text only |

### 14.3 ⚠️ Reports Screen — BEING DEPRECATED

- The legacy Reports screen (FRD-06) is being replaced by the Summary dashboard.
- Some reports features may be partially functional or untested.

### 14.4 ⚠️ Investment Account Types

- `AccountEntity.VALID_TYPES` does not include `"investment"` despite being a target feature.
- `investmentPlatform` field exists on TransactionEntity but no dedicated UI for investment tracking.

### 14.5 Known UI/UX Gaps

- **Design alignment:** Several sub-screens (Tags, Budgets, Goals) use different padding/spacing than the DESIGN-GUIDE template (see DESIGN-GUIDE.md §8).
- **Peer list click handler:** `onPeerClick` in NavHost is TODO (no peer detail screen).
- **BorrowLend screen:** Route exists (`borrow_lend`) but has no composable registered in NavHost.
- **CategoryArchived state:** `isArchived` field exists but may not be fully respected across all pickers.
- **Receipt preview:** Receipt path is stored but no full-screen preview UI exists.

### 14.6 Technical Limitations

- **Database encryption:** Room database is not encrypted at rest. Sensitive financial data is stored in plaintext SQLite.
- **No backup encryption:** JSON/CSV exports are unencrypted plaintext.
- **No data validation pipeline:** Import validation is basic (parse-or-null).
- **No automated tests:** No unit or instrumentation tests found in the codebase.
- **No CI/CD pipeline:** No GitHub Actions or similar configuration found.
- **Paging 3 integration:** Transactions screen uses Paging 3 but some filtering logic may not compose correctly with paging (filter results may show stale data if not reset).
- **ViewModel scope:** `SummaryViewModel` uses `AndroidViewModel` with `WhileSubscribed(5000)` — may hold large data in memory for 5 seconds after screen goes background.

### 14.7 Performance Considerations

- **Summary screen:** Recalculates all aggregates on any data change (combines 10+ flows). May be slow with large datasets (10,000+ transactions).
- **Filter state:** Full pipeline rebuilds on any filter change, recalculating all 5 tabs' data.
- **No database indices on certain query columns:** `peerContactId` on transactions has no index.
- **Image receipts stored as Base64 in DB:** Can cause significant database size increase.
- **No lazy loading for export:** Loads all entities into memory for CSV/JSON export.

### 14.8 Feature Gaps vs Original HTML App

From BRIDGE.md, the following features remain unimplemented compared to the original HTML reference app:

| Feature | Priority | Status |
|---------|----------|--------|
| Dashboard Time Filters | HIGH | ✅ Now implemented in Summary |
| Dashboard Charts | HIGH | ✅ Now implemented in Summary |
| Sub-categories | HIGH | ✅ Field exists but UI exposure may be partial |
| Reminders | MEDIUM | Not implemented |
| Goal Linked to Savings | MEDIUM | ✅ Field exists |
| Budget Progress Bar (colors) | MEDIUM | Possible but not explicitly confirmed |
| JSON Backup | LOW | ✅ Implemented |
| Receipts/Documents (full) | LOW | Stored but no preview UI |

---

## 15. Edge Cases & Error Handling Reference

### 15.1 Transaction Edge Cases

| Scenario | Behavior |
|----------|----------|
| Delete category with transactions | `categoryId` set to null (SET_NULL), transaction preserved |
| Delete account with transactions | CASCADE — all account transactions deleted |
| Split parent deleted | CASCADE — all split children deleted |
| Zero amount transaction | Validation rejects |
| Future date | Allowed (for scheduled entries) |
| Negative balance on account | Warning indicator (if UI supports) |

### 15.2 Budget Edge Cases

| Scenario | Behavior |
|----------|----------|
| Delete category with budget | CASCADE — budget deleted |
| Budget exceeded | Overrun count shown |
| No budgets for month | Empty state shown |

### 15.3 Goal Edge Cases

| Scenario | Behavior |
|----------|----------|
| Goal achieved | Auto-mark `isCompleted = true` |
| Contribution exceeds target | Allowed, shows >100% |
| Delete goal with transactions | Transactions kept, goalId unlinked |
| Past deadline | Shows "Overdue" indicator |
| No deadline set | Shows "No deadline" text |

### 15.4 Export/Import Edge Cases

| Scenario | Behavior |
|----------|----------|
| No network (sync) | Offline state, queued for retry |
| Export target unwritable | Error result with message |
| Import invalid CSV | Parse errors, row skips |
| Clear data | Confirmation dialog → full wipe |
| File not found (receipt) | Skip, log warning |

### 15.5 Sync Edge Cases

| Scenario | Behavior |
|----------|----------|
| Network lost mid-sync | Error, retry with backoff |
| Not signed in | Sync skipped |
| Firestore offline | Error result |
| Concurrent sync | Single pending queue |

---

## 16. Design Conventions for Training Material

### 16.1 UI Spacing Constants (from DESIGN-GUIDE.md)

| Element | Value |
|---------|-------|
| LazyColumn horizontal padding | 20.dp |
| LazyColumn vertical padding | 4.dp |
| Item spacing | 0.dp (use dividers) |
| FAB bottom spacer | 80.dp |
| Row vertical padding | 5.dp |
| Icon container size | 36.dp |
| Icon container corner radius | 10.dp |
| Icon size | 20.dp |
| Spacer after icon | 14.dp |
| Divider start padding | 50.dp |
| Divider thickness | 0.5.dp |

### 16.2 Design Patterns

| Pattern | Usage |
|---------|-------|
| `ScrollToTopBox` | Wraps LazyColumn for scroll-to-top FAB |
| `TopAppBar` with back arrow | Sub-screens with `onNavigateBack` |
| `SectionHeader` + `ListRow` | Settings sub-menus |
| `AlertDialog` | Forms and confirmations |
| `BottomSheet` | Filters, transaction details, date picker |
| `FloatingActionButton` + `Add` icon | Primary create action |
| Empty State | Center-aligned icon + message + CTA button |

### 16.3 Key Observations for Training

1. **Summary is the primary screen** — the dashboard users see first and interact with most
2. **Transactions is the most complex screen** — largest file (763 lines), Paging 3 integration, 8 filter dimensions
3. **Settings is the hub** — all secondary management screens accessed from Settings
4. **Data flows up** — Transactions feed Summary aggregations; Budgets, Goals, Reports all depend on transaction data
5. **Filters are sticky** — Transaction filters persist in navigation arguments
6. **No undo** — No undo mechanism for delete operations (immediate CASCADE)
7. **All amounts are positive** — transaction type determines direction (income = credit, expense = debit)
8. **Currency limited to 10 options** — INR, USD, EUR, GBP, JPY, CAD, AUD, CHF, CNY, BRL
