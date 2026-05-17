---
phase: 38-local-ai-client
plan: 02
type: execute
subsystem: lifecycle-docs
tags: [lifecycle-hooks, doc-update, lite-rt-lm]
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/app/MainActivity.kt
  - .planning/ROADMAP.md
metrics:
  modified-files: 2
  commits: 1
---

# Plan 38-02: Lifecycle hooks + doc updates

## Commits

| # | Commit | Description |
|---|--------|-------------|
| 1 | TBD | feat(38-02): wire EdgeAiClient.close() to MainActivity lifecycle + update ROADMAP.md |

## Deviations

- `REQUIREMENTS.md` had zero MediaPipe references — no changes needed
- `IMPLEMENTATION_PLAN.md` had zero MediaPipe references — no changes needed
- `ROADMAP.md` Phase 39 still references `LocalModelAiClient` — this is Phase 39's concern per spec boundary

## Self-Check

- [x] MainActivity has `@Inject lateinit var edgeAiClient: EdgeAiClient`
- [x] MainActivity overrides `onStop()` → `lifecycleScope.launch { edgeAiClient.close() }`
- [x] MainActivity overrides `onTrimMemory(TRIM_MEMORY_RUNNING_CRITICAL)` → same pattern
- [x] `ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL` used inline (no import)
- [x] ROADMAP.md Phase 38 overview references `EdgeAiClient` (not `LocalModelAiClient`)
- [x] ROADMAP.md Phase 38 plan description references `LiteRT-LM` (not `MediaPipe`)
- [x] IMPLEMENTATION_PLAN.md: no MediaPipe references found — no changes
- [x] REQUIREMENTS.md: no MediaPipe references found — no changes

**Result: PASSED**
