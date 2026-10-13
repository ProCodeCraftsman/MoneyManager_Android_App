# AI backlog

Ordered. Top of each section is next. Detail lives in `features/`. Status words are defined in `README.md`.
Last reviewed: 2026-10-13 (plan corrected after the SMS flow review: connect step, voice auto-commit, receiver scope, discarded AI-040 and AI-078).

## Execution order (across sections)

1. Done 2026-10-11: AI-006 built, AI-002 verified, AI-003/004 closed, v19 migration seen on the phone.
2. AI-010 (pipeline stages): the scanner needs "SMS in, draft out" without the UI flow.
3. AI-053 scanner, then AI-070 + AI-071 bootstrap (fills memory before the first inbox appears; also measures capture coverage).
4. AI-054 dedup, AI-055 Account Sources, AI-056 hub + inbox, AI-014 edit tracking.
5. AI-079 SMS connect step and AI-012 commit policy (removes the live voice auto-commit). AI-058 banner + scan on open and AI-059 reminders are built; they need the connect step to work on a fresh install.
6. AI-072 normalisation, AI-073 + AI-074 audit.
7. AI-057 model best-guess, AI-075 teach screen, AI-076 correction-aware memory, AI-077 model judge (shadow first).
8. Later: AI-060 receiver (pending draft and notification only, after AI-079), AI-045 EmbeddingGemma replay. AI-040 and AI-078 are discarded (see Discarded).

## Now (finish SMS on the real device)

| ID | Item | Feature | Size | Status | Notes |
|---|---|---|---|---|---|
| AI-006 | Stop `AiAgent` writing the model's category into merchant memory before the user saves (text and image paths); memory learns from saves only | F07, F08 | S | Built | 2026-10-11. Unit tests in `AiAgentSmsFlowTest` (AI text draft, rule draft with model pick, image draft); confirmed they fail with the old code. Not seen on the phone. Side effect: `quickAdd` auto-commits no longer teach memory (see AI-012) |
| AI-005 | Apply learned account links on the `ai_fallback` and AI-drafted paths | F01 | S | Built | Unit tests only (`SmsDraftMapperTest`); not seen on the phone. Also covers the merchant-cache fast path |
| AI-079 | SMS connect step: a first-run card on the home screen explains SMS reading (masked, no raw text kept) and asks for READ_SMS and a start date. While READ_SMS is missing, the banner says "Connect SMS to get suggestions" instead of hiding. The open scan only runs once granted | F07 | S | Planned | Found 2026-10-13: nothing requests READ_SMS on app open. Only the SMS inbox screen and the Settings reminder toggle ask today, so a fresh install gets a silent empty scan. Replaces the setup need that AI-078 covered |
| AI-012 | Auto-commit policy: remove the live auto-commit for voice (`AiDraftViewModel`: `autoCommit = sourceType == "VOICE"`, and `quickAdd`, which always passes `autoCommit = true`). Drafts are never saved without review (README principle 7). Any per-source policy comes later, if ever | F04 | S | Built | 2026-10-13. Auto-commit removed entirely: `AiAgent` no longer takes a transaction repository, so it cannot save; `quickAdd` and its Quick Add button are gone (voice opens the draft screen). Unit: `AiAgentSmsFlowTest` (15 pass, incl. a new voice test). Not seen on the phone. Left for later: `NavigateToCreated` handlers in three screens are now unreachable; `TransactionDraft.isHighConfidence` is unused |

