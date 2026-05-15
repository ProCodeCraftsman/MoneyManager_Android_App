---
phase: 33-data-ai-implementation
plan: 02
subsystem: ai
tags: ["draft-parser", "prompt-builder", "usecase"]
key-files:
  - MoneyManager/app/src/main/java/com/moneymanager/data/ai/DraftParser.kt
  - MoneyManager/app/src/main/java/com/moneymanager/data/ai/PromptBuilder.kt
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/GenerateDraftFromTextUseCase.kt
  - MoneyManager/app/src/main/java/com/moneymanager/domain/ai/TransactionDraft.kt
metrics:
  commits: 1
  files_changed: 4
  lines_added: 111
  lines_removed: 0
---

## Plan 33-02: DraftParser + PromptBuilder + UseCase Update

### Commits

| # | Commit | Description |
|---|--------|-------------|
| 1 | 5482266 | feat(33-02): create DraftParser + PromptBuilder, update UseCase to return Result<TransactionDraft> |

### Deviations

None. All changes match plan specification exactly.

### Self-Check

PASSED

- [x] DraftParser.kt: `object DraftParser` declared
- [x] DraftParser.kt: `ignoreUnknownKeys = true` in Json config
- [x] DraftParser.kt: uses `indexOf('{')`/`lastIndexOf('}')` for JSON extraction
- [x] DraftParser.kt: returns `Result.failure` on any exception
- [x] DraftParser.kt: zero `android.*` imports
- [x] PromptBuilder.kt: `object PromptBuilder` declared
- [x] PromptBuilder.kt: `fun build(rawText, context): String` declared
- [x] PromptBuilder.kt: `.take(20)` on categories for defense-in-depth
- [x] PromptBuilder.kt: string sanitization via `.replace("\"", "'")` and `.replace("\n", " ")`
- [x] PromptBuilder.kt: zero `android.*` imports
- [x] GenerateDraftFromTextUseCase: returns `Result<TransactionDraft>`
- [x] GenerateDraftFromTextUseCase: uses `PromptBuilder.build(rawText, context)`
- [x] GenerateDraftFromTextUseCase: uses `DraftParser.parse(rawResponse).getOrThrow()` inside `mapCatching`
- [x] GenerateDraftFromTextUseCase: placeholder inline buildString block removed
- [x] TransactionDraft.kt: `@Serializable` annotation added
