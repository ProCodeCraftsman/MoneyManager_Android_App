# MoneyManager Bridge Plan

## Overview

This document maps the features from the HTML reference app (MoneyManager) to the current Android implementation, identifies gaps, and provides a phased implementation plan.

---

## Gap Analysis

### ✅ Already Implemented (Android)
| Feature | HTML Reference | Android Status |
|---------|---------------|----------------|
| Accounts | Bank, Cash, Credit, Savings, Investment types | Implemented |
| Basic Transactions | Income/Expense/Savings types | Implemented |
| Budgets | Monthly limits per category | Implemented (basic) |
| Goals | Savings targets with progress | Implemented |
| Dashboard | Stats cards | Implemented |
| Settings | Dark mode, Currency, Cloud backup | Implemented |

### ❌ Missing in Android (Gaps)

| # | Feature | Priority | Description |
|---|---------|----------|-------------|
| 1 | Transaction Search | HIGH | Full-text search across transactions |
| 2 | Transaction Filters | HIGH | Filter by type, account, category, tag |
| 3 | Split Transactions | HIGH | Split one transaction into multiple categories |
| 4 | Transaction Tags | HIGH | Add custom tags to transactions |
| 5 | Sub-categories | HIGH | Categories with sub-categories (e.g., Food > Restaurants) |
| 6 | Dashboard Time Filters | HIGH | Filter stats by Day/Week/Month/Year/All |
| 7 | Dashboard Charts | HIGH | Pie chart for spending by category |
| 8 | Reminders | MEDIUM | Dashboard alerts for upcoming recurring |
| 9 | Recurring Transactions | MEDIUM | Auto-generate transactions on schedule |
| 10 | Templates | MEDIUM | Pre-filled transaction forms |
| 11 | Reports Screen | MEDIUM | Monthly trend, category breakdown charts |
| 12 | Budget Progress Bar | MEDIUM | Visual progress with color changes (80%/over) |
| 13 | Goal Contributions | MEDIUM | Add manual contributions to goals |
| 14 | Goal Linked to Savings | MEDIUM | Auto-count savings transactions toward goal |
| 15 | CSV Import/Export | MEDIUM | Bulk import from spreadsheet, export for analysis |
| 16 | JSON Backup Import/Export | LOW | Full data backup/restore |
| 17 | PIN Lock Setup | LOW | Set/change 4-digit PIN |
| 18 | Auto-lock Timeout | LOW | Lock app after idle time |
| 19 | Receipts/Documents | LOW | Attach images/PDFs to transactions |
| 20 | Transfer Between Accounts | MEDIUM | Move money without income/expense |

---

## Implementation Phases

### Phase 1: Core Transaction Features
**Goal:** Full transaction management parity with HTML

- [ ] Transaction search with full-text query
- [ ] Filter transactions by type, account, category, tag
- [ ] Transaction tags - create, assign, filter by tag
- [ ] Sub-categories support for all categories
- [ ] Transfer between accounts (creates transfer transaction, not income/expense)

### Phase 2: Dashboard Enhancements  
**Goal:** Rich dashboard with filtering and charts

- [ ] Time filter pills (Day/Week/Month/Year/All/Custom)
- [ ] Stats cards update based on filter
- [ ] Spending pie chart by category (clickable for drill-down)
- [ ] Budget widget on dashboard
- [ ] Dashboard reminders section

### Phase 3: Recurring & Reports
**Goal:** Automation and analytics

- [ ] Recurring transactions with frequency (weekly/monthly/yearly)
- [ ] Auto-create transaction entries
- [ ] Reminders for upcoming recurring
- [ ] Reports screen with monthly trend line chart
- [ ] Category breakdown horizontal bar chart
- [ ] Date range picker for reports

### Phase 4: Budgets, Goals, Templates
**Goal:** Full planning features

- [ ] Budget progress bar with color states (green/amber/red)
- [ ] Savings targets for investment categories
- [ ] Goal contributions (manual add)
- [ ] Link savings transactions to goals
- [ ] Goal target date with countdown
- [ ] Templates list
- [ ] Use template to pre-fill transaction

### Phase 5: Categories Management
**Goal:** Full category control

- [ ] Add custom expense/income/savings categories
- [ ] Add sub-categories to any category
- [ ] Custom category badge display
- [ ] Delete/edit categories
- [ ] Default categories seed data

### Phase 6: Data Management
**Goal:** Import/export capabilities

- [ ] Export transactions to CSV
- [ ] Import transactions from CSV with preview
- [ ] CSV template download
- [ ] Export full JSON backup
- [ ] Import JSON backup with confirmation
- [ ] Clear all data option

### Phase 7: Settings - Security
**Goal:** Privacy and security features

- [ ] PIN lock toggle and setup
- [ ] Change PIN dialog
- [ ] Biometric unlock toggle
- [ ] Auto-lock timeout options
- [ ] Storage usage display

### Phase 8: Receipts
**Goal:** Document attachment

- [ ] Attach receipt images to transactions
- [ ] Receipt thumbnail display
- [ ] Full-screen receipt preview
- [ ] Delete receipt from transaction

---

## Technical Notes

### Data Model Additions
```
TransactionEntity:
  - note: String (existing)
  - categoryId: Long (existing)
  - subCategoryId: Long (NEW)
  - tags: List<Long> (NEW)
  - isTransfer: Boolean (NEW)
  - toAccountId: Long (NEW for transfers)
  - recurringId: Long (NEW)
  - investmentApp: String? (NEW)
  - goalId: Long (NEW)
  - receipts: List<Receipt> (NEW)

CategoryEntity:
  - emoji: String (NEW)
  - subCategories: List<SubCategory> (NEW)

TagEntity:
  - name: String
  - color: String

RecurringEntity:
  - name, amount, type, accountId, categoryId
  - frequency: String (weekly/monthly/yearly)
  - startDate: Long
  - reminderDays: Int
  - autoCreate: Boolean
  - lastGenerated: Long?
  - active: Boolean
```

### Key UI Components to Add
1. `ExpensePieChart` - Already partially implemented
2. `TrendLineChart` - For reports
3. `TransferDialog` - Already exists
4. Search bar with filter chips
5. Tag input with color picker
6. Split transaction UI
7. Category drill-down panel

---

## Success Criteria

Each phase is complete when:
- All listed features are implemented
- App compiles without errors
- Basic manual testing passes
- No crash on normal usage paths

---

## Priority Ordering Rationale

1. **Phases 1-2** (Transactions + Dashboard): Core daily usage
2. **Phase 3** (Recurring + Reports): Regular periodic usage
3. **Phase 4** (Planning): Monthly review workflow
4. **Phase 5** (Categories): Setup phase
5. **Phase 6** (Data): Emergency backup capability
6. **Phase 7** (Security): Privacy requirements
7. **Phase 8** (Receipts): Edge case for documentation
