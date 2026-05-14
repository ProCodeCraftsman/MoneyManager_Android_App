---
phase: 27-data-layer-calculations
plan: 01
subsystem: data, calculation
tags: [insights, calculator, pure-kotlin, ui-state]

# Dependency graph
requires:
  - phase: none
    provides: [TransactionEntity, TransactionRepository]
provides:
  - InsightsCalculator pure Kotlin calculation engine
  - InsightsUiState data contracts for all 3 panes
  - StatusUiState, RisksUiState, TrendsUiState sub-states
affects: [28-navigation-screen-shell, 29-status-pane, 30-risks-pane, 31-trends-pane]

# Tech tracking
tech-stack:
  added: [pure Kotlin object, java.util.Calendar, java.text.SimpleDateFormat]
  patterns: [Pure Kotlin calculator (no Android deps), decomposed UI state, Calendar date calculations]

key-files:
  created:
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsCalculator.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsUiState.kt

key-decisions:
  - "Q1: SAVINGS only (not investment) for savings aggregation"
  - "Q2: Net Position = income − expense − savings + borrowing − lending"
  - "Q5: Separate overspending (expense > income) and negative position (net < 0) rules"
  - "hasEnoughHistory=false suppresses RSK-03 (expense increase) and RSK-06 (savings improvement)"
  - "Max 3 alerts with WARNING severity before INFO"

patterns-established:
  - "Pure Kotlin objects for calculation engines (no Android imports)"
  - "Decomposed UI state into sub-states per pane"
  - "Filter split children FIRST before all aggregations"

requirements-completed: []  # Infrastructure phase - no user-facing requirements

# Metrics
duration: 15min
completed: 2026-04-29
---

# Phase 27 Plan 01: Data Layer & Calculations - Calculator & State Summary

**Pure Kotlin InsightsCalculator with decomposed UI state contracts for Status/Risks/Trends panes**

## Performance

- **Duration:** 15 min
- **Started:** 2026-04-29T00:00:00Z
- **Completed:** 2026-04-29T00:15:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Created `InsightsCalculator` pure Kotlin object with no Android runtime imports
- Computes all 7 monetary figures (netPosition, netCashFlow, income, expense, savings, lending, borrowing) from same current-month window
- Split child transactions excluded via `filter { !it.isSplitChild }` applied to all aggregations
- Q1 resolution: SAVINGS type only (not investment) for savings aggregation
- Q2 resolution: Net Position formula = income − expense − savings + borrowing − lending
- Q5 resolution: Separate rules for overspending (expense > income) and negative position (net < 0)
- `hasEnoughHistory` flag suppresses comparison-based risk rules (RSK-03, RSK-06) when no prior month data
- Created `InsightsUiState`, `StatusUiState`, `RisksUiState`, `TrendsUiState` data contracts
- Max 3 risk alerts with WARNING severity before INFO (RSK-01)
- RiskAlert includes icon, title, explanation, and triggeringValue (RSK-07)

## Task Commits

1. **Task 1: Create InsightsUiState data contracts** - `7798ebc` (feat)
2. **Task 2: Create InsightsCalculator pure Kotlin object** - `5bd82d7` (feat)

**Plan metadata:** `958ea04` (docs: complete plan)

_Note: TDD tasks may have multiple commits (test → feat → refactor)_

## Files Created/Modified

- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsUiState.kt` - UI state contracts for all 3 panes (StatusUiState, RisksUiState, TrendsUiState)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsCalculator.kt` - Pure Kotlin calculation engine with date-range based insights

## Decisions Made

- Q1: SAVINGS only (not investment) - per STATE.md resolution
- Q2: Net Position = income − expense − savings + borrowing − lending - per STATE.md resolution
- Q5: Separate overspending and negative position rules - per STATE.md resolution
- hasEnoughHistory = previousMonthTxs.isNotEmpty() - suppresses RSK-03 and RSK-06
- Max 3 alerts, WARNING before INFO - per RSK-01

## Deviations from Plan

None - plan executed exactly as written

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- InsightsCalculator and InsightsUiState ready for Phase 28 (Navigation & Screen Shell)
- All calculation logic complete and unit-testable (pure Kotlin)
- ViewModel wiring (Plan 27-02) completes the data pipeline

---
*Phase: 27-data-layer-calculations*
*Completed: 2026-04-29*
