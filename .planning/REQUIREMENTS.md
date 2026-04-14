# Requirements: MoneyManager Android

**Defined:** 2026-04-14
**Core Value:** Personal finance management with comprehensive default categories and full dashboard

## v2.0 Requirements

### Default Categories

- [ ] **CAT-01**: App ships with 11 pre-configured Expense categories (Food & Dining, Transport, Shopping, Bills & Utilities, Health, Entertainment, Travel, Education, Home, Personal Care, Other Expense)
- [ ] **CAT-02**: App ships with 5 pre-configured Income categories (Salary, Freelance, Investment, Gift, Other Income)
- [ ] **CAT-03**: App ships with 2 pre-configured Savings & Investment categories (Mutual Funds, Fixed Deposit)
- [ ] **CAT-04**: Default categories cannot be deleted by users
- [ ] **CAT-05**: Default categories can be archived/unarchived by users
- [ ] **CAT-06**: Users can add custom categories (name, emoji, type)
- [ ] **CAT-07**: Users can edit custom categories
- [ ] **CAT-08**: Users can delete custom categories

### Sub-Categories

- [ ] **SUB-01**: Food & Dining category has sub-categories (Restaurants, Online Delivery, Bakery, Groceries)
- [ ] **SUB-02**: Transport category has sub-categories (Fuel, Cab/Ride Share, Public Transit)
- [ ] **SUB-03**: Shopping category has sub-categories (Clothing, Electronics, Home Goods)
- [ ] **SUB-04**: Bills & Utilities category has sub-categories (Electricity, Internet, Rent, Insurance)
- [ ] **SUB-05**: Health category has sub-categories (Doctor, Pharmacy, Health Insurance)
- [ ] **SUB-06**: Entertainment category has sub-categories (Streaming, Movies, Games)
- [ ] **SUB-07**: Travel category has sub-categories (Flights, Hotel, Activities)
- [ ] **SUB-08**: Mutual Funds category has sub-categories (Equity, Debt, ELSS, Index)
- [ ] **SUB-09**: Fixed Deposit category has sub-categories (Bank FD, Corporate FD)
- [ ] **SUB-10**: Users can add sub-categories to any category
- [ ] **SUB-11**: Sub-categories inherit parent's category type

### Category Archive

- [ ] **ARCH-01**: Users can archive default categories (hides from selection but preserves transaction history)
- [ ] **ARCH-02**: Users can unarchive archived categories
- [ ] **ARCH-03**: Archived categories do not appear in category picker
- [ ] **ARCH-04**: Archived categories preserve historical transaction data

### Dashboard - Time Filter

- [ ] **DASH-01**: Dashboard displays time filter bar with options: Day, Week, Month (default), Year, All, Custom
- [ ] **DASH-02**: Selecting "Custom" reveals two date-picker inputs (From/To)
- [ ] **DASH-03**: All stat cards and charts respond to selected time filter
- [ ] **DASH-04**: Recent transactions list ignores time filter (always shows last 8)
- [ ] **DASH-05**: Budget widget always shows current month (ignores dashboard time filter)

### Dashboard - Stat Cards

- [ ] **DASH-06**: Net Worth card displays sum of all account balances (always, regardless of time filter)
- [ ] **DASH-07**: Income card displays total income for selected period
- [ ] **DASH-08**: Expenses card displays total expenses for selected period
- [ ] **DASH-09**: Net card displays income minus expenses for selected period
- [ ] **DASH-10**: Stat cards have appropriate accent colors (gold for net worth, green for income, red for expenses)

### Dashboard - Charts

- [ ] **DASH-11**: Spending by Category displays as doughnut chart
- [ ] **DASH-12**: Chart shows expense breakdown by category for filtered period
- [ ] **DASH-13**: Tapping a chart segment opens drill-down panel showing all transactions in that category
- [ ] **DASH-14**: Drill-down panel shows transaction date, account, category, tags, and amount
- [ ] **DASH-15**: Drill-down panel has close button