## Next (shared platform, so voice and image do not fork the logic)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-010 | Extract `DraftPipeline` stages (Verify, ResolveAccount, ResolveCategory, Learn) out of `AiAgent` so every source calls the same code | F04 | M | Built | 2026-10-12. Stages: `SmsDraftPipeline.fromRules` (verify, account links, memory category), `SmsDraftPipeline.resolveAccount`, `SmsDraftPipeline.withCategoryFromMemory`, and `DraftLearning` (Learn: merchant memory and SMS account links written on save and on history confirm; the transfer destination rule is `DraftLearning.accountToLearn`). Unit tests (`SmsDraftPipelineTest`, `DraftLearningTest`, `AiAgentSmsFlowTest` unchanged). Not seen on the phone. Gap: the merchant-cache fast path in `AiAgent` still uses the plain `lookup` (no type check), so it is not the pipeline's category stage; changing it would alter which drafts reach the model |
| AI-011 | `CapturedInput` adapter type per source (SMS, VOICE, IMAGE, TEXT) with source-specific normalise + verify | F04 | M | Planned | AI-010 |
| AI-013 | Include learned account links and SMS-related learned data in backup/export (JSON, CSV bundle, Drive) | F04 | M | Built | Merged into AI-051 (2026-10-13): one item. Links, merchant memory and ledger are in AI-051's backup formats; unit tests only |
| AI-014 | Edit-tracking: record which fields the user changed before saving a draft (field names + source only, no values); show per-source zero-edit rate in a debug screen | F05 | M | Built | 2026-10-12. `DraftEdits` diff (names only) runs on every save of a captured draft and on inbox approve; counts live in DataStore (`DraftEditStats`, no migration, not backed up). Debug view under Add from > Draft edit stats. Unit tests (`DraftEditsTest`). Inline category changes count as an edit. Not seen on the phone; zero-edit rate not measured yet |
| AI-015 | Golden-fixture harness: scrubbed inputs with expected draft, run in unit tests, per source | F05 | M | Planned | none |
| AI-016 | Duplicate detection for voice and receipt sources, and transfer pairing. SMS-to-SMS and manual-vs-SMS dedup is already AI-054 | F06 | M | Planned | AI-010, AI-054 |

