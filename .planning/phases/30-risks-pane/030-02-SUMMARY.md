---
phase: 30-risks-pane
plan: 02
subsystem: ui-insights-risks
tags: [compose, ui, risks-pane, screen-integration, viewmodel-wiring]
dependency_graph:
  requires: [RisksPaneHeader, RisksAlertCard, RisksAlertList, RisksPaneEmptyState, RisksHistoryDisclaimer, InsightsViewModel, InsightsUiState]
  provides: [RisksPaneScreen, RisksPaneErrorState, Updated InsightsScreen]
  affects: [Phase 31 Trends Pane integration]
tech-stack:
  added: [ViewModel wiring with collectAsStateWithLifecycle, HorizontalPager integration, Material 3 typography tokens]
  patterns: [State hoisting, Conditional rendering based on UI state, Column with verticalScroll]
key-files:
  created:
    - path: ui/insights/risks/RisksPaneErrorState.kt
      description: Error state view with retry callback and Material error color text
    - path: ui/insights/risks/RisksPaneScreen.kt
      description: Root screen wiring ViewModel state to composables with full state handling
  modified:
    - path: ui/screens/InsightsScreen.kt
      description: Updated to use RisksPaneScreen instead of RisksPaneStub
decisions:
  - "Followed StatusPaneScreen.kt pattern for ViewModel wiring via collectAsStateWithLifecycle"
  - "Used Column with verticalScroll (not LazyColumn) per UI-SPEC to avoid scroll interop conflicts in HorizontalPager"
  - "Conditional rendering: hasTransactions=false -> EmptyState, hasTransactions=true -> Header+List+Disclaimer hierarchy"
  - "Error state created but not yet wired to ViewModel (future enhancement when error state added to UiState)"
metrics:
  duration_seconds: 240
  completed_date: "2026-04-30"
---

# Phase 30 Plan 02: Risks Pane Screen Integration Summary

## One-liner
Created RisksPaneScreen root composable wiring ViewModel state to stateless composables, integrated into InsightsScreen replacing the stub.

## Tasks Completed

| Task | Name | Commit | Files | Status |
|------|------|--------|-------|--------|
| 1 | Create RisksPaneErrorState composable | d51b401 | RisksPaneErrorState.kt | ✅ Complete |
| 2 | Create RisksPaneScreen root composable | 1ecdb33 | RisksPaneScreen.kt | ✅ Complete |
| 3 | Update InsightsScreen.kt to use RisksPaneScreen | a8756e7 | InsightsScreen.kt | ✅ Complete |

## Deviations from Plan

### Auto-fixed Issues

None - plan executed exactly as written.

### Auth Gates

None encountered.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| threat_flag: text_display | RisksPaneScreen.kt | Renders user's own financial data from local ViewModel only, no external inputs |
| threat_flag: text_display | RisksPaneErrorState.kt | Error message displayed via Text composable (no HTML/web risks) |

## Verification Results

1. ✅ RisksPaneErrorState.kt exists, displays exact error copy from UI-SPEC: "Unable to load financial alerts. Pull to refresh or try again later."
2. ✅ RisksPaneErrorState uses full-screen centered Column layout with MaterialTheme.colorScheme.error
3. ✅ RisksPaneScreen.kt exists, wires to ViewModel via collectAsStateWithLifecycle
4. ✅ RisksPaneScreen handles hasTransactions=false → RisksPaneEmptyState (full-screen)
5. ✅ RisksPaneScreen renders header+list+disclaimer hierarchy when transactions exist
6. ✅ Alert ordering preserved (InsightsCalculator already sorts WARNING before INFO)
7. ✅ Max 3 alerts enforced (InsightsCalculator.take(3) already implemented)
8. ✅ Currency symbol (₹) appears in alert explanations (from InsightsCalculator)
9. ✅ UI-SPEC spacing: 32dp gap header-to-list (xl token), 16dp top padding for disclaimer
10. ✅ InsightsScreen.kt updated, page 1 calls RisksPaneScreen(modifier), RisksPaneStub function removed
11. ✅ Import for RisksPaneScreen added to InsightsScreen.kt
12. ✅ TrendsPaneStub preserved for Phase 31

## Key Decisions Made

1. **ViewModel Wiring Pattern**: Followed StatusPaneScreen.kt pattern exactly:
   - `val uiState by viewModel.uiState.collectAsStateWithLifecycle()`
   - Extract sub-state: `val risksState = uiState.risks`
   - Extract additional state: `val hasEnoughHistory = uiState.hasEnoughHistory`

2. **Conditional Rendering Logic**:
   - `!risksState.hasTransactions` → Show `RisksPaneEmptyState()` full-screen
   - `risksState.hasTransactions` → Show scrollable Column with:
     - `RisksPaneHeader()` at top
     - 32dp Spacer (xl token)
     - `RisksAlertList(alerts = risksState.alerts)`
     - If `!hasEnoughHistory`: 16dp Spacer + `RisksHistoryDisclaimer()`

3. **Scroll Implementation**: Used `Column` with `verticalScroll(rememberScrollState())` instead of `LazyColumn` per UI-SPEC to avoid scroll interop conflicts inside HorizontalPager (following PITFALLS.md guidance).

4. **Error State Reserved for Future**: Created `RisksPaneErrorState` with `onRetry` callback, but not yet wired to ViewModel. Error state handling can be added when ViewModel exposes error state in a future enhancement.

5. **Stub Removal**: Removed `RisksPaneStub` composable function entirely from InsightsScreen.kt as it's no longer needed.

## Self-Check: PASSED

- ✅ All 3 composable files exist:
   - `RisksPaneErrorState.kt` - FOUND
   - `RisksPaneScreen.kt` - FOUND
   - `InsightsScreen.kt` (modified) - FOUND

- ✅ All commits exist:
   - d51b401 - FOUND (Task 1: RisksPaneErrorState)
   - 1ecdb33 - FOUND (Task 2: RisksPaneScreen)
   - a8756e7 - FOUND (Task 3: InsightsScreen update)

- ✅ RiskyPaneScreen correctly wires to ViewModel and handles all states
- ✅ InsightsScreen.kt no longer shows "Risks Pane — coming in Phase 30" stub
- ✅ All 5 stateless composables from plan 01 are now integrated correctly
- ✅ No HIGH/CRITICAL risk warnings from impact analysis (all new files or safe modifications)
