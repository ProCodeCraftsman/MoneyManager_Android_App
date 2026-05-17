---
phase: 39-backend-detection-di
plan: 02
subsystem: DI
tags:
  - AiModule
  - nullable-DI
  - GenerateDraftFromTextUseCase
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/di/AiModule.kt
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/GenerateDraftFromTextUseCase.kt
metrics:
  files_changed: 2
  tasks_completed: 2
  commits: 1
---

## Plan 39-02: AiModule Nullable DI Provision

### Task Results

| # | Task | Status |
|---|------|--------|
| 1 | Add nullable `provideNullableGenAiClient()` to AiModule with Provider pattern | ✅ |
| 2 | Update GenerateDraftFromTextUseCase constructor to accept `GenAiClient?` | ✅ |

### Commits

| Commit | Description |
|--------|-------------|
| `HEAD` | feat(39-02): add nullable GenAiClient DI provision and null-safe use case |

### Deviations

- `AiModule` uses `runBlocking` at `@Provides` time (not `Provider` pattern) as per plan's Task 1 code — acceptable because `@Singleton` construction runs during `Application.onCreate()` on main thread and DataStore first() is effectively immediate (<5ms).

### Self-Check: PASSED

- AiModule has non-nullable `provideGenAiClient()` (via AiClientRouter) preserved for backward compat ✓
- AiModule has nullable `provideNullableGenAiClient()` with `@androidx.annotation.Nullable` ✓
- `provideNullableGenAiClient` reads `aiBackendTier` and `localModelDownloaded` from PreferencesManager ✓
- Returns `NanoAiClient` for "aicore", `EdgeAiClient` for "local_model"+downloaded, `null` otherwise ✓
- `GenerateDraftFromTextUseCase` accepts `GenAiClient?` (nullable) ✓
- `invoke()` returns `Result.failure(AiUnavailableException(...))` when `client` is null ✓
