---
phase: 38-local-ai-client
plan: 01
type: execute
subsystem: data-ai
tags: [ai-result, edge-ai-client, idle-timer, none-persistence]
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/AiResult.kt
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/GenAiClient.kt
  - MoneyManager/app/src/main/java/com/moneymanager/data/ai/EdgeAiClient.kt
metrics:
  new-files: 1
  modified-files: 2
  commits: 2
---

# Plan 38-01: AiResult sealed class + EdgeAiClient enhancements

## Commits

| # | Commit | Description |
|---|--------|-------------|
| 1 | TBD | feat(38-01): create AiResult sealed class and add generateDraftWithProgress to GenAiClient |
| 2 | TBD | feat(38-01): add PreferencesManager param, idle timer, and NONE persistence to EdgeAiClient |

## Deviations

None — all changes per plan spec.

## Self-Check

- [x] AiResult.kt exists with sealed interface (Loading, Success, Error) — zero Android imports
- [x] GenAiClient.kt has new `generateDraftWithProgress()` default method emitting Loading first
- [x] All 3 existing GenAiClient methods unchanged
- [x] EdgeAiClient has 4-param constructor including PreferencesManager
- [x] PreferencesManager and AiBackend imports present
- [x] buildEngine() persists NONE before throwing when all delegates fail
- [x] Idle timer infrastructure (idleScope, idleTimerJob, idleTimeoutMs = 5 min)
- [x] resetIdleTimer() called at start of each generateDraft() variant
- [x] cancelIdleTimer() called in cleanUp()
- [x] close() calls cancelIdleTimer() then cleanUp()

**Result: PASSED**
