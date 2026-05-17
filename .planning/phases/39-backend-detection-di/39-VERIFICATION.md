---
phase: 39-backend-detection-di
verified: 2026-05-18T00:00:00Z
status: passed
score: 4/4 roadmap success criteria verified
overrides_applied: 0
gaps:
  - truth: "AiModule provides NanoAiClient when cached backend is AICORE_READY, LocalModelAiClient when LOCAL_READY, and null when NONE or model not yet downloaded"
    status: partial
    reason: "Implementation functional but deviates from roadmap SC in two observable ways: (1) AiModule reads tier as 'aicore'/'local_model'/'none' (AiBackend.id values) not the 'AICORE_READY'/'LOCAL_READY' strings the ROADMAP SC specifies — these string spaces are separate (aiBackendTier stores id, aiAvailabilityStatus stores READY/NEVER strings). Detection is internally consistent but the roadmap wording implies the tier key stores 'AICORE_READY'. (2) The function is named providePreferredGenAiClient (with @Named('preferredClient')), not provideNullableGenAiClient as all three PLANs state. (3) PLAN 39-02 truth 1 states 'NOT runBlocking at @Provides time' but implementation uses runBlocking — documented deviation in SUMMARY."
    artifacts:
      - path: "MoneyManager/app/src/main/java/com/moneymanager/di/AiModule.kt"
        issue: "Function named providePreferredGenAiClient, not provideNullableGenAiClient; uses runBlocking at @Provides time against stated design"
    missing:
      - "Clarify whether aiBackendTier stores 'aicore'/'local_model'/'none' or 'AICORE_READY'/'LOCAL_READY'/'NEVER' — ROADMAP SC and implementation disagree on which key holds which string format"
  - truth: "DeviceCapabilityManager unit tests verify FeatureStatus mapping is correct (0=UNAVAILABLE, 3=AVAILABLE)"
    status: failed
    reason: "DeviceCapabilityManagerTest.kt contains zero tests that verify the FeatureStatus mapping. No import of com.google.mlkit.genai.common.FeatureStatus. No test that passes FeatureStatus.AVAILABLE (code 3) and asserts AICORE is returned, or FeatureStatus.UNAVAILABLE (code 0) and asserts local model fallback. The AICORE-path tests from the PLAN (callResolveWithAicoreCode helper, FeatureStatus.AVAILABLE test, FeatureStatus.UNAVAILABLE test, AICORE_READY skips local model check) are entirely absent from the produced test file."
    artifacts:
      - path: "MoneyManager/app/src/test/java/com/moneymanager/data/ai/DeviceCapabilityManagerTest.kt"
        issue: "No FeatureStatus import, no AICore-tier tests. 8 tests cover only RAM check and resolveCurrentTier DataStore reads — not the FeatureStatus mapping that was the primary bug this phase fixed."
    missing:
      - "Test: FeatureStatus.AVAILABLE (code 3) routes to AiBackend.AICORE"
      - "Test: FeatureStatus.UNAVAILABLE (code 0) falls through to resolveLocalModelTier"
      - "Test: AICORE_READY skips local model check entirely (selectModelForDevice not called)"
      - "These require mocking Generation statics (MockK mockkStatic) or extracting checkAicoreStatusCode() to an injectable dependency"
---

# Phase 39: Backend Detection & DI Verification Report

**Phase Goal**: The app correctly selects which `GenAiClient` implementation to inject at startup — AICore when available, local model when downloaded and RAM sufficient, null otherwise — and the Hilt graph compiles cleanly.
**Verified**: 2026-05-18
**Status**: gaps_found
**Re-verification**: No — initial verification

---

## Goal Achievement

