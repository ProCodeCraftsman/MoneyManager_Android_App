---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: AI-Assisted Transaction Drafting
status: planning
stopped_at: Milestone v3.0 started — defining requirements
last_updated: "2026-05-15T00:00:00.000Z"
last_activity: 2026-05-15 - Milestone v3.0 started, research in progress
progress:
  total_phases: 0
  completed_phases: 0
  total_plans: 0
  completed_plans: 0
---

# Project State

## Current Milestone: v3.0 AI-Assisted Transaction Drafting

Phase: Not started (defining requirements)
Plan: —
Status: Defining requirements

Last activity: 2026-05-15 — Milestone v3.0 started

## Milestone Goal

Integrate Gemini Nano (via Android AICore) as an opt-in drafting assistant. Users create transaction drafts from SMS, receipt images, or voice memos — AI suggests a pre-filled form, user reviews and confirms. App stays 100% functional without AI. All existing features unchanged.

## Phase Structure

TBD — roadmap being created (phases continue from 31+)

## Previous Milestone

v2.2 Insights Dashboard — Phases 27–30 complete (Data Layer, Navigation Shell, Status Pane, Risks Pane). Phase 31 Trends Pane was pending at milestone transition.

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

- **Last session:** 2026-05-15
- **Stopped at:** Milestone v3.0 started — research agents launched, requirements and roadmap pending
- **Next phase:** TBD — awaiting research completion and roadmap creation
