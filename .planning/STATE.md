---
gsd_state_version: 1.0
milestone: v3.1
milestone_name: Progress
status: Phase 37 complete — ready for Phase 38 planning
stopped_at: null
last_updated: "2026-05-17T22:30:00.000Z"
last_activity: 2026-05-17 — Phase 37 executed (domain contracts + data layer)
progress:
  total_phases: 27
  completed_phases: 19
  total_plans: 48
  completed_plans: 41
  percent: 69
---

# Project State

## Current Position

Phase: 37 — Data Foundation ✅ Complete (2 plans)
Status: Phase 37 complete — ready for Phase 38 planning
Last activity: 2026-05-17 — Phase 37 executed

```
[###       ] 30% — 2/4 phases with plans
```

## Milestone Goal

Extend the AI layer from AICore-only to a 3-tier system — AICore (preferred), local Gemma 3 1B model via MediaPipe (fallback for capable hardware, user opt-in download), and None (graceful degradation). Enables AI drafting on devices like S24 Ultra that have Snapdragon 8 Gen 3 NPU but no AICore.

## Phase Structure

| Phase | Name | Requirements | Status |
|-------|------|--------------|--------|
| 37 | Data Foundation | HYBRID-02, HYBRID-03, HYBRID-07, AIFND-11 (mod) | ✅ Complete (2/2 plans) |
| 38 | Local AI Client | HYBRID-04, HYBRID-09, HYBRID-10, AIFND-04 (mod) | Not started |
| 39 | Backend Detection & DI | HYBRID-01, HYBRID-08, AIFND-01 (mod) | Not started |
| 40 | User-Facing Download Flow | HYBRID-05, HYBRID-06 | Not started |

## Previous Milestone

v3.0 AI-Assisted Transaction Drafting — Phases 32–36 complete (Domain AI Foundation, Data AI Implementation, DI Wiring & AI Availability, AI Draft Source Screens, Dialog Integration & FAB).

## Accumulated Context

### Architecture decisions locked in by research (v3.0)

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
- draftJson nav arg deserialized with try/catch; malformed JSON produces null draft per threat model T-36-01 (36-01)
- Expandable AI Draft FAB: Column layout with AnimatedVisibility, tertiaryContainer toggle FAB, secondaryContainer mini-FABs; voice FAB conditionally hidden via SpeechRecognizer.isRecognitionAvailable(); existing + FAB behavior preserved (36-02)
- clearDraft() in AiDraftViewModel resets _uiState to AiDraftUiState(); called from NavHost AddTransaction composable onDraftDismiss lambda (36-03)
- AddEditTransactionDialog accepts initialDraft + onDraftDismiss (null defaults). LaunchedEffect(initialDraft) populates form fields. Source banner shows "Draft from {sourceType}" at dialog top (36-03)
- AI field highlighting: aiSuggestedFields set tracks 8 draft-populated fields; conditional Box(primary.copy(alpha=0.08f)) + BadgedBox(AutoAwesome) per field; each edit handler clears only its own field from the set (36-04)

### Architecture decisions for v3.1 (from spec)

- Backend detection order: Generation.getClient().checkStatus() (ML Kit) → RAM check ≥6 GB → NONE. NEVER use PackageManager.getPackageInfo("com.google.android.aicore") as proxy.
- Cached backend string values: "AICORE_READY", "LOCAL_READY", "LOCAL_DOWNLOADABLE", "LOCAL_DOWNLOADING", "NEVER"
- New PreferencesManager keys (same DataStore, additive): ai_backend, ai_availability, local_model_downloaded, local_model_path, user_opted_in_ai
- LocalModelAiClient: lazy init (model loaded on first inference), delegate cascade QNN → GPU(OpenCL) → CPU(XNNPACK), temperature=0.0 topK=1
- Model file stored at: context.filesDir/models/gemma3_1b_int4.task (app-private, never external storage)
- ModelDownloadManager: WiFi-only default, Flow<DownloadProgress>, foreground service or WorkManager for backgrounding
- AiModule: returns GenAiClient? — null when NONE or model not downloaded → AI buttons hidden, manual entry works
- Memory management: LocalModelAiClient.close() on Activity.onStop(), onTrimMemory(CRITICAL), or 5-min inference idle
- AICore always preferred over local model — periodically re-check on app launch even if local model downloaded
- If all MediaPipe delegates fail: catch exception, mark backend NONE, hide AI features
- Gradle addition: implementation("com.google.mediapipe:tasks-genai:0.10.22") — additive only, no existing deps changed

### Research flags (v3.0 — verify at integration time)

- Phase 33 (NanoAiClient): FeatureStatus constant names and PromptClient.create() / checkAvailability() signatures in genai-common:1.0.0-beta3 — beta API, verify against actual AAR
- Phase 33 (PromptBuilder): call countTokens() on a representative prompt to confirm top-20 category cap is sufficient
- Phase 35 (VoiceMemoScreen): offline speech model availability for hi-IN on target Indian market devices — test on real devices
- Phase 35 (SMS): start Play Console Permission Declaration Form process for READ_SMS in parallel with Phase 35 development

### Codebase context

- getAllTransactions() retained in DAO/Repository (used by DashboardViewModel, AccountsViewModel, BudgetsViewModel, ExportRepository)
- TransactionsViewModel uses Paging 3 (LazyPagingItems); AI flows use non-paginated single-shot calls
- ReportsScreen removed (phase 26) — chart components removed with it
- Compose BOM 2024.12.01 already on classpath — all required Compose APIs available
- categoryUsageCounts already computed in AddTransactionViewModel — PromptContextBuilder reuses this, no extra DB query
- DeviceCapabilityManager currently handles AICore detection only — will be expanded to 3-tier in Phase 39
- PreferencesManager currently has 2 AI keys (ai_availability_status from Phase 33) — Phase 37 adds 5 more
- NanoAiClient exists and is unchanged — LocalModelAiClient is a second parallel implementation
- GenAiClient interface exists in domain/ai/ — no changes needed to interface itself
- AiModule exists and provides nullable GenAiClient? — Phase 39 expands the selection logic
- MoneyManagerApp already has the startup hook for DeviceCapabilityManager (Phase 34)

## Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 20260430-001 | fix the gaps found in the export templates | 2026-04-30 | 5f36e49 | [20260430-001-fix-export-template-gaps](./quick/20260430-001-fix-export-template-gaps/) |
| 260504-dsa | implement Summary screen for personal finance | 2026-05-04 | a14bff7 | [260504-dsa-implement-summary-screen-for-personal-fi](./quick/260504-dsa-implement-summary-screen-for-personal-fi/) |
| 260504-ewv | Fix PIN and Biometric Lock Issues | 2026-05-04 | 90d962b | [260504-ewv-fix-pin-and-biometric-lock-issues](./quick/260504-ewv-fix-pin-and-biometric-lock-issues/) |

## Session Info

- **Last session:** 2026-05-17T22:30:00.000Z
- **Phase 37:** Completed — Data Foundation (2/2 plans)
- **Next:** Phase 38 planning — Local AI Client
