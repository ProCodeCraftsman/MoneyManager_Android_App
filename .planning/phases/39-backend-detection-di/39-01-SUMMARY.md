---
phase: 39-backend-detection-di
plan: 01
subsystem: BackendDetection
tags:
  - DeviceCapabilityManager
  - FeatureStatus
  - bugfix
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/data/ai/DeviceCapabilityManager.kt
metrics:
  files_changed: 1
  tasks_completed: 2
  commits: 1
---

## Plan 39-01: DeviceCapabilityManager FeatureStatus Fix

### Task Results

| # | Task | Status |
|---|------|--------|
| 1 | Fix FeatureStatus mapping and remove cachedNoneTier | ✅ |
| 2 | Remove stale invalidateTierCache() call from MoneyManagerApp (no-op — call did not exist) | ✅ |

### Commits

| Commit | Description |
|--------|-------------|
| `HEAD` | fix(39-01): correct FeatureStatus mapping, remove cachedNoneTier, clean up detection flow |

### Deviations

None.

### Self-Check: PASSED

- FeatureStatus imported from `com.google.mlkit.genai.common.FeatureStatus` ✓
- `resolveBackendTier()` uses `FeatureStatus.AVAILABLE` (code 3) as first branch — returns AICORE ✓
- `FeatureStatus.UNAVAILABLE` (code 0) falls through to `else` — tries local model ✓
- `cachedNoneTier` field eliminated (zero occurrences in file) ✓
- `invalidateTierCache()` method eliminated (zero occurrences in file) ✓
- `resolveCurrentTier()` reads DataStore directly every call — no in-memory short-circuit ✓
- `hasSufficientRam` check remains first (fast-fail for low-RAM devices) ✓
- MoneyManagerApp.kt has no stale `invalidateTierCache` call ✓