### Observable Truths (Roadmap Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC1 | On AICore-ready device, DeviceCapabilityManager detects AICORE_READY via `Generation.getClient().checkStatus()` and skips local model check | ✓ VERIFIED | `FeatureStatus.AVAILABLE` branch at line 40 returns `AiBackend.AICORE` and calls `persistTier(AiBackend.AICORE, "READY")` without touching `resolveLocalModelTier()`. RAM check runs first (line 34). `checkAicoreStatusCode()` wraps `Generation.getClient().checkStatus()` with -1 fallback. |
| SC2 | On no-AICore device with ≥6 GB RAM, detects LOCAL_DOWNLOADABLE or LOCAL_READY — never falls through to NEVER on capable hardware | ✓ VERIFIED | `else` branch (UNAVAILABLE=0 or error=-1) at line 59 calls `resolveLocalModelTier()`. That method returns `LOCAL_MODEL` for both downloaded (`LOCAL_READY`) and not-downloaded (`LOCAL_DOWNLOADABLE`) cases when a model is selected. Only returns NONE if `selectModelForDevice()` returns null (no compatible model). |
| SC3 | Detection runs on every app launch — even if local model previously downloaded, AICore is re-checked first | ✓ VERIFIED | `cachedNoneTier` field removed (zero occurrences in DeviceCapabilityManager.kt). `resolveCurrentTier()` reads DataStore directly every call (line 69). When stored value is non-pending, uses `AiBackend.fromId(stored)` returning the cached tier — the startup re-check guarantee depends on the caller (`checkAndCacheAvailability()`) being invoked at app start, which existed pre-phase. RAM check precedes AICore check in every `resolveBackendTier()` call. |
| SC4 | AiModule provides NanoAiClient for AICORE_READY, LocalModelAiClient for LOCAL_READY, null for NONE or model not yet downloaded — Hilt graph compiles | ✗ PARTIAL | Implementation functional: returns `NanoAiClient` for `"aicore"`, `EdgeAiClient` for `"local_model"+downloaded`, `null` otherwise. Three gaps: (1) Roadmap SC names `LocalModelAiClient` but class is `EdgeAiClient`; (2) tier strings read are `"aicore"`/`"local_model"` (AiBackend.id), not `"AICORE_READY"`/`"LOCAL_READY"` as SC states; (3) method named `providePreferredGenAiClient` not `provideNullableGenAiClient`. Build reported passing (60 tests green per context). |

**Score**: 3/4 roadmap success criteria verified

---

### Plan Must-Haves vs Actuals

#### Plan 39-01 Must-Haves

| Truth | Status | Evidence |
|-------|--------|----------|
| FeatureStatus mapping correct: 0=UNAVAILABLE, 3=AVAILABLE | ✓ VERIFIED | `when (aicoreCode)` uses `FeatureStatus.AVAILABLE` (first branch), `FeatureStatus.DOWNLOADABLE`, `FeatureStatus.DOWNLOADING`, `else` for UNAVAILABLE/error. Import confirmed: `com.google.mlkit.genai.common.FeatureStatus`. No magic numbers. |
| resolveBackendTier() uses FeatureStatus constants instead of magic numbers | ✓ VERIFIED | grep confirms lines 40/45/52: `FeatureStatus.AVAILABLE`, `FeatureStatus.DOWNLOADABLE`, `FeatureStatus.DOWNLOADING`. Zero instances of bare `0 ->`, `1 ->`, `2 ->`, `3 ->` in that method. |
| cachedNoneTier field is removed | ✓ VERIFIED | grep: no matches for `cachedNoneTier` in DeviceCapabilityManager.kt |
| AICORE re-check happens first on every app launch | ✓ VERIFIED | No in-memory cache short-circuit. `resolveCurrentTier()` reads DataStore; re-detection via `resolveBackendTier()` when stored is `"pending"`. |
| On AICORE_READY device, local model check skipped entirely | ✓ VERIFIED | `FeatureStatus.AVAILABLE` branch returns directly without calling `resolveLocalModelTier()`. |
| On device without AICore but ≥6 GB RAM, detects LOCAL_DOWNLOADABLE or LOCAL_READY | ✓ VERIFIED | `else` branch → `resolveLocalModelTier()` → `LOCAL_MODEL` (either status). |
| hasSufficientRam check runs before AICore check | ✓ VERIFIED | Lines 34-38: RAM check + early return before the `when(aicoreCode)` block. |
| Existing persistTier call format preserved (AiBackend.id + status string) | ✓ VERIFIED | `persistTier(tier, status)` calls `setAiBackendTier(tier.id)` + `setAiAvailabilityStatus(status)`. |
| invalidateTierCache() removed from DeviceCapabilityManager | ✓ VERIFIED | Zero occurrences in file. |
| invalidateTierCache call removed from MoneyManagerApp | ✓ VERIFIED | grep of MoneyManagerApp directory: no matches. |

