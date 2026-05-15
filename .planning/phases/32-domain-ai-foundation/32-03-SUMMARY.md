---
phase: 32-domain-ai-foundation
plan: 03
status: completed
completed_at: 2026-05-15
subsystem: domain/ai
tags: [behavior, use-case, builder, no-android-imports]
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/PromptContextBuilder.kt
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/GenerateDraftFromTextUseCase.kt
metrics:
  files_created: 2
  total_domain_ai_files: 7
  android_imports: 0
  compilation: passed
---

## Commits

| Task | Description | Status |
|------|-------------|--------|
| Task 1 | Create PromptContextBuilder with top-20 sort+cap build() | Created |
| Task 2 | Create GenerateDraftFromTextUseCase with nullable GenAiClient injection | Created |

## Deviations

- GenerateDraftFromTextUseCase returns `Result<String>` not `Result<TransactionDraft>` — per design decision, DraftParser (Phase 33) will handle JSON-to-TransactionDraft parsing. Recorded at plan-spec level.
- Placeholder prompt template used in GenerateDraftFromTextUseCase — Phase 33 PromptBuilder will replace with proper templating.

## Self-Check

PASSED — All 7 domain/ai/ files compile together, zero Android imports, null-client path returns Result.failure(AiUnavailableException()), top-20 cap logic present.
