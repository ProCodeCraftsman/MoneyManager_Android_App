---
phase: 34-di-wiring-ai-availability
plan: 02
subsystem: di
tags: hilt, dagger, injection, coroutines, dispatchers-io, startup
requires:
  - phase: 34-01
    provides: AiModule with DeviceCapabilityManager @Singleton provider
provides:
  - DeviceCapabilityManager field injection in MoneyManagerApp
  - Async IO startup hook that calls checkAndCacheAvailability() on Dispatchers.IO
affects:
  - 35-ai-draft-source-screens
  - 36-dialog-integration-fab
tech-stack:
  added: []
  patterns:
    - "Application-scoped one-shot coroutine using CoroutineScope(Dispatchers.IO).launch — fire-and-forget, no ANR risk"
    - "Hilt @Inject field injection in @HiltAndroidApp Application subclass for singleton dependencies"
key-files:
  created: []
  modified:
    - MoneyManager/app/src/main/java/com/moneymanager/app/MoneyManagerApp.kt
key-decisions:
  - "Used CoroutineScope(Dispatchers.IO).launch instead of GlobalScope — avoids discouraged GlobalScope usage while keeping fire-and-forget semantics for an Application-scoped one-shot task"
  - "CoroutineScope(Dispatchers.IO) creates a new scope each time (no structured concurrency needed for a one-shot Application startup hook)"
requirements-completed:
  - AIFND-12
duration: 2 min
completed: 2026-05-15
---

# Phase 34 Plan 02: DI Wiring & AI Availability — Application Startup Hook Summary

**DeviceCapabilityManager field injection via Hilt @Inject in MoneyManagerApp, with async checkAndCacheAvailability() call launched on Dispatchers.IO at startup — non-blocking, no ANR risk, existing appLockManager unchanged**

## Performance

- **Duration:** 2 min
- **Started:** 2026-05-15T22:30:00Z
- **Completed:** 2026-05-15T22:31:47Z
- **Tasks:** 1
- **Files modified:** 1

## Accomplishments

- Added `DeviceCapabilityManager` as a Hilt-injected field in `MoneyManagerApp` (alongside existing `appLockManager`)
- Added `CoroutineScope(Dispatchers.IO).launch` in `onCreate()` to call `checkAndCacheAvailability()` asynchronously — the main thread returns immediately with no blocking
- Added required imports: `DeviceCapabilityManager`, `CoroutineScope`, `Dispatchers`, `launch`
- Preserved existing `appLockManager` field injection and `registerActivityLifecycleCallbacks(appLockManager)` call exactly as-is
- Verified no `runBlocking` present in the file — main thread blocking is prevented by design and checked via acceptance criteria

## Task Commits

Each task was committed atomically:

1. **Task 1: Add DeviceCapabilityManager injection and async IO startup hook** — `608e284` (feat)

**Plan metadata:** (committed below)

## Files Modified

- `MoneyManager/app/src/main/java/com/moneymanager/app/MoneyManagerApp.kt` — Added 4 new imports, `@Inject lateinit var deviceCapabilityManager: DeviceCapabilityManager` field, and `CoroutineScope(Dispatchers.IO).launch { ... }` block as last statement in `onCreate()` (10 insertions, 0 deletions, 0 modifications to existing lines)

## Decisions Made

- Used `CoroutineScope(Dispatchers.IO).launch` rather than `GlobalScope.launch` — explicit IO dispatcher usage communicates intent and avoids discouraged GlobalScope
- Created a new `CoroutineScope` for the one-shot Application startup call rather than holding a class-level scope — this is appropriate since the job is fire-and-forget and survives for the Application lifetime by definition (the launch completes or outlives the app, both acceptable)
- Did NOT use `runBlocking` — acceptance criteria enforce its absence to guarantee no main-thread blocking

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None — execution was straightforward. The file matched the plan's interface block exactly, three additive edits produced the expected result.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- `MoneyManagerApp.onCreate()` now asynchronously calls `DeviceCapabilityManager.checkAndCacheAvailability()` on `Dispatchers.IO` at startup — the AppLockManager lifecycle registration still happens first on the main thread
- On AICore devices: `checkAndCacheAvailability()` probes `PromptClient.create(context)` and caches "READY" in `PreferencesManager`
- On non-AICore devices: the catch block writes "NEVER" to `PreferencesManager` and the app continues normally
- Ready for Phase 35 (AI Draft Source Screens) — `AiAvailabilityRepository.isAiAvailable: Flow<Boolean>` will reflect the cached result
- Requirement AIFND-12 completed: DeviceCapabilityManager check runs once at app startup and caches result

## Self-Check: PASSED

- [x] File modified exists: MoneyManagerApp.kt
- [x] Acceptance criteria (10/10 pass):
  - `@Inject lateinit var deviceCapabilityManager: DeviceCapabilityManager` ✓
  - `CoroutineScope(Dispatchers.IO).launch` ✓
  - `deviceCapabilityManager.checkAndCacheAvailability()` ✓
  - Import: `DeviceCapabilityManager` ✓
  - Import: `CoroutineScope` ✓
  - Import: `Dispatchers` ✓
  - Import: `launch` ✓
  - No `runBlocking` ✓
  - `appLockManager` field unchanged ✓
  - `registerActivityLifecycleCallbacks(appLockManager)` unchanged ✓
- [x] Plan verification (6/6 checks pass):
  - `Dispatchers.IO` in file (line 23) ✓
  - `runBlocking` count = 0 ✓
  - `checkAndCacheAvailability` in file (line 24) ✓
  - `deviceCapabilityManager` @Inject field + usage (lines 5, 18, 24) ✓
  - `registerActivityLifecycleCallbacks` still present (line 22) ✓
  - `appLockManager` still present (lines 4, 15, 22) ✓
- [x] Commit exists: `608e284`

---

*Phase: 34-di-wiring-ai-availability*
*Completed: 2026-05-15*
