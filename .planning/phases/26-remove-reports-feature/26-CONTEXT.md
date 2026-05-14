# Phase 26: Remove Reports Feature - Context

**Gathered:** 2026-04-28
**Status:** Ready for planning

<domain>
## Phase Boundary

Remove ReportsScreen, ReportsViewModel, chart components (TrendLineChart, ExpensePieChart, CategoryBarChart), navigation routes, and all related UI from the app. Goal is to reduce app complexity by removing unused/underutilized features.

</domain>

<decisions>
## Implementation Decisions

### Feature Removal
- **D-01:** Remove ReportsScreen.kt (app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt)
- **D-02:** Remove ReportsViewModel.kt (app/src/main/java/com/moneymanager/app/ui/screens/ReportsViewModel.kt)
- **D-03:** Remove Reports route from MoneyManagerNavHost.kt
- **D-04:** Remove ReportsViewModel import and DI registration from MoneyManagerNavHost.kt
- **D-05:** Remove TrendLineChart.kt (app/src/main/java/com/moneymanager/app/ui/components/TrendLineChart.kt)
- **D-06:** Remove ExpensePieChart.kt (app/src/main/java/com/moneymanager/app/ui/components/ExpensePieChart.kt)
- **D-07:** Remove CategoryBarChart.kt (app/src/main/java/com/moneymanager/app/ui/components/CategoryBarChart.kt)

### Build Verification
- **D-08:** Build must succeed after removal - verify no compile errors

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `MoneyManager/docs/FRD-06-Reports.md` — Reports feature spec (being removed)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt` — Navigation routes
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt` — Screen to remove
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/ReportsViewModel.kt` — ViewModel to remove
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TrendLineChart.kt` — Chart to remove
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/ExpensePieChart.kt` — Chart to remove
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/CategoryBarChart.kt` — Chart to remove

</canonical_refs>

<codebase_context>
## Existing Code Insights

### Files to Remove
- ReportsScreen.kt: 754 lines, uses TrendLineChart, ExpensePieChart, CategoryBarChart
- ReportsViewModel.kt: Provides ReportsUiState with time ranges, trend data, category breakdown, budget progress, lending summary
- TrendLineChart.kt: Chart component for spending trends
- ExpensePieChart.kt: Chart component for category breakdown (also used in DashboardScreen)
- CategoryBarChart.kt: Chart component for budget progress

### Integration Points
- MoneyManagerNavHost.kt: Reports route at line 315, ReportsViewModel import at line 50
- DashboardScreen.kt: Uses ExpensePieChart (will need removal if charts removed)

### Usage Analysis
- ReportsScreen and ReportsViewModel: Only used in Reports feature
- TrendLineChart: Only used in ReportsScreen
- ExpensePieChart: Used in ReportsScreen and DashboardScreen
- CategoryBarChart: Only used in ReportsScreen

</codebase_context>

<specifics>
## Specific Ideas

User requested full removal including chart components (TrendLineChart, ExpensePieChart, CategoryBarChart). This means Dashboard's pie chart usage will also need to be removed or replaced.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 26-remove-reports-feature*
*Context gathered: 2026-04-28*