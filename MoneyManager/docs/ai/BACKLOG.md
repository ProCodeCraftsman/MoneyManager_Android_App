# AI backlog

Ordered. Top of each section is next. Detail lives in `features/`. Status words are defined in `README.md`.
Last reviewed: 2026-10-11.

## Execution order (across sections)

1. Done 2026-10-11: AI-006 built, AI-002 verified, AI-003/004 closed, v19 migration seen on the phone.
2. AI-010 (pipeline stages): the scanner needs "SMS in, draft out" without the UI flow.
3. AI-053 scanner, then AI-070 + AI-071 bootstrap (fills memory before the first inbox appears; also measures capture coverage).
4. AI-054 dedup, AI-055 Account Sources, AI-056 hub + inbox, AI-014 edit tracking.
5. AI-058 banner + scan on open, AI-059 reminders.
6. AI-072 normalisation, AI-073 + AI-074 audit.
7. AI-057 model best-guess, AI-075 teach screen, AI-076 correction-aware memory, AI-077 model judge (shadow first).
8. Later: AI-060 receiver, AI-045 EmbeddingGemma replay, AI-078 setup flow.

## Now (finish SMS on the real device)

| ID | Item | Feature | Size | Status | Notes |
|---|---|---|---|---|---|
| AI-006 | Stop `AiAgent` writing the model's category into merchant memory before the user saves (text and image paths); memory learns from saves only | F07, F08 | S | Built | 2026-10-11. Unit tests in `AiAgentSmsFlowTest` (AI text draft, rule draft with model pick, image draft); confirmed they fail with the old code. Not seen on the phone. Side effect: `quickAdd` auto-commits no longer teach memory (see AI-012) |
| AI-005 | Apply learned account links on the `ai_fallback` and AI-drafted paths | F01 | S | Built | Unit tests only (`SmsDraftMapperTest`); not seen on the phone. Also covers the merchant-cache fast path |

## Next (shared platform, so voice and image do not fork the logic)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-010 | Extract `DraftPipeline` stages (Verify, ResolveAccount, ResolveCategory, Learn) out of `AiAgent` so every source calls the same code | F04 | M | Built | AI-002. Part 1 done: `SmsDraftPipeline.fromRules` (rule match, verify, account links, memory category) is model-free and takes the account list, so the scanner can call it. Unit tests only (`SmsDraftPipelineTest`, `AiAgentSmsFlowTest` unchanged). Not seen on the phone. Still open: ResolveAccount/Category as named stages, Learn |
| AI-011 | `CapturedInput` adapter type per source (SMS, VOICE, IMAGE, TEXT) with source-specific normalise + verify | F04 | M | Planned | AI-010 |
| AI-012 | Auto-commit policy object (per source, confidence thresholds, kill switch in Settings); remove the hard-coded `autoCommit = sourceType == "VOICE"` in `AiDraftViewModel` | F04 | S | Planned | AI-010 |
| AI-013 | Include learned account links and SMS-related learned data in backup/export (JSON, CSV bundle, Drive) | F04 | M | Built | Done inside AI-051 (links, merchant memory, ledger); unit tests only |
| AI-014 | Edit-tracking: record which fields the user changed before saving a draft (field names + source only, no values); show per-source zero-edit rate in a debug screen | F05 | M | Planned | AI-010 |
| AI-015 | Golden-fixture harness: scrubbed inputs with expected draft, run in unit tests, per source | F05 | M | Planned | none |
| AI-016 | Duplicate detection across sources (bank SMS vs card SMS vs voice vs receipt for one purchase) | F06 | L | Planned | AI-010 |

## Auto-capture (F07, ordered)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-050 | In-app JSON backup taken and stored off-device; then aggregate-only profile of it (accounts by type, sources, txns per month, duplicate-ish pairs; no names printed) | F07 | S | Verified | Done 2026-10-05; numbers in F07 "Data profile" |
| AI-051 | `captured_messages` ledger + draft table, migration v19, backup/CSV/Drive formats (includes AI-013) | F07 | L | Built | v18→v19 migration ran on the S24 over real data on 2026-10-11 (app opens, data intact). Restore of a v19 backup not yet run on the phone |
| AI-052 | Share-sheet text target into the draft flow | F07 | S | Planned | none |
| AI-053 | `personal` flavor (READ_SMS) + inbox scanner: cursor window with 2 h overlap, start date, anchor fallback, 31-day/500-message cap, rules only, writes pending ledger rows; check merged release manifest | F07 E4 | M | Built | 2026-10-11. 9 unit tests (`InboxScannerTest`: planner, drafts, ledger skip, non-transactions, overlap, cap, anchor). Play release manifest has no SMS permission (checked). Not run on the phone: no trigger until AI-058 |
| AI-054 | Semantic dedup: ref number, amount+account+window, transfer pairing, recurring/EMI/manual; uncertain matches shown as merge/keep-both | F07 E5, F06 | M | Planned | AI-053 |
| AI-055 | Account Sources screen (map or create account) + balance cross-check | F07 E6 | M | Planned | AI-051 |
| AI-056 | Capture hub screen replacing the AI dropdown + draft inbox: bulk approve of deterministic rows only, inline category fix, reject / always-ignore, undo | F07 E7 | L | Planned | AI-053, AI-054 |
| AI-058 | Scan on app open + home banner (dismiss today / don't show) | F07 E8 | S | Planned | AI-056 |
| AI-059 | Noon / evening reminder worker: inexact alarm, scan first, notify only if count > 0, reboot reschedule, notification permission, battery tip | F07 E9 | M | Planned | AI-058 |
| AI-057 | Model best-guess for missing fields, batched after scan (multiple choice) with verify, timeout, parser tests | F07 E10 | M | Planned | AI-010 (AI-004 closed without data: measure picks inside this workflow) |
| AI-060 | Real-time RECEIVE_SMS receiver into the same scanner | F07 E11 | S | Idea | AI-059 |

## Learning, audit, setup (F08, ordered)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-070 | SMS ↔ saved-transaction join engine (unique match, amount + ±1 day, any account) producing merchant→category and source→account labels + capture coverage figure | F08 L1 | M | Planned | AI-053 |
| AI-071 | "Learned from your history" confirm screen writing memory + links | F08 L2 | M | Planned | AI-070 |
| AI-072 | Merchant key normalisation (POS prefixes, UPI QR ids, truncation) with alias read | F08 L4 | S | Planned | none |
| AI-073 | Deterministic audit checks A1–A8 as pure functions | F08 A1 | M | Planned | AI-072 |
| AI-074 | Audit worker (idle + charging) + "Check these" section + dismiss fingerprints in DataStore and JSON backup | F08 A2 | M | Planned | AI-073, AI-056 |
| AI-075 | Teach screen: top 30 unlabelled merchants with batched model suggestions | F08 L3 | M | Planned | AI-071, AI-057 |
| AI-076 | Correction-aware merchant memory | F08 L5 | S | Planned | AI-014 |
| AI-077 | Model plausibility judge for audit, shadow mode then live at ≥ 80% precision | F08 A3 | M | Planned | AI-074 |
| AI-078 | AI-assisted setup: accounts, category set, recurring, budgets from history | F08 O1–O5 | L | Idea | Only if the app gets other users |

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
| AI-040 | Learned SMS templates (FRD-08 Phase D): model proposes a regex for an analyzer gap, user approves, stored as JSON next to built-ins | F01 | Needs a safe regex sandbox and a test-on-sample step |
| AI-041 | Notification-listener capture (bank/UPI app notifications) as a Play-safe alternative to SMS | F01, F04 | Policy review needed first |
| AI-042 | Optional inbox import in a non-Play build (FRD-08 Phase E) | F01 | |
| AI-043 | Natural-language query over transactions ("how much on food last month") | new | Needs its own feature file; model must pick a prepared query, not write SQL |
| AI-044 | Model/runtime upgrades behind `AiClientRouter` (bigger model on capable devices) | F04 | Re-run the golden fixtures before switching |
| AI-045 | EmbeddingGemma 2 replay: run the AI-057 / AI-075 category picks (AI-004 closed without logging) through embedding similarity (category names + labelled examples); switch only if accuracy is equal or better | F07, F08 | About 190 MB extra model; only worth it if it beats the model call on the same set |

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
