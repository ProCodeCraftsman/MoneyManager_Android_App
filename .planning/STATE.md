---
gsd_state_version: 1.0
milestone: v3.0
milestone_name: AI-Assisted Transaction Drafting
status: in_progress
stopped_at: Completed 36-01-PLAN.md
last_updated: "2026-05-15"
last_activity: 2026-05-15 - 36-01 executed: Navigation foundation (AiDraft routes, draftJson nav arg, isAiAvailable wiring)
progress:
  total_phases: 5
  completed_phases: 4
  total_plans: 16
  completed_plans: 13
---

# Project State

## Current Milestone: v3.0 AI-Assisted Transaction Drafting

Phase: 36 — Dialog Integration & FAB
Plan: 1/4 — 36-01 complete
Status: In Progress

Last activity: 2026-05-15 — 36-01 executed: Navigation foundation (AiDraft routes, draftJson nav arg, isAiAvailable wiring)

## Milestone Goal

Integrate Gemini Nano (via Android AICore) as an opt-in drafting assistant. Users create transaction drafts from SMS, receipt images, or voice memos — AI suggests a pre-filled form, user reviews and confirms. App stays 100% functional without AI. All existing features unchanged.

## Phase Structure

| Phase | Name | Requirements | Status |
|-------|------|--------------|--------|
| 32 | Domain AI Foundation | AIFND-03, AIFND-05, AIFND-06, AIFND-07, AIFND-08 | Not started |
| 33 | Data AI Implementation | AIFND-01, AIFND-04, AIFND-09, AIFND-10 | ✅ Complete |
| 34 | DI Wiring & AI Availability | AIFND-02, AIFND-11, AIFND-12 | ✅ Complete |
| 35 | AI Draft Source Screens | SMS-01–10, OCR-01–09, VOICE-01–10, STD-01, STD-02, STD-03 | ✅ Complete |
| 36 | Dialog Integration & FAB | DRAFT-01–09, STD-04 | In Progress — 1/4 plans complete |

## Previous Milestone

v2.2 Insights Dashboard — Phases 27–30 complete (Data Layer, Navigation Shell, Status Pane, Risks Pane). Phase 31 Trends Pane was pending at milestone transition.

## Accumulated Context

### Architecture decisions locked in by research

- Domain layer (Phase 32) has zero Android imports — pure Kotlin, fully JVM-testable
- TransactionType enum is the single source of truth for type strings in all new AI code — prevents PITFALL-21 (prompt/registry drift)
- DeviceCapabilityManager stores "READY" / "NEVER" / "PENDING" string enum — NOT Boolean (PITFALL-01)
- PreferencesManager extended with one new key (ai_availability_status) — second DataStore delegate is forbidden (PITFALL-04)
- AiModule uses @javax.annotation.Nullable on nullable @Provides — Kotlin ? alone is insufficient for KSP (PITFALL-13)
- MoneyManagerApp startup hook launches async on Dispatchers.IO — no blocking IPC on main thread (PITFALL-14)
- AiDraftViewModel is a single shared @HiltViewModel for all 3 source screens (STD-03)
- SMS flow is clipboard/paste-first; READ_SMS is feature-flagged pending Play Console declaration approval (PITFALL-12)
- ImageProxy.close() called synchronously before any coroutine launch in OCR flow (PITFALL-08)
- SpeechRecognizer.destroy() called in DisposableEffect.onDispose (PITFALL-10)
- All changes to existing files use null-default parameters — strictly additive (STD-04, PITFALL-15)
- PromptContextBuilder caps categories at top-20 by usage frequency — token budget guard (PITFALL-05)
- DraftParser strips markdown fences, extracts JSON between first { and last }, uses ignoreUnknownKeys=true (PITFALL-07)
- clearDraft() called on dialog dismiss — prevents re-population on second open (PITFALL-15)
- 4 new Gradle lines only: genai-prompt:1.0.0-beta2, genai-common:1.0.0-beta3, play-services-mlkit-text-recognition:19.0.1, kotlinx-serialization-json:1.8.1
- No LaunchedEffect collecting navigationEvent in Transactions composable block — each AI source screen's composable drives its own nav via onNavigateToConfirm lambda, preventing double-navigation race on replay=0 SharedFlow (36-01)
- onDraftDismiss wired as empty lambda stub in NavHost; Plan 36-03 replaces with clearDraft() (36-01)
- draftJson nav arg deserialized with try/catch; malformed JSON produces null draft per threat model T-36-01 (36-01)

### Research flags (verify at integration time)

- Phase 33 (NanoAiClient): FeatureStatus constant names and PromptClient.create() / checkAvailability() signatures in genai-common:1.0.0-beta3 — beta API, verify against actual AAR
- Phase 33 (PromptBuilder): call countTokens() on a representative prompt to confirm top-20 category cap is sufficient
- Phase 35 (VoiceMemoScreen): offline speech model availability for hi-IN on target Indian market devices — test on real devices
- Phase 35 (SMS): start Play Console Permission Declaration Form process for READ_SMS in parallel with Phase 35 development

### Codebase context (from v2.2)

- getAllTransactions() retained in DAO/Repository (used by DashboardViewModel, AccountsViewModel, BudgetsViewModel, ExportRepository)
- TransactionsViewModel uses Paging 3 (LazyPagingItems); AI flows use non-paginated single-shot calls
- ReportsScreen removed (phase 26) — chart components removed with it; Canvas pattern from AccountComparisonChart.kt is the model
- Compose BOM 2024.12.01 already on classpath — all required Compose APIs available
- categoryUsageCounts already computed in AddTransactionViewModel — PromptContextBuilder reuses this, no extra DB query

### Previous milestone decisions (v2.2)

- InsightsCalculator is a pure Kotlin object — no Android runtime, fully unit-testable
- Data source: getTransactionsByDateRange() only — never getAllTransactions() (7 active subscribers already)
- HorizontalPager with hardcoded pageCount=3; TabRow synced to pagerState
- DailyLineChart: custom Canvas composable, not MPAndroidChart (zero new gradle dependencies)
- isSplitChild=true rows excluded from every aggregation
- hasEnoughHistory = prevMonthTxs.isNotEmpty(); comparison-based rules suppressed when false
- InsightsUiState decomposed into StatusUiState, RisksUiState, TrendsUiState sub-states

## Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 20260430-001 | fix the gaps found in the export templates | 2026-04-30 | 5f36e49 | [20260430-001-fix-export-template-gaps](./quick/20260430-001-fix-export-template-gaps/) |
| 260504-dsa | implement Summary screen for personal finance | 2026-05-04 | a14bff7 | [260504-dsa-implement-summary-screen-for-personal-fi](./quick/260504-dsa-implement-summary-screen-for-personal-fi/) |
| 260504-ewv | Fix PIN and Biometric Lock Issues | 2026-05-04 | 90d962b | [260504-ewv-fix-pin-and-biometric-lock-issues](./quick/260504-ewv-fix-pin-and-biometric-lock-issues/) |

## Session Info

- **Last session:** 2026-05-15
- **Stopped at:** Completed 36-01-PLAN.md — Navigation foundation
- **Next phase:** Phase 36, Plan 36-02 — Expandable AI Draft FAB
