# MoneyManager App - Functional Documentation

**Version:** 2.0  
**Last Updated:** April 2026

---

## 1. Introduction

MoneyManager is a personal finance management Android application that helps users track their income, expenses, savings, and transfers across multiple accounts. The app provides visual insights into spending patterns, budget tracking, goal monitoring, and recurring transaction automation.

---

## 2. App Screens

### 2.1 Dashboard (Home)
The main screen displaying financial overview:

| Feature | Description |
|---------|-------------|
| **Net Worth** | Total balance across all accounts |
| **Income Summary** | Total income for selected period |
| **Expense Summary** | Total expenses for selected period |
| **Net Balance** | Income minus expenses |
| **Spending Chart** | Doughnut chart showing expenses by category |
| **Budget Progress** | Current month budget status per category |
| **Recent Transactions** | Last 8 transactions |
| **Recurring Reminders** | Upcoming scheduled transactions |
| **Time Filter** | Day, Week, Month, Year, All, Custom range |

### 2.2 Accounts
Manage financial accounts:

| Feature | Description |
|---------|-------------|
| **Account Types** | Bank, Cash, Credit Card, Savings, Investment |
| **Create Account** | Add with name, type, initial balance, emoji, color |
| **Edit Account** | Modify account details |
| **Delete Account** | Remove account (cascades transactions) |
| **Account Comparison** | Visual chart comparing accounts |

### 2.3 Transactions
Record and manage all financial transactions:

| Feature | Description |
|---------|-------------|
| **Transaction Types** | Income, Expense, Savings, Transfer |
| **Create Transaction** | Add amount, account, category, date, description |
| **Edit Transaction** | Modify any transaction field |
| **Delete Transaction** | Remove transaction |
| **Split Transaction** | Divide into multiple categories |
| **Link to Goal** | Associate with savings goal |
| **Attach Receipt** | Store receipt image |
| **Search** | Find by description/note |
| **Filtering** | By date, account, category, tags, type |

### 2.4 Transfers
Move money between accounts:

| Feature | Description |
|---------|-------------|
| **Same Account Transfer** | Within same account |
| **Cross Account Transfer** | Between different accounts |
| **Transfer History** | View past transfers |

### 2.5 Categories
Organize transactions by category:

| Feature | Description |
|---------|-------------|
| **Expense Categories** | Food, Transport, Shopping, Bills, Entertainment, etc. |
| **Income Categories** | Salary, Freelance, Investment, Gift, etc. |
| **Savings Categories** | Mutual Funds, Fixed Deposit, Emergency Fund |
| **Subcategories** | Nested categories (e.g., Food → Restaurants, Grocers) |
| **Custom Categories** | User-created categories |
| **Archive/Unarchive** | Hide categories from picker |

### 2.6 Budgets
Monthly spending limits:

| Feature | Description |
|---------|-------------|
| **Set Budget** | Define amount per category |
| **Monthly View** | Budgets reset each month |
| **Progress Tracking** | Visual progress bar |
| **Savings Target** | Mark as savings goal |

### 2.7 Recurring Transactions
Automate regular transactions:

| Feature | Description |
|---------|-------------|
| **Frequency Options** | Daily, Weekly, Biweekly, Monthly, Yearly |
| **Auto-Generation** | Creates transactions on schedule |
| **Reminders** | Notification before due date |
| **Toggle Active** | Pause/resume recurring |
| **Link to Goal** | Associate with savings goal |

### 2.8 Goals
Track savings targets:

| Feature | Description |
|---------|-------------|
| **Create Goal** | Name, target amount, deadline |
| **Add Funds** | Add money to goal |
| **Progress Tracking** | Current vs target amount |
| **Mark Complete** | Mark goal as achieved |

### 2.9 Templates
Quick transaction entry:

| Feature | Description |
|---------|-------------|
| **Create Template** | Preset transaction details |
| **Quick Add** | One-tap transaction creation |
| **Reusable** | Use multiple times |