## Auto-capture (F07, ordered)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-050 | In-app JSON backup taken and stored off-device; then aggregate-only profile of it (accounts by type, sources, txns per month, duplicate-ish pairs; no names printed) | F07 | S | Verified | Done 2026-10-05; numbers in F07 "Data profile" |
| AI-051 | `captured_messages` ledger + draft table, migration v19, backup/CSV/Drive formats (includes AI-013) | F07 | L | Built | v18→v19 migration ran on the S24 over real data on 2026-10-11 (app opens, data intact). Restore of a v19 backup not yet run on the phone |
| AI-052 | Share-sheet text target into the draft flow | F07 | S | Planned | none |
| AI-053 | `personal` flavor (READ_SMS) + inbox scanner: cursor window with 2 h overlap, start date, anchor fallback, 31-day/500-message cap, rules only, writes pending ledger rows; check merged release manifest | F07 E4 | M | Built | 2026-10-11. 9 unit tests (`InboxScannerTest`: planner, drafts, ledger skip, non-transactions, overlap, cap, anchor). Play release manifest has no SMS permission (checked). Not run on the phone. The AI-058 trigger runs on open, but READ_SMS is not requested there, so it returns nothing until AI-079 |
| AI-054 | Semantic dedup: ref number, amount+account+window, transfer pairing, recurring/EMI/manual; uncertain matches shown as merge/keep-both | F07 E5, F06 | M | Built | 2026-10-12. Pure `CaptureDedup`: reference → saved → pending → uncertain. Each saved spend can absorb one SMS. Hidden only on a merchant match; amount + window alone is shown as "maybe duplicate". Unit tests (`CaptureDedupTest`, 11) and scanner tests (4). Not built: transfer pairing (own-account debit + credit), manual-vs-draft check at save time. Not seen on the phone. Ledger schema unchanged |
| AI-055 | Account Sources screen (map or create account) + balance cross-check | F07 E6 | M | Built | 2026-10-12, partial. Under Add from: maps each source to an account or creates one (opens at zero), and moves its pending drafts. Grouping and labels unit-tested (`AccountSourcesTest`). Not built: balance cross-check (the SMS balance is not kept on the draft). Not seen on the phone |
| AI-056 | Capture hub screen replacing the AI dropdown + draft inbox: bulk approve of deterministic rows only, inline category fix, reject / always-ignore, undo | F07 E7 | L | Built | 2026-10-12, partial. Add from replaces the FAB dropdown; SMS tile shows the pending count. Inbox scans on open, groups by day, "Save N ready" (expense and income with no flags), undo snackbar, reject, merge / keep both, top-5 category fix, and tap-to-edit. Not built: always-ignore list, paste-text tile. Only the pure rules are unit-tested (`InboxRulesTest`); inbox view model has no test. Not seen on the phone |
| AI-058 | Scan on app open + home banner (dismiss today / don't show) | F07 E8 | S | Built | 2026-10-12. `CaptureBanner` on the transactions screen, dismiss for today or don't show again (`CaptureBannerPolicy`, unit-tested in `CaptureBannerPolicyTest`). Scan runs on inbox open. Gap found 2026-10-13: a fresh install gets a silent empty scan until READ_SMS is granted (AI-079). Not seen on the phone |
| AI-059 | Noon / evening reminder worker: inexact alarm, scan first, notify only if count > 0, reboot reschedule, notification permission, battery tip | F07 E9 | M | Built | 2026-10-12. `CaptureReminderScheduler` / `CaptureReminderWorker` / `CaptureReminderReceiver` (boot and update re-arm), schedule logic in `CaptureReminderSchedule` (unit-tested in `CaptureReminderScheduleTest`). Settings toggle asks for READ_SMS and notification permission and shows a battery tip once. Not seen on the phone: notifications and timing need 3 days of S24 use |
| AI-057 | Model best-guess for missing fields, batched after scan (multiple choice) with verify, timeout, parser tests | F07 E10 | M | Planned | AI-010 (AI-004 closed without data: measure picks inside this workflow) |
| AI-060 | Real-time `RECEIVE_SMS` receiver into the same scanner. Each new bank or card SMS becomes a pending draft plus one quiet notification with the count. It never auto-saves | F07 E11 | S | Planned | AI-059, AI-079, AI-012 |

## Learning, audit, setup (F08, ordered)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-070 | SMS ↔ saved-transaction join engine (unique match, amount + ±1 day, any account) producing merchant→category and source→account labels + capture coverage figure | F08 L1 | M | Built | 2026-10-11. Pure `HistoryJoin` with 9 unit tests (`HistoryJoinTest`); read-only SMS pass in `LearnFromHistoryUseCase`. Coverage figure and label counts not yet recorded from the phone |
| AI-071 | "Learned from your history" confirm screen writing memory + links | F08 L2 | M | Built | 2026-10-11. Screen under the AI tile stack on TransactionsScreen (personal build only). ViewModel unit test not written; not seen on the phone |
| AI-072 | Merchant key normalisation (POS prefixes, UPI QR ids, truncation) with alias read | F08 L4 | S | Built | 2026-10-12. Pure `MerchantKey.canonical` (terminal ids, QR ids, city and store suffixes) used by memory, history and the join; memory reads fall back to the legacy key. 7 + 4 unit tests; not seen on the phone. Truncation stays prefix-matched in `MerchantHistoryIndex` |
| AI-073 | Deterministic audit checks A1–A8 as pure functions | F08 A1 | M | Built | 2026-10-12. `AuditChecks` + `AuditRanker` in `domain/audit`, 17 unit tests; inputs are plain `AuditTxn` rows. Not wired to data or UI yet (AI-074) |
| AI-074 | Audit worker (idle + charging) + "Check these" section + dismiss fingerprints in DataStore and JSON backup | F08 A2 | M | Built | 2026-10-12. `AuditRepository` + `AuditWorker` (daily, idle + charging, SMS build only), cached findings in DataStore, "Check these" + "Check now" at the top of the SMS inbox, "It's correct" stored and in the JSON settings block. Only A1 "Change to X" is a one-tap fix; A2–A8 are "It's correct" only. No hub tile. Balance (A6) not fed yet. 3 + 2 unit tests; not seen on the phone |
| AI-075 | Teach screen: top 30 unlabelled merchants with batched model suggestions | F08 L3 | M | Planned | AI-071, AI-057 |
| AI-076 | Correction-aware merchant memory | F08 L5 | S | Planned | AI-014 |
| AI-077 | Model plausibility judge for audit, shadow mode then live at ≥ 80% precision | F08 A3 | M | Planned | AI-074 |
| AI-078 | AI-assisted setup where the model generates categories, budgets and recurring rows | F08 O1–O5 | L | Discarded | 2026-10-13. Free-form generation breaks principle 2. Setup for the user's own data is AI-079; deterministic O1/O2/O4/O5 stay in F08 as Idea |

## Then (voice, image)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-020 | Voice: audit the existing `VoiceMemoScreen` -> `processText("VOICE")` flow on the S24 (offline recogniser quality, Indian-English amounts and names) | F02 | S | Planned | none |
| AI-021 | Voice: transcript normaliser (spoken numbers, "k", "lakh", relative dates, "yesterday") with unit tests | F02 | M | Planned | AI-011 |
| AI-022 | Voice: multi-transaction utterances ("tea 20 and auto 80") split to several drafts | F02 | M | Idea | AI-021 |
| AI-023 | Voice: confirm card with spoken-back summary; no auto-commit until AI-012 and measured zero-edit rate allow | F02 | M | Idea | AI-012, AI-014 |
| AI-030 | Image: benchmark on 30 real receipts: ML Kit text recognition then text path, versus current direct vision call | F03 | M | Planned | AI-015 |
| AI-031 | Image: receipt parser for total, date, merchant, tax; Verify against OCR text | F03 | L | Idea | AI-030, AI-011 |
| AI-032 | Image: UPI/payment-app screenshot parser (deterministic first) | F03 | M | Idea | AI-030 |
| AI-033 | Image: line-item split into split transactions (optional) | F03 | L | Idea | AI-031 |

## Later

| ID | Item | Feature | Notes |
|---|---|---|---|
| AI-041 | Notification-listener capture (bank/UPI app notifications) as a Play-safe alternative to SMS | F01, F04 | Policy review needed first |
| AI-042 | Optional inbox import in a non-Play build (FRD-08 Phase E) | F01 | |
| AI-043 | Natural-language query over transactions ("how much on food last month") | new | Needs its own feature file; model must pick a prepared query, not write SQL |
| AI-044 | Model/runtime upgrades behind `AiClientRouter` (bigger model on capable devices) | F04 | Re-run the golden fixtures before switching |
| AI-045 | EmbeddingGemma 2 replay: run the AI-057 / AI-075 category picks (AI-004 closed without logging) through embedding similarity (category names + labelled examples); switch only if accuracy is equal or better | F07, F08 | About 190 MB extra model; only worth it if it beats the model call on the same set |

## Discarded (2026-10-13)

| ID | Item | Why |
|---|---|---|
| AI-040 | Learned SMS templates where the model proposes a regex for an analyzer gap | Regex is free-form model output, which breaks principle 2 and the CLAUDE.md rule. A future version may let the model pick from app-built candidates; not planned |
| AI-078 | AI-assisted setup where the model generates categories, budgets and recurring rows | Same free-form problem. The need that matters (connecting SMS) is AI-079 |

## Done

| ID | Item | Feature | Verified |
|---|---|---|---|
| AI-D01 | SMS analyzer (debug only), masked export | F01 | Device |
| AI-D02 | Rule engine, 64 built-in rules, draft mapper, account matcher | F01 | Unit tests; corpus fit only |
| AI-D03 | `AiAgent` SMS wiring (reject, rule-first, AI fallback) | F01 | Unit tests |
| AI-D04 | `SmsVerifier`, learned account links, history + model category pick | F01 | Unit tests; not on device |
| AI-D06 | SMS work committed as logical commits (AI-001) | F01 | Git log |
| AI-D05 | Existing text/image draft use cases, `AiClientRouter`, model download | pre-existing | Earlier work |
| AI-002 | Rescan on the S24, review `verifyIssues`, tune `SmsVerifier`. 1250 txn SMS, rejects 16 → 2: the 14 direction conflicts were rules whose wording uses the opposite verb (NPS, PUC, RD, JioFiber, Motilal), so the check now runs on generic parses only. 2 left are `date_in_future` on `hdfc.upi-sent` (29 Dec 2025, likely a year-first date); the date is dropped safely. Masker also masks a glued title (`MR<NAME>`) | F01 | Device, 2026-10-11 |
| AI-003 | Closed 2026-10-11 by decision. Baseline from the AI-002 scan: ready 8%, account matched 66% (Utkarsh card, HDFC UPI unmatched), category from memory 8%. Missing categories are expected: memory learns over time from saves. Per-draft zero-edit rate waits for AI-014 | F01, F05 | Device scan only |
| AI-004 | Closed 2026-10-11 by decision. No pick-vs-final measurement and no debug log of picks. Category matching gets its own AI-assisted workflow (AI-075 teach screen, AI-057 batched best-guess); `withCategoryFromAi` stays as a reviewed suggestion meanwhile | F01 | Not measured |
