# Requirements: MoneyManager v2.2

**Defined:** 2026-04-28
**Core Value:** Financial clarity from transaction data alone — no assumptions, no budgets, no AI

## v2.2 Requirements

### Status Screen

- [ ] **STA-01**: User sees Net Position as a large hero number on the Status screen (current month: income − expense − savings + borrowing − lending)
- [ ] **STA-02**: User sees Net Cash Flow (income − expense) below the hero number
- [ ] **STA-03**: User sees Total Income for the current month
- [ ] **STA-04**: User sees Total Expense for the current month
- [ ] **STA-05**: User sees Total Savings for the current month
- [ ] **STA-06**: User sees Total Lending for the current month
- [ ] **STA-07**: User sees Total Borrowing for the current month
- [ ] **STA-08**: User sees the current month and year label on the Status screen
- [ ] **STA-09**: User sees "No financial activity recorded yet" when no transactions exist

### Risks Screen

- [x] **RSK-01**: User sees up to 3 financial alerts on the Risks screen, negative alerts shown before positive
- [x] **RSK-02**: "Expenses exceed income this month" alert fires when total_expense > total_income
- [x] **RSK-03**: "Spending increased significantly compared to last month" alert fires when expense_change_percentage > 20 (suppressed when no previous-month transactions exist)
- [x] **RSK-04**: "Your financial position is negative" alert fires when net_position < 0
- [x] **RSK-05**: "Borrowing is high relative to income" alert fires when total_borrowing > total_income × 0.5
- [x] **RSK-06**: "Savings improved compared to last month" positive alert fires when total_savings > previous_month_savings (suppressed when no previous-month transactions exist)
- [x] **RSK-07**: Each alert displays an icon, a title, and a short explanation that includes the triggering number
- [x] **RSK-08**: User sees "No financial activity recorded yet" when no transactions exist

### Trends Screen

- [ ] **TRD-01**: User sees the dominant transaction type (highest total amount in current month) and its total amount
- [ ] **TRD-02**: User sees a daily income/expense line chart for the current month (income line + expense line, grouped by calendar day)
- [ ] **TRD-03**: Chart is not rendered when fewer than 2 daily data points exist; empty state message shown instead
- [ ] **TRD-04**: User sees "No financial activity recorded yet" when no transactions exist

### Navigation & Infrastructure

- [ ] **INF-01**: User can navigate to Insights from the bottom navigation bar
- [ ] **INF-02**: User can swipe left/right between Status, Risks, and Trends screens
- [ ] **INF-03**: A tab indicator shows which of the 3 screens is currently active

## Future Requirements

### Trends Enhancements

- **TRD-F01**: User sees expense % change vs previous month with direction indicator (increase/decrease/no change)
- **TRD-F02**: User can navigate to previous months to view historical insights

### Status Enhancements

- **STA-F01**: User sees month-over-month delta indicators next to each figure

## Out of Scope

| Feature | Reason |
|---------|--------|
| AI predictions or forecasts | Spec explicitly prohibits assumed data |
| Budget/goal integration | No budget data in scope — transaction records only |
| Category breakdowns | No category assumptions per spec |
| Swipe-to-dismiss alerts | Alerts are derived state; dismissal reappears unchanged on reopen |
| Month navigation (prev/next) | Lock to current month for v1 |
| Multi-month comparison charts | Scope limited to within-month daily view |
| Investment type as savings | Only SAVINGS type counts; investment excluded per data model |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| STA-01 | Phase 29 | Pending |
| STA-02 | Phase 29 | Pending |
| STA-03 | Phase 29 | Pending |
| STA-04 | Phase 29 | Pending |
| STA-05 | Phase 29 | Pending |
| STA-06 | Phase 29 | Pending |
| STA-07 | Phase 29 | Pending |
| STA-08 | Phase 29 | Pending |
| STA-09 | Phase 29 | Pending |
| RSK-01 | Phase 30 | Completed |
| RSK-02 | Phase 30 | Completed |
| RSK-03 | Phase 30 | Completed |
| RSK-04 | Phase 30 | Completed |
| RSK-05 | Phase 30 | Completed |
| RSK-06 | Phase 30 | Completed |
| RSK-07 | Phase 30 | Completed |
| RSK-08 | Phase 30 | Completed |
| TRD-01 | Phase 31 | Pending |
| TRD-02 | Phase 31 | Pending |
| TRD-03 | Phase 31 | Pending |
| TRD-04 | Phase 31 | Pending |
| INF-01 | Phase 28 | Pending |
| INF-02 | Phase 28 | Pending |
| INF-03 | Phase 28 | Pending |

**Coverage:**
- v2.2 requirements: 24 total
- Mapped to phases: 24/24 ✓
- Unmapped: 0

---
*Requirements defined: 2026-04-28*
*Last updated: 2026-04-28 — traceability filled by roadmap*
