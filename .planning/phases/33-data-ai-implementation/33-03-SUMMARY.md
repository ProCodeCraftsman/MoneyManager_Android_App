---
phase: 33-data-ai-implementation
plan: 03
subsystem: ai
tags: ["nanoclient", "device-capability", "aicore"]
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/data/ai/NanoAiClient.kt
  - MoneyManager/app/src/main/java/com/moneymanager/data/ai/DeviceCapabilityManager.kt
metrics:
  commits: 1
  files_changed: 2
  lines_added: 46
  lines_removed: 0
---

## Plan 33-03: NanoAiClient + DeviceCapabilityManager

### Commits

| # | Commit | Description |
|---|--------|-------------|
| 1 | ef64ad1 | feat(33-03): create NanoAiClient implementing GenAiClient and DeviceCapabilityManager with 3-state availability |

### Deviations

**ML Kit API surface note:** The genai-prompt:1.0.0-beta2 and genai-common:1.0.0-beta3 APIs are beta and may differ from what was written. Both NanoAiClient and DeviceCapabilityManager use `PromptClient.create(context)` from genai-common. NanoAiClient calls `.runPrompt(prompt)` — exact method name to verify against AAR. DeviceCapabilityManager maps `FeatureStatus.AVAILABLE` → "READY", `UNAVAILABLE` → "NEVER", `DOWNLOADING` → "PENDING" — exact enum constant names to verify against genai-common AAR.

### Self-Check

PASSED

- [x] NanoAiClient.kt: `class NanoAiClient ... : GenAiClient` declared
- [x] NanoAiClient.kt: `override suspend fun generateDraft(prompt: String): Result<String>` implemented
- [x] NanoAiClient.kt: top-level try/catch wrapping entire implementation
- [x] NanoAiClient.kt: returns `Result.failure` in catch block — never throws
- [x] NanoAiClient.kt: imports `GenAiClient` and `AiUnavailableException`
- [x] DeviceCapabilityManager.kt: `class DeviceCapabilityManager` declared
- [x] DeviceCapabilityManager.kt: `suspend fun checkAndCacheAvailability()` implemented
- [x] DeviceCapabilityManager.kt: exactly 3 calls to `setAiAvailabilityStatus("READY"/"NEVER"/"PENDING")`
- [x] DeviceCapabilityManager.kt: catch block calls `setAiAvailabilityStatus("NEVER")` as fallback
- [x] DeviceCapabilityManager.kt: no `preferencesDataStore` declaration (no second DataStore)
- [x] DeviceCapabilityManager.kt: no Boolean value passed to `setAiAvailabilityStatus`