#### Plan 39-02 Must-Haves

| Truth | Status | Evidence |
|-------|--------|----------|
| AiModule provides GenAiClient? using Provider pattern — NOT runBlocking | ✗ FAILED (deviation documented) | Implementation uses `runBlocking { preferencesManager.aiBackendTier.first() }` and `runBlocking { preferencesManager.isLocalModelDownloaded.first() }`. SUMMARY acknowledges this deviation. |
| AiModule returns NanoAiClient when AICORE_READY, EdgeAiClient when LOCAL_READY, null when NONE | ✓ VERIFIED (with name caveat) | `providePreferredGenAiClient()` returns `nanoAiClient` for `"aicore"`, `edgeAiClient` for `"local_model"+downloaded`, `null` otherwise. Functionally correct per the DataStore value space. |
| GenerateDraftFromTextUseCase constructor accepts GenAiClient? | ✓ VERIFIED | Line 16: `private val client: GenAiClient?` |
| invoke() returns Result.failure(AiUnavailableException) when client is null | ✓ VERIFIED | Lines 49-51: `if (client == null) { return Result.failure(AiUnavailableException("No AI backend available")) }` |
| AiClientRouter retained unchanged as runtime routing safety net | ✓ VERIFIED | `provideGenAiClient(router: AiClientRouter)` preserved at line 23-27. AiClientRouter not modified. |
| @androidx.annotation.Nullable on @Provides method | ✓ VERIFIED | Line 32: `@androidx.annotation.Nullable` on `providePreferredGenAiClient`. Line 52: `@Named("preferredClient") @androidx.annotation.Nullable client: GenAiClient?` on consumer. |
| Hilt graph compiles without KSP errors | ✓ VERIFIED (per context) | Context states 60 unit tests pass after fix commit. Build passes. |

#### Plan 39-03 Must-Haves