### 2.10 Tags
Additional transaction organization:

| Feature | Description |
|---------|-------------|
| **Create Tags** | Custom labels with colors |
| **Apply Tags** | Multiple tags per transaction |
| **Filter by Tag** | View tagged transactions |

### 2.11 Reports
Financial analytics:

| Feature | Description |
|---------|-------------|
| **Category Report** | Spending breakdown by category |
| **Trend Analysis** | Spending over time |
| **Monthly Comparison** | Compare across months |
| **Export Data** | Export to CSV/Excel |

### 2.12 Borrow/Lend
Track money with friends/peers:

| Feature | Description |
|---------|-------------|
| **Record Loan** | Money lent to peer |
| **Record Borrow** | Money borrowed from peer |
| **Peer List** | Manage contacts |
| **Track Balance** | Outstanding amounts |

### 2.13 Settings
App configuration:

| Feature | Description |
|---------|-------------|
| **Currency** | Set default currency |
| **Theme** | Light/Dark mode |
| **App Lock** | PIN protection |
| **Biometric Auth** | Fingerprint/Face unlock |
| **Data Backup** | Export to file |
| **Data Restore** | Import from backup |
| **Clear Data** | Reset all data |

---

## 3. Data Model

### 3.1 Entities

| Entity | Key Fields |
|--------|------------|
| **Account** | id, name, type, balance, currency, emoji, color |
| **Transaction** | id, accountId, type, amount, categoryId, date, description, note |
| **Category** | id, name, emoji, color, type, parentId |
| **Budget** | id, categoryId, amount, month |
| **Recurring** | id, accountId, type, amount, frequency, nextDate, isActive |
| **Goal** | id, name, targetAmount, currentAmount, deadline |
| **Tag** | id, name, color |
| **Template** | id, name, type, amount, accountId, categoryId |
| **PeerContact** | id, name, phone, email, totalOwed, totalOwing |

---

## 4. User Flows

### 4.1 Adding a Transaction
1. Open app → Dashboard
2. Tap "+" button
3. Enter amount
4. Select type (Income/Expense/Savings)
5. Choose account
6. Select category
7. Add description (optional)
8. Add tags (optional)
9. Link to goal (optional)
10. Tap Save

### 4.2 Creating a Budget
1. Go to Settings → Budgets
2. Tap "+" button
3. Select category
4. Enter budget amount
5. Tap Save

### 4.3 Setting Up Recurring
1. Go to Settings → Recurring
2. Tap "+" button
3. Fill form:
   - Account
   - Type (Income/Expense/Savings)
   - Amount
   - Category
   - Frequency
   - Start date
4. Enable reminder (optional)
5. Tap Save

### 4.4 Tracking a Goal
1. Go to Settings → Goals
2. Tap "+" button
3. Enter goal name
4. Set target amount
5. Set deadline (optional)
6. Tap Save
7. To add funds: tap goal → Add Amount → Save

---

## 5. Technical Architecture

| Component | Technology |
|-----------|------------|
| **Database** | Room (SQLite) |
| **Architecture** | MVVM + Clean Architecture |
| **DI** | Hilt |
| **UI** | Jetpack Compose |
| **Navigation** | Compose Navigation |
| **State** | ViewModel + StateFlow |
| **Background** | WorkManager |
| **Cloud** | Firebase |

---

## 6. Security Features

| Feature | Description |
|---------|-------------|
| **PIN Lock** | 4-digit PIN required to open app |
| **Biometric** | Fingerprint/Face authentication |
| **Auto-lock** | Lock after inactivity |
| **Secure Storage** | Encrypted preferences |

---

## 7. Additional Features

| Feature | Description |
|---------|-------------|
| **Quick Add Widget** | Home screen widget for fast entry |
| **Deep Links** | Open app from external links |
| **Data Export** | Export to CSV/Excel/JSON |
| **Firebase Sync** | Cloud backup and sync |

---

*Document Version 2.0*