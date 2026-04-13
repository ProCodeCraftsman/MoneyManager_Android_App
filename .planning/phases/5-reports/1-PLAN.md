<objective>
Implement Reports screen with charts and analytics
</objective>

<context>
Phase 4 requires Reports with charts. Currently missing entirely. Need comprehensive financial reporting:
- Income vs Expense trends
- Category breakdowns
- Monthly/yearly comparisons
</context>

<tasks>

## 1. Create Reports Screen Structure
- [x] Add ReportsScreen to navigation
- [x] Create ReportsViewModel
- [x] Tab layout: Overview, Trends, Categories, Budgets

## 2. Overview Tab
- [x] Total income/expense for selected period
- [x] Net savings amount
- [x] Month-over-month comparison

## 3. Trends Tab
- [x] Line chart showing income/expense over time
- [x] Time range selector: Week, Month, Quarter, Year
- [x] Cumulative balance trend

## 4. Categories Tab
- [x] Pie/donut chart for expense breakdown
- [x] List view with amounts and percentages
- [x] Drill-down by category

## 5. Budget vs Actual Report
- [x] Show budget progress across all categories
- [x] Visual indicators for over/under budget
- [x] Monthly budget summary

## 6. Data Aggregation
- [x] Create ReportsRepository methods (in ViewModel)
- [x] Efficient queries for date ranges
- [x] Category aggregation calculations

## 7. Export Functionality
- [ ] Export report as PDF
- [ ] Export data as CSV
- [ ] Share report option

</tasks>

<success_criteria>
- Reports screen shows meaningful financial analytics
- Charts render correctly with real data
- Can export/share reports
- All budget progress visible in reports
</success_criteria>
