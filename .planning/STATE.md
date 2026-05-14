---
gsd_state_version: 1.0
milestone: v2.1
milestone_name: Progress
status: verifying
stopped_at: Phase 30 Risks Pane complete — 8/8 composables created, build verified
last_updated: "2026-04-30T10:31:15.816Z"
last_activity: 2026-05-04 - Completed quick task 260504-ewv: Fix PIN and Biometric Lock Issues
progress:
  total_phases: 20
  completed_phases: 13
  total_plans: 30
  completed_plans: 24
---

# Project State

## Current Milestone: v2.2 Insights Dashboard

Phase: 29
Plan: Not started
Status: Phase 30 complete — Risks Pane stateless composables and screen integration done, build verified

Last activity: 2026-04-30

## Milestone Goal

Add a 3-screen swipeable Insights section (STATUS / RISKS / TRENDS) derived purely from transaction records. All calculations deterministic (current vs previous month). Mobile-first large typography with failsafe empty state.

## Phase Structure

| Phase | Name | Requirements | Status |
|-------|------|--------------|--------|
| 27 | Data Layer & Calculations | Infrastructure (no user-facing requirements) | Not started |
| 28 | Navigation & Screen Shell | INF-01, INF-02, INF-03 | Not started |
| 29 | Status Pane | STA-01–STA-09 | Not started |
| 30 | Risks Pane | RSK-01–RSK-08 | ✅ Complete |
| 31 | Trends Pane | TRD-01–TRD-04 | Not started |

**Dependency chain:** 27 → 28 → 29, 30, 31 (29/30/31 all depend on 28 independently)

## Previous Milestone

v2.1 (Multiple Themes + Transaction enhancements) — Phase 26 was last phase.

## Accumulated Context

### Architecture decisions locked in by research

- Single InsightsViewModel; all panes share one StateFlow<InsightsUiState>
- InsightsCalculator is a pure Kotlin object — no Android runtime, fully unit-testable
- Data source: getTransactionsByDateRange() only — never getAllTransactions() (7 active subscribers already)
- HorizontalPager with hardcoded pageCount=3; TabRow synced to pagerState
- DailyLineChart: custom Canvas composable, not MPAndroidChart (zero new gradle dependencies)
- isSplitChild=true rows excluded from every aggregation
- hasEnoughHistory = prevMonthTxs.isNotEmpty(); comparison-based rules suppressed when false
- InsightsUiState decomposed into StatusUiState, RisksUiState, TrendsUiState sub-states

### Phase 27 Key Decisions (completed 2026-04-29)

- Q1 RESOLVED: SAVINGS only (not investment) for savings aggregation in InsightsCalculator
- Q2 RESOLVED: Net Position = income − expense − savings + borrowing − lending
- Q5 RESOLVED: Separate overspending (expense > income) and negative position (net < 0) rules
- hasEnoughHistory=false suppresses RSK-03 (expense increase) and RSK-06 (savings improvement)
- Max 3 alerts with WARNING severity before INFO (RSK-01)
- InsightsCalculator is pure Kotlin object with no Android imports
- InsightsViewModel uses getTransactionsByDateRange() ONLY — never getAllTransactions()

### Codebase context

- getAllTransactions() retained in DAO/Repository (used by DashboardViewModel, AccountsViewModel, BudgetsViewModel, ExportRepository)
- TransactionsViewModel uses Paging 3 (LazyPagingItems); Insights uses non-paginated date-range Flow
- ReportsScreen removed (phase 26) — chart components removed with it; Canvas pattern from AccountComparisonChart.kt is the model
- Compose BOM 2024.12.01 already on classpath — HorizontalPager, Canvas all available

### Open questions (resolved in Phase 27)

- ~~Q1: Does SAVINGS aggregate include investment-type transactions?~~ **RESOLVED: SAVINGS only (not investment)**
- ~~Q2: Net Position formula — income − expense − savings + borrowing − lending, or income − expense only?~~ **RESOLVED: income − expense − savings + borrowing − lending**
- Q3: Savings rate denominator when income = 0 (include borrowing or show N/A)? *(Not in Phase 27 scope)*
- ~~Q5: Overspending and negative net position — confirm as one consolidated alert rule~~ **RESOLVED: Separate rules — overspending (expense > income) and negative position (net < 0)**

## Decisions

### Phase 27 Key Decisions (completed 2026-04-29)

- Q1 RESOLVED: SAVINGS only (not investment) for savings aggregation in InsightsCalculator
- Q2 RESOLVED: Net Position = income − expense − savings + borrowing − lending
- Q5 RESOLVED: Separate overspending (expense > income) and negative position (net < 0) rules
- hasEnoughHistory=false suppresses RSK-03 (expense increase) and RSK-06 (savings improvement)
- Max 3 alerts with WARNING severity before INFO (RSK-01)
- InsightsCalculator is pure Kotlin object with no Android imports
- InsightsViewModel uses getTransactionsByDateRange() ONLY — never getAllTransactions()

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 20260430-001 | fix the gaps found in the export templates | 2026-04-30 | 5f36e49 | [20260430-001-fix-export-template-gaps](./quick/20260430-001-fix-export-template-gaps/) |
| 260504-dsa | implement Summary screen for personal finance | 2026-05-04 | a14bff7 | [260504-dsa-implement-summary-screen-for-personal-fi](./quick/260504-dsa-implement-summary-screen-for-personal-fi/) |
| 260504-ewv | Fix PIN and Biometric Lock Issues | 2026-05-04 | 90d962b | [260504-ewv-fix-pin-and-biometric-lock-issues](./quick/260504-ewv-fix-pin-and-biometric-lock-issues/) |

## Session Info

- **Last session:** 2026-05-04
- **Stopped at:** Quick task 260504-ewv complete — PIN unlock routing fixed, biometric re-trigger fixed, numeric keyboard added to PinSetupDialog, assembleDebug verified
- **Next phase:** Phase 31 — Trends Pane (UI-SPEC → Plan → Execute)