### Dashboard - Recent Transactions

- [ ] **DASH-16**: Recent Transactions shows last 8 transactions
- [ ] **DASH-17**: Each transaction displays icon, title, date, account, category, tags, badges
- [ ] **DASH-18**: Amount displayed with type-specific coloring and sign prefix

### Dashboard - Budget Widget

- [ ] **DASH-19**: Budget widget displays if budgets are configured
- [ ] **DASH-20**: Shows each budget with progress bar
- [ ] **DASH-21**: Always uses current calendar month

### Dashboard - Recurring Reminders

- [ ] **DASH-22**: Gold-highlighted banner shows upcoming recurring transactions within reminder window
- [ ] **DASH-23**: Each reminder displays category emoji, name, amount, days until due, next occurrence date

## v2.1 Requirements (Future)

### Enhanced Dashboard

- **DASH-24**: Net worth trend chart over time
- **DASH-25**: Budget vs actual comparison widget
- **DASH-26**: Actionable insights ("spending up 30% vs last month")

### Category Management

- **CAT-09**: Category reordering/drag-and-drop
- **CAT-10**: Category color customization

## Out of Scope

| Feature | Reason |
|---------|--------|
| Category sharing/import | Niche feature, defer to v2+ |
| Auto-categorization rules | High complexity, requires rule engine |
| Customizable dashboard widgets | Low initial value, defer to v2+ |
| Cash flow forecast | High complexity, defer to v2+ |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| CAT-01 | Phase 12 | Pending |
| CAT-02 | Phase 12 | Pending |
| CAT-03 | Phase 12 | Pending |
| CAT-04 | Phase 12 | Pending |
| CAT-05 | Phase 12 | Pending |
| CAT-06 | Phase 12 | Pending |
| CAT-07 | Phase 12 | Pending |
| CAT-08 | Phase 12 | Pending |
| SUB-01 | Phase 12 | Pending |
| SUB-02 | Phase 12 | Pending |
| SUB-03 | Phase 12 | Pending |
| SUB-04 | Phase 12 | Pending |
| SUB-05 | Phase 12 | Pending |
| SUB-06 | Phase 12 | Pending |
| SUB-07 | Phase 12 | Pending |
| SUB-08 | Phase 12 | Pending |
| SUB-09 | Phase 12 | Pending |
| SUB-10 | Phase 12 | Pending |
| SUB-11 | Phase 12 | Pending |
| ARCH-01 | Phase 12 | Pending |
| ARCH-02 | Phase 12 | Pending |
| ARCH-03 | Phase 12 | Pending |
| ARCH-04 | Phase 12 | Pending |
| DASH-01 | Phase 13 | Pending |
| DASH-02 | Phase 13 | Pending |
| DASH-03 | Phase 13 | Pending |
| DASH-04 | Phase 13 | Pending |
| DASH-05 | Phase 13 | Pending |
| DASH-06 | Phase 13 | Pending |
| DASH-07 | Phase 13 | Pending |
| DASH-08 | Phase 13 | Pending |
| DASH-09 | Phase 13 | Pending |
| DASH-10 | Phase 13 | Pending |
| DASH-11 | Phase 13 | Pending |
| DASH-12 | Phase 13 | Pending |
| DASH-13 | Phase 13 | Pending |
| DASH-14 | Phase 13 | Pending |
| DASH-15 | Phase 13 | Pending |
| DASH-16 | Phase 13 | Pending |
| DASH-17 | Phase 13 | Pending |
| DASH-18 | Phase 13 | Pending |
| DASH-19 | Phase 13 | Pending |
| DASH-20 | Phase 13 | Pending |
| DASH-21 | Phase 13 | Pending |
| DASH-22 | Phase 13 | Pending |
| DASH-23 | Phase 13 | Pending |

**Coverage:**
- v2.0 requirements: 37 total
- Mapped to phases: 37
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-14*
*Last updated: 2026-04-14 after initial definition*