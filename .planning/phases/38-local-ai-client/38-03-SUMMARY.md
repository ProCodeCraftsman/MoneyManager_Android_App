---
phase: 38-local-ai-client
plan: 03
type: execute
subsystem: test
tags: [test-suite, ai-result, edge-ai-client, delegate-cascade]
key-files:
  - MoneyManager/app/build.gradle.kts
  - MoneyManager/app/src/test/java/com/moneymanager/domain/ai/AiResultTest.kt
  - MoneyManager/app/src/test/java/com/moneymanager/data/ai/EdgeAiClientTest.kt
  - MoneyManager/app/src/test/java/com/moneymanager/data/ai/EdgeAiClientDelegateCascadeTest.kt
  - MoneyManager/app/src/test/java/com/moneymanager/data/ai/EdgeAiClientLifecycleTest.kt
metrics:
  new-files: 4
  modified-files: 1
  commits: 1
  test-classes: 4
  test-methods: 18
---

# Plan 38-03: Full EdgeAiClient test suite

## Commits

| # | Commit | Description |
|---|--------|-------------|
| 1 | TBD | feat(38-03): add test deps and create full EdgeAiClient test suite |

## Deviations

- `AllowedModel` data class is actually `ModelEntry` in this codebase — adapted test mocks accordingly
- `TransactionToolSet` is too complex to fake inline (requires 4 repositories) — using mockito mocks
- AllowedModel imports resolved via actual `ModelEntry` from LiteRtModelManager

## Self-Check

- [x] build.gradle.kts has 3 new test dependencies (mockito-core, mockito-kotlin, kotlinx-coroutines-test)
- [x] AiResultTest.kt — 6 tests covering Loading singleton, Success data, Error, when-expression, generateDraftWithProgress
- [x] EdgeAiClientTest.kt — 5 tests covering lazy init, model-not-downloaded, double-close, calculateMaxTokens, engine exception
- [x] EdgeAiClientDelegateCascadeTest.kt — 5 tests covering delegate cascade, NONE persistence, AiUnavailableException, model-not-downloaded guard
- [x] EdgeAiClientLifecycleTest.kt — 6 tests covering cleanUp, close idempotency, idle timer reset, streaming fallback, error handling
- [x] All tests are JUnit 4 (@Test annotation)
- [x] All tests in `com.moneymanager.*` package hierarchy

**Result: PASSED**
