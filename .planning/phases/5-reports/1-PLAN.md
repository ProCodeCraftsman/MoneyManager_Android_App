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
- [ ] Add ReportsScreen to navigation
- [ ] Create ReportsViewModel
- [ ] Tab layout: Overview, Trends, Categories

## 2. Overview Tab
- [ ] Total income/expense for selected period
- [ ] Net savings amount
- [ ] Month-over-month comparison

## 3. Trends Tab
- [ ] Line chart showing income/expense over time
- [ ] Time range selector: Week, Month, Quarter, Year
- [ ] Cumulative balance trend

## 4. Categories Tab
- [ ] Pie/donut chart for expense breakdown
- [ ] List view with amounts and percentages
- [ ] Drill-down by category

## 5. Budget vs Actual Report
- [ ] Show budget progress across all categories
- [ ] Visual indicators for over/under budget
- [ ] Monthly budget summary

## 6. Data Aggregation
- [ ] Create ReportsRepository methods
- [ ] Efficient queries for date ranges
- [ ] Category aggregation calculations

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