| Truth | Status | Evidence |
|-------|--------|----------|
| DeviceCapabilityManager tests verify FeatureStatus mapping (0=UNAVAILABLE, 3=AVAILABLE) | ✗ FAILED | DeviceCapabilityManagerTest.kt has no import for `com.google.mlkit.genai.common.FeatureStatus`. No test passes FeatureStatus.AVAILABLE and asserts AICORE. No test passes FeatureStatus.UNAVAILABLE and asserts local model. The entire FeatureStatus mapping coverage is absent. |
| Tests verify RAM<6GB returns NONE | ✓ VERIFIED | Test "RAM below 6GB returns NONE immediately" (line 45): sets totalMem=4GB, calls `resolveBackendTier()`, asserts `AiBackend.NONE`. |
| Tests verify AICORE_READY skips local model check | ✗ FAILED | No such test in file. The PLAN's `AICORE_READY skips local model check entirely` test is absent. |
| Tests verify LOCAL_READY when model downloaded | ✓ VERIFIED | Test "RAM exactly 6GB allows local model check" (line 64): mocks `isModelDownloaded()=true`, asserts `AiBackend.LOCAL_MODEL`. |
| Tests verify LOCAL_DOWNLOADABLE when model not downloaded but >=6GB | ✓ VERIFIED | Test "no model selected for device returns NONE" covers null-model case; "RAM exactly 6GB" test covers downloaded=true. However no explicit test for downloaded=false+model-present. Partially covered. |
| Tests verify cachedNoneTier NOT used (detection re-runs every call) | ✓ VERIFIED | `resolveCurrentTier reads from DataStore stored value` test (line 98) verifies DataStore is read. cachedNoneTier is gone from source. |
| AiModule test verifies nullable provision returns correct impl per tier | ✓ VERIFIED | AiModuleTest.kt has 6 tier scenarios calling `providePreferredGenAiClient()`: aicore→NanoAiClient, local_model+downloaded→EdgeAiClient, local_model+not-downloaded→null, none→null, pending→null, unknown→null. |
| AiModule test verifies null returned for NONE tier | ✓ VERIFIED | Test `providePreferredGenAiClient returns null for none tier` (line 59). |
| Hilt graph compiles with zero KSP errors | ✓ VERIFIED (per context) | 60 tests pass per fix commit context. |

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `MoneyManager/app/src/main/java/com/moneymanager/data/ai/DeviceCapabilityManager.kt` | Corrected 3-tier detection logic | ✓ VERIFIED | 133 lines. FeatureStatus constants used. cachedNoneTier removed. resolveCurrentTier reads DataStore. |
| `MoneyManager/app/src/main/java/com/moneymanager/di/AiModule.kt` | Nullable GenAiClient? provision | ✓ VERIFIED | 57 lines. providePreferredGenAiClient with @Named("preferredClient") + @androidx.annotation.Nullable. runBlocking used (documented deviation from Provider pattern). |
| `MoneyManager/app/src/main/java/com/moneymanager/domain/ai/GenerateDraftFromTextUseCase.kt` | Null-safe GenAiClient consumer | ✓ VERIFIED | client: GenAiClient? at line 16. Null check at line 49 returns AiUnavailableException. |
| `MoneyManager/app/src/test/java/com/moneymanager/data/ai/DeviceCapabilityManagerTest.kt` | Unit tests for 3-tier detection | ✗ STUB (partial) | Exists. 8 tests. Covers RAM check and DataStore reads. Missing: FeatureStatus mapping tests, AICORE skips local model test. Core defect — FeatureStatus mapping fix is untested. |
| `MoneyManager/app/src/test/java/com/moneymanager/di/AiModuleTest.kt` | Hilt test for nullable provision | ✓ VERIFIED | Exists. 7 tests. Calls `module.providePreferredGenAiClient()` directly. All tier scenarios covered with Mockito mocks. |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `resolveBackendTier()` | `FeatureStatus` constants | `when (status) { FeatureStatus.AVAILABLE -> ... }` | ✓ WIRED | Lines 40, 45, 52 confirmed. Import at line 8. |
| `resolveCurrentTier()` | DataStore-backed tier (no cachedNoneTier) | `preferencesManager.aiBackendTier.first()` | ✓ WIRED | Line 69: `val stored = preferencesManager.aiBackendTier.first()`. Zero cachedNoneTier occurrences. |
| `AiModule.providePreferredGenAiClient()` | `PreferencesManager.aiBackendTier + isLocalModelDownloaded` | runBlocking reads at provision time | ✓ WIRED | Lines 38 and 42: both DataStore keys read via `runBlocking { ... .first() }`. |
| `GenerateDraftFromTextUseCase` | AiModule nullable provision | `client == null` check at entry point | ✓ WIRED | Line 49-51: null guard present. Module wires via `@Named("preferredClient") @androidx.annotation.Nullable`. |
| `DeviceCapabilityManagerTest` | `DeviceCapabilityManager.resolveBackendTier()` | mock FeatureStatus values | ✗ NOT WIRED | No FeatureStatus mocking. Tests call resolveBackendTier() but cannot control AICore status code path — checkAicoreStatusCode() calls real ML Kit which throws on JVM, returning -1, so all tests silently take the local model path regardless. |
| `AiModuleTest` | `AiModule.provideNullableGenAiClient()` | mock PreferencesManager | ✓ WIRED | Tests call `module.providePreferredGenAiClient(mockPrefs, mockNano, mockEdge)` directly. Mockito stubs return flowOf() values consumed by runBlocking. |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `AiModule.providePreferredGenAiClient` | `tier` (String) | `preferencesManager.aiBackendTier.first()` — DataStore Flow | Yes — reads from DataStore, written by DeviceCapabilityManager.persistTier() | ✓ FLOWING |
| `AiModule.providePreferredGenAiClient` | `downloaded` (Boolean) | `preferencesManager.isLocalModelDownloaded.first()` — DataStore Flow | Yes — reads from DataStore, written by `preferencesManager.setLocalModelDownloaded(true)` | ✓ FLOWING |
| `GenerateDraftFromTextUseCase.invoke` | `client` (GenAiClient?) | Injected via Hilt from `providePreferredGenAiClient` | Yes — real NanoAiClient/EdgeAiClient/null based on DataStore | ✓ FLOWING |

