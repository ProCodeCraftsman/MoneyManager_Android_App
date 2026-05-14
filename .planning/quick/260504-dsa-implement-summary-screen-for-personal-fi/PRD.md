# Product Requirements Document (PRD)

## Summary Screen — Personal Finance App

---

# 1. Objective

Provide a **single consolidated financial overview screen** that enables users to:

* Understand overall financial health
* Monitor expenses vs income
* Track category-level spending
* Identify overspending quickly

All values must be **derived dynamically from transaction records**.
No hard-coded values.

---

# 2. Data Source

Primary source: **Transaction Records**

Fields required:

* id
* type (EXPENSE, INCOME, SAVINGS, LENDING, BORROWING, TRANSFER)
* amount
* category
* account
* date
* budget_limit (category-level or global)
* status

---

# 3. Global Functionality

## 3.1 Data Handling

* All UI components must consume **processed aggregates**, not raw records
* Data must be:

  * Filtered
  * Grouped
  * Aggregated
* Calculations must update **reactively** when filters change

---

## 3.2 Time Filtering

Default:

* Current month

Supported:

* Day
* Week
* Month
* Custom range

All components must respect selected time range

---

## 3.3 Navigation Behavior

* Horizontal swipe → switch between tabs
* Vertical scroll → view content
* Sticky header + tabs

---

## 3.4 Theme & Styling

* Use **existing design system**
* Support:

  * Light mode
  * Dark mode
* No new style invention

---

# 4. Screen Sections

1. Header
2. Net Balance Summary
3. Navigation Tabs
4. Expense Summary Section (Active Tab)
5. Category Insights
6. Budget Insights

---

# 5. Components & Functional Description

---

## 5.1 Header

### Components

* Title: "Summary"
* Time selector
* Calendar icon
* Filter icon

### Function

* Displays active period
* Entry point for filters

### Behavior

* Tap date → open date selector
* Tap filter → open filter modal

---

## 5.2 Net Balance Summary Card

### Components

* Net Balance
* Income this period
* Expense this period
* Trend indicator

### Calculations

Income:

Sum(type = INCOME)

Expense:

Sum(type = EXPENSE)

Net Balance:

Income - Expense

Trend:

Compare with previous period

---

## 5.3 Navigation Tabs

### Components

* Expense (default active)
* Income
* Lending
* Transfers
* Savings

### Function

Switch dataset context

### Behavior

* Tap or swipe changes tab
* Active tab highlighted

---

# 6. Expense Screen (Primary Focus)

---

## 6.1 Expense Overview Card

### Components

* Total Spent
* Budget Remaining
* Circular Progress Indicator

### Calculations

Total Spent:

Sum(type = EXPENSE)

Budget Remaining:

Budget - Total Spent

Utilization %:

(Total Spent / Budget) × 100

---

## 6.2 Spend Category Pie Chart

### Components

* Pie chart of category-wise expense
* Legend (top categories)

### Function

Visual distribution of spending

### Data Logic

Group:

Transactions by category

Value:

Sum(amount)

Sort:

Descending

Limit:

Top categories (others grouped as "Others")

---

## 6.3 Top 5 Category Summary

### Components

* Category name
* Amount spent
* Percentage of total spend
* Progress bar

### Function

Shows **highest spending categories**

### Logic

Sort:

By total spend (descending)

Select:

Top 5

Percentage:

(category_spend / total_spend) × 100

---

## 6.4 Top 5 Budget Utilization

### Components

* Category name
* Budget limit
* Amount spent
* Utilization %
* Progress bar

### Function

Shows **categories closest to or exceeding budget**

### Logic

Utilization:

(category_spend / category_budget)

Sort:

Descending

Select:

Top 5

---

# 7. Filters

### Types

## 7.1 Time Filter

* Today
* Week
* Month
* Custom

## 7.2 Account Filter

* Bank
* Wallet
* Card

## 7.3 Category Filter

* Multi-select

## 7.4 Transaction Type Filter

* Expense
* Income
* Others

---

### Behavior

* Applies globally
* Updates all components instantly
* Persists during session

---

# 8. Additional Functional Considerations

## 8.1 Empty States

If no data:

* Show message:
  "No transactions available"
* Provide CTA:
  "Add Transaction"

---

## 8.2 Data Edge Cases

* Missing category → assign "Uncategorized"
* No budget → hide utilization % or show "N/A"
* Negative values → handle gracefully

---

## 8.3 Performance

* Load summary < 500ms
* Cache aggregates
* Lazy load charts

---

## 8.4 Interaction Enhancements

* Tap category → navigate to detailed transaction list
* Tap chart slice → filter by category
* Long press → quick actions (optional)

---

## 8.5 State Management

* Maintain:

  * Active tab
  * Selected filters
  * Scroll position

---

# 9. Design Constraints

* Use **existing UI components**
* Follow **existing spacing, typography, and color tokens**
* Maintain consistency across tabs
* No new component invention unless required

---

# 10. Accessibility

* Contrast ratio ≥ 4.5:1
* Minimum touch target: 48dp
* Support dynamic font scaling
* Screen reader labels for charts and values

---

# 11. Success Criteria

* User can identify:

  * Total spend
  * Top spending categories
  * Budget status

Within:

< 5 seconds of viewing screen

---

# End of Document
