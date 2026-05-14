---
phase: 27-data-layer-calculations
plan: 02
subsystem: viewmodel, data, navigation
tags: [insights, viewmodel, hilt, date-range, StateFlow]

# Dependency graph
requires:
  - phase: 27-data-layer-calculations
    provides: [InsightsCalculator, InsightsUiState, StatusUiState, RisksUiState, TrendsUiState]
provides:
  - InsightsViewModel wired to TransactionRepository
  - Reactive data pipeline using getTransactionsByDateRange()
affects: [28-navigation-screen-shell, 29-status-pane, 30-risks-pane, 31-trends-pane]

# Tech tracking
tech-stack:
  added: [HiltViewModel, AndroidViewModel, StateFlow, SharingStarted.WhileSubscribed]
  patterns: [Date-range based data loading, combine() for multiple flows, Calendar date calculations]

key-files:
  created:
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsViewModel.kt
  verified:
    - MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt
    - MoneyManager/app/src/main/java/com/moneymanager/data/repository/TransactionRepositoryImpl.kt

key-decisions:
  - "ViewModel uses getTransactionsByDateRange() ONLY - never getAllTransactions()"
  - "Current + previous month date ranges computed via Calendar"
  - "hasEnoughHistory flag passed through from InsightsCalculator"
  - "Currency from PreferencesManager applied to InsightsUiState"

patterns-established:
  - "ViewModel uses Calendar for date range calculations (like DashboardViewModel)"
  - "Combine current+previous flows with currency preference"
  - "SharingStarted.WhileSubscribed(5000) for StateFlow"

requirements-completed: []  # Infrastructure phase - no user-facing requirements

# Metrics
duration: 10min
completed: 2026-04-29
---

# Phase 27 Plan 02: Data Layer & Calculations - ViewModel Summary

**InsightsViewModel with date-range flows wired to TransactionRepository, emitting StateFlow<InsightsUiState>**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-29T00:15:00Z
- **Completed:** 2026-04-29T00:25:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Created `InsightsViewModel` with `@HiltViewModel` annotation
- Uses `getTransactionsByDateRange()` ONLY - never uses `getAllTransactions()` (success criterion 1 & 4)
- Current month and previous month date ranges calculated using `Calendar` (success criterion 3)
- Injects `TransactionRepository` and `PreferencesManager`
- Emits `StateFlow<InsightsUiState>` with `hasEnoughHistory` flag (success criterion 2)
- Calls `InsightsCalculator.compute()` with both transaction lists
- Currency from `PreferencesManager.currency` flow applied to `InsightsUiState`
- Verified `TransactionRepository` and `TransactionRepositoryImpl` already have `getTransactionsByDateRange()` method

## Task Commits

1. **Task 1: Verify Repository has getTransactionsByDateRange** - verified (no commit needed)
2. **Task 2: Create InsightsViewModel** - `958ea04` (feat)

**Plan metadata:** `958ea04` (docs: complete plan)

_Note: TDD tasks may have multiple commits (test → feat → refactor)_

## Files Created/Modified

- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsViewModel.kt` - Hilt ViewModel with StateFlow<InsightsUiState> using date-range flows
- `MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt` - Verified has getTransactionsByDateRange (no changes)
- `MoneyManager/app/src/main/java/com/moneymanager/data/repository/TransactionRepositoryImpl.kt` - Verified delegates to DAO (no changes)

## Decisions Made

- ViewModel uses `Calendar.getInstance()` for date calculations (like DashboardViewModel pattern)
- `SharingStarted.WhileSubscribed(5000)` for StateFlow (standard pattern)
- `hasEnoughHistory` flag from `InsightsCalculator` passed through to UI state
- Currency preference applied in `combine()` block

## Deviations from Plan

None - plan executed exactly as written

## Issues Encountered

None

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- InsightsViewModel ready for Phase 28 (Navigation & Screen Shell)
- Data pipeline complete: Repository → ViewModel → StateFlow<InsightsUiState>
- All calculation logic in InsightsCalculator, all state contracts in InsightsUiState
- Phase 28 can now create the screen shell that consumes `InsightsViewModel.uiState`

---
*Phase: 27-data-layer-calculations*
*Completed: 2026-04-29*