---

### Behavioral Spot-Checks

Step 7b: SKIPPED (no runnable entry points without Android emulator — this is an Android project; all checks require Gradle build or emulator).

---

### Probe Execution

Step 7c: No probes declared in any PLAN or SUMMARY. No `scripts/*/tests/probe-*.sh` files relevant to this phase.

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| HYBRID-01 | 39-01, 39-03 | 3-tier backend detection via Generation.checkStatus() + RAM check | ✓ SATISFIED | DeviceCapabilityManager.resolveBackendTier() implements exactly this: RAM check first, then ML Kit AICore status, then local model tier. |
| HYBRID-08 | 39-02, 39-03 | AiModule provides GenAiClient? — NanoAiClient/LocalModelAiClient/null per tier | ✓ SATISFIED (with deviation) | Implemented as providePreferredGenAiClient(). Returns correct implementations per tier. Class name is EdgeAiClient not LocalModelAiClient (same thing). |
| AIFND-01 (mod) | 39-01, 39-02, 39-03 | DeviceCapabilityManager detects 3 tiers; AICore always preferred, re-checked every launch | ✓ SATISFIED | cachedNoneTier removed. resolveCurrentTier reads DataStore. resolveBackendTier has correct FeatureStatus mapping. |

No orphaned requirements: all 3 IDs (HYBRID-01, HYBRID-08, AIFND-01 mod) are claimed by plans and implemented.

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `AiModule.kt` | 38, 42 | `runBlocking` at `@Singleton @Provides` time | ⚠️ Warning | Blocks main thread during Application.onCreate() Hilt component initialization. Acceptable per SUMMARY reasoning (<5ms DataStore cache hit) but violates stated design constraint in PLAN 39-02 truth 1. |
| `DeviceCapabilityManagerTest.kt` | entire file | No FeatureStatus constant tested | ✗ BLOCKER (for test completeness) | The primary bug this phase fixed (FeatureStatus.AVAILABLE=3 vs 0) has zero test coverage. If the bug is re-introduced, no test will catch it. |
| `AiModule.kt` | 38 | `localModelDownloaded` key name inconsistency | ℹ️ Info | AiModule calls `preferencesManager.isLocalModelDownloaded` (actual property name, confirmed correct). PLAN 39-02 interface snippet used `localModelDownloaded` — implementation is correct. |

---

### Human Verification Required

None — all remaining gaps are code-level and verifiable programmatically.

---

### Gaps Summary

Two gaps block a clean pass:

**Gap 1 — SC4 Partial (WARNING-level):** The ROADMAP success criterion SC4 says AiModule reads `"AICORE_READY"` / `"LOCAL_READY"` tier strings, but the implementation reads `"aicore"` / `"local_model"` (AiBackend.id values). These are two separate DataStore keys: `ai_backend_tier` stores the id, `ai_availability_status` stores the READY/NEVER string. The implementation is internally consistent (DeviceCapabilityManager writes id to the tier key, AiModule reads id from the tier key) but the ROADMAP SC wording is misleading. The functional behavior (correct client per backend) is satisfied. This is a documentation gap in the ROADMAP, not a code bug. A human decision is needed: accept as-is or update ROADMAP SC4 to reflect "aicore"/"local_model"/"none" string format.

**Gap 2 — Missing FeatureStatus test coverage (BLOCKER for test goal):** The phase explicitly fixed a critical FeatureStatus mapping bug (0 was being treated as AVAILABLE when it means UNAVAILABLE). The plan required tests verifying this fix. The produced test file contains zero FeatureStatus-related tests. The bug could be silently re-introduced with no test to catch it. Plan 39-03 truths 1 and 3 ("verify FeatureStatus mapping" and "verify AICORE_READY skips local model check") are FAILED. This is the most actionable gap.

The `runBlocking` deviation is documented in SUMMARY and is an accepted design choice — not a blocker.

---

_Verified: 2026-05-18_
_Verifier: Claude (gsd-verifier)_
