# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Repository layout

- Git root is `D:\Android projec`. The Android project lives in `MoneyManager/` (single Gradle module `:app`).
- `MoneyManager/docs/` holds the functional docs (`FRD-0x-*.md`, `PRODUCT-FUNCTIONAL-DOCUMENT.md`, `DESIGN-GUIDE.md`). Read the relevant FRD before changing a feature, and update it when behaviour changes.
- `MoneyManager/BUGS.md`, `PLAN.md`, `BRIDGE.md` are historical working notes, not specs.
- `phone_db_backups/` contains real user data. Never read, commit or copy from it.

## Stack

Kotlin 2.3, Jetpack Compose (Material 3), Hilt, Room (DB version 18, schemas exported to `app/schemas`), Kotlin Serialization, WorkManager, Firebase (Crashlytics), on-device AI (LiteRT-LM and ML Kit GenAI). `minSdk 26`, `compileSdk/targetSdk 36`.

## Package map (`com.moneymanager`)

| Package | Responsibility |
|---|---|
| `data/` | Room entities, DAOs, repositories (impl), backup, AI clients, preferences |
| `domain/` | Use cases, repository interfaces, pure logic. Keep it Android-free where possible so it is unit-testable on the JVM |
| `domain/ai/` | Draft pipeline: `AiAgent` → `GenerateDraftFromTextUseCase` → `GenAiClient` |
| `domain/sms/` | Deterministic SMS rule engine plus `SmsVerifier`, learned account links, category prompt (see `docs/FRD-08-SMS-Parsing.md`) |
| `app/ui/` | Compose screens + ViewModels, one sub-package per feature |
| `app/smsanalyzer/` | Dev tooling: SMS masking and clustering (the screen itself is debug-only) |
| `di/` | Hilt modules |

## Build and test (Windows)

Run from `MoneyManager/`:

```
./gradlew.bat :app:testDebugUnitTest --offline -q                      # all unit tests
./gradlew.bat :app:testDebugUnitTest --tests "com.moneymanager.domain.sms.*" --offline -q
./gradlew.bat :app:compileDebugKotlin --offline -q
./gradlew.bat installDebug                                              # phone connected
```

- A successful `-q` run prints nothing; a failure prints the report path. Per-test failures are in `app/build/test-results/testDebugUnitTest/*.xml`.
- Tests are plain JVM JUnit 4 (`testImplementation` includes `org.json`, Mockito, coroutines-test). `unitTests.isReturnDefaultValues = true`.
- There is no emulator workflow. UI changes need a check on a real phone; say so when something is unverified on device.

## Conventions

- Match the surrounding code: naming, comment density, idiom. Comments explain why, not what.
- Commits: after a change is verified (compile and the relevant unit tests pass, or say which check was not run), commit all of its changes. Conventional prefix (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`), one logical change per commit, imperative summary. Stage paths explicitly. Never commit ignored or private files (see Privacy rules and `.gitignore`). Do not push unless asked.
- Any Room schema change needs a migration, a version bump, and a backup/export format update (JSON backup, CSV bundle, Drive backup). See the data-fidelity notes in memory before touching these.
- Money amounts are `Double` in entities today; do not round-trip through strings.
- Transaction types are `income`, `expense`, `savings`, `transfer`, `lend`, `borrow` (`domain/ai/TransactionType`). Account types are `bank`, `cash`, `credit`, `savings`, `peer`.
- Regexes: use Kotlin raw strings (`"""..."""`). Do not generate Kotlin regex source from Python/shell string replacement; `\b` gets mangled into a backspace character. Write the file with the Write/Edit tools.

## Privacy rules (hard constraints)

- `READ_SMS` is declared only in `app/src/debug/AndroidManifest.xml`. It must never appear in the release manifest (Play policy). Verify with the merged release manifest after any manifest change.
- Raw SMS text must not be persisted, exported or logged by the analyzer. Only masked templates leave the device, and the account holder's own name must be masked before sharing.
- Test fixtures derived from real messages must be scrubbed (names, account numbers, amounts) before being committed.

## On-device AI workstream (current focus)

Next phase of the app: local AI agents that turn SMS, then voice and images, into draft transactions. Docs live in `MoneyManager/docs/ai/`:

- `README.md`: principles, working agreement, status vocabulary, feature-file template. Read first.
- `BACKLOG.md`: ordered next items (IDs `AI-0xx`). Update it in the same change that finishes or adds an item.
- `features/F01..F08-*.md`: per-feature design, phases, acceptance, risks (SMS, voice, image, shared pipeline, evals, duplicate detection, auto-capture inbox, learning/audit/setup).

Rules for AI work (full reasoning in `docs/ai/README.md`):

- Target model is Gemma 4 E2B (2.4 GB) on a Galaxy S24 Ultra; everything must also work with no model. Deterministic rules and learned data first, the model last.
- The model only picks from a short numbered list the app builds; it never writes ids, amounts, dates or free text into a draft. Do not add features that depend on free-form JSON or tool calls from the model.
- Verify extracted fields against the source text; any doubt means no suggestion, plus `needsReview` and a flag. Nothing auto-saves except under an explicit per-source policy.
- Every new model prompt needs a strict reply parser, a timeout, and parser unit tests (clean, empty, prose, contradictory replies).
- State what is only unit-tested versus seen on the phone. Statuses: Idea, Planned, In progress, Built, Verified, Shipped.
- New capture sources (voice, image) plug into the shared pipeline (F04: extract, verify, resolve, policy, learn) rather than adding their own flow.

## SMS parsing feature

- Spec: `MoneyManager/docs/FRD-08-SMS-Parsing.md`. Runbook for refreshing rules: `MoneyManager/docs/SMS-ANALYZER.md`. Per-sender coverage: `MoneyManager/docs/SMS-PATTERN-CATALOG.md`.
- Pipeline for `sourceType == "SMS"`: rule engine → (matched, actionable) deterministic draft, or (non-transaction) clear rejection, or (unmatched) existing deterministic extractor + merchant cache + on-device AI, with the rule engine's partial fields passed as hints.
- Adding support for a new bank template: run the debug SMS Analyzer on the phone, export the JSON, add a rule in `BuiltInSmsRules.kt` plus a test case, update the catalog doc.

## Gotchas learned the hard way

- "credit" in "credit card" must never count as a credit direction. Direction needs past-tense verbs (`credited`, `debited`, `spent`, `received`).
- Scheduled/AutoPay/statement-due messages are reminders, not transactions. Card repayment SMS from the card issuer duplicates the bank-side debit, so it maps to a transfer, not an expense.
- Windows paths contain a space (`D:\Android projec`); quote them in shell commands.
