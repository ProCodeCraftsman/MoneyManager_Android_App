---
phase: 34-di-wiring-ai-availability
plan: 01
subsystem: di
tags: hilt, daggger, ksp, datastore, nullable
requires:
  - phase: 33-data-ai-implementation
    provides: NanoAiClient, DeviceCapabilityManager, GenerateDraftFromTextUseCase, GenAiClient, PreferencesManager
provides:
  - AiModule Hilt DI module with nullable GenAiClient? provider
  - AiAvailabilityRepository exposing Flow<Boolean> availability
affects:
  - 35-ai-draft-source-screens
  - 36-dialog-integration-fab
tech-stack:
  added: []
  patterns:
    - "Hilt @Module object with @javax.annotation.Nullable on @Provides methods and parameters for optional dependencies"
    - "Repository receives PreferencesManager via constructor injection, reads DataStore-derived Flows without owning a delegate"
key-files:
  created:
    - MoneyManager/app/src/main/java/com/moneymanager/di/AiModule.kt
    - MoneyManager/app/src/main/java/com/moneymanager/data/repository/AiAvailabilityRepository.kt
  modified: []
key-decisions:
  - "NanoAiClient() is no-arg — corrected from plan assumption of NanoAiClient(context)"
  - "DeviceCapabilityManager takes only PreferencesManager — corrected from plan assumption of (Context, PreferencesManager)"
  - "PromptClient.create(context) retained as AICore probe in provideGenAiClient — mirrors actual AICore API pattern"
patterns-established:
  - "Nullable DI bindings use @javax.annotation.Nullable (qualified) on both the @Provides method and the receiving parameter — KSP requirement"
requirements-completed:
  - AIFND-02
  - AIFND-11
duration: 1 min
completed: 2026-05-15
---

# Phase 34 Plan 01: DI Wiring & AI Availability Summary

**AiModule Hilt DI module with nullable GenAiClient? (null on non-AICore devices), DeviceCapabilityManager, and GenerateDraftFromTextUseCase providers + AiAvailabilityRepository exposing Flow<Boolean> availability derived from PreferencesManager**

## Performance

- **Duration:** 1 min
- **Started:** 2026-05-15T22:23:59Z
- **Completed:** 2026-05-15T22:24:54Z
- **Tasks:** 2
- **Files created:** 2

## Accomplishments

- Created `AiModule.kt` — Hilt `@Module @InstallIn(SingletonComponent::class)` object with 3 `@Provides` methods:
  - `provideGenAiClient()` — probes AICore via `PromptClient.create(context)`, returns `NanoAiClient()` or null on exception; annotated with `@javax.annotation.Nullable` on the method itself
  - `provideDeviceCapabilityManager()` — wires `DeviceCapabilityManager(preferencesManager)`
  - `provideGenerateDraftFromTextUseCase()` — receives `@javax.annotation.Nullable GenAiClient?` parameter; required for KSP null-safety
- Created `AiAvailabilityRepository.kt` — `@Singleton` class injecting `PreferencesManager`, exposing `isAiAvailable: Flow<Boolean>` mapped from `aiAvailabilityStatus` with strict equality (`status == "READY"`)
- Both files respect architectural invariants: no second DataStore delegate, no `androidx.datastore` imports in repository

## Task Commits

Each task was committed atomically:

1. **Task 1: Create AiModule.kt** — `0a7862e` (feat)
2. **Task 2: Create AiAvailabilityRepository.kt** — `32d3681` (feat)

**Plan metadata:** (committed below)

## Files Created

- `MoneyManager/app/src/main/java/com/moneymanager/di/AiModule.kt` — Hilt DI module with 3 @Provides methods for nullable GenAiClient?, DeviceCapabilityManager, and GenerateDraftFromTextUseCase
- `MoneyManager/app/src/main/java/com/moneymanager/data/repository/AiAvailabilityRepository.kt` — Repository exposing isAiAvailable: Flow<Boolean> from PreferencesManager.aiAvailabilityStatus

## Decisions Made

- Retained `PromptClient.create(context)` as the AICore probe in `provideGenAiClient()` — the plan's design was correct even though `NanoAiClient` constructors differed from assumptions
- Used qualified `@javax.annotation.Nullable` (not import) on both method and parameter sites to avoid annotation ambiguity with other `@Nullable` imports
- `AiAvailabilityRepository` uses constructor-injected `PreferencesManager` rather than owning its own DataStore delegate — enforces the single-delegate invariant

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] NanoAiClient() is no-arg — plan assumed NanoAiClient(context)**
- **Found during:** Task 1 (Create AiModule)
- **Issue:** Plan specified `NanoAiClient(context)` but the actual `NanoAiClient` class has no constructor parameters — it calls `Generation.getClient()` internally
- **Fix:** Changed to `NanoAiClient()` in the `provideGenAiClient` body
- **Files modified:** AiModule.kt
- **Verification:** Grep confirmed no `NanoAiClient(context)` call exists
- **Committed in:** `0a7862e` (Task 1 commit)

**2. [Rule 1 - Bug] DeviceCapabilityManager takes only PreferencesManager — plan assumed (Context, PreferencesManager)**
- **Found during:** Task 1 (Create AiModule)
- **Issue:** Plan specified `DeviceCapabilityManager(context, preferencesManager)` but the actual constructor takes only `preferencesManager: PreferencesManager`
- **Fix:** Removed `@ApplicationContext context: Context` parameter from `provideDeviceCapabilityManager` and changed constructor call to `DeviceCapabilityManager(preferencesManager)`
- **Files modified:** AiModule.kt
- **Verification:** Grep confirmed `DeviceCapabilityManager(preferencesManager)` is the only call
- **Committed in:** `0a7862e` (Task 1 commit)

---

**Total deviations:** 2 auto-fixed (2 bugs — plan constructor signatures differed from actual source code)
**Impact on plan:** Both fixes were essential for compilation. Without them, `NanoAiClient(context)` and `DeviceCapabilityManager(context, preferencesManager)` would cause Kotlin compile errors. No scope creep.

## Issues Encountered

- None — execution was straightforward after constructors were corrected from source

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Hilt DI graph can now provide nullable `GenAiClient?` (null on non-AICore devices), `DeviceCapabilityManager`, and `GenerateDraftFromTextUseCase`
- ViewModels can inject `AiAvailabilityRepository` to observe `isAiAvailable: Flow<Boolean>` for UI visibility gating
- Ready for Phase 34 Plan 02 (remaining DI wiring) and Phase 35 (AI Draft Source Screens)

## Self-Check: PASSED

- [x] AiModule.kt exists and matches all acceptance criteria (13/13)
- [x] AiAvailabilityRepository.kt exists and matches all acceptance criteria (7/7)
- [x] Plan-level verification (7/7 checks pass):
  - `@javax.annotation.Nullable` count = 2 ✓
  - `GenAiClient?` return type present ✓
  - `PromptClient.create()` + `NanoAiClient()` + null fallback ✓
  - `isAiAvailable: Flow<Boolean>` present ✓
  - No `preferencesDataStore` in repository (0 hits) ✓
  - `status == "READY"` mapping present ✓
  - No `androidx.datastore` imports (0 hits) ✓
- [x] Both files committed atomically (commit hashes: `0a7862e`, `32d3681`)

---

*Phase: 34-di-wiring-ai-availability*
*Completed: 2026-05-15*
