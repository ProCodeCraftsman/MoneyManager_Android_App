# AI backlog

Ordered. Top of each section is next. Detail lives in `features/`. Status words are defined in `README.md`.
Last reviewed: 2026-10-05.

## Now (finish SMS on the real device)

| ID | Item | Feature | Size | Status | Notes |
|---|---|---|---|---|---|
| AI-001 | Commit the SMS work as logical commits (analyzer, rule engine, agent wiring, verify/links/chooser, docs). Exclude `.gradle/file-system.probe` and `.idea/deploymentTargetSelector.xml` | F01 | S | Planned | Waiting for the user's go-ahead |
| AI-002 | Reinstall debug build, rescan, review `verifyIssues` from the new export; tune `SmsVerifier` if rejects are not near zero | F01 | S | Planned | Needs the phone |
| AI-003 | Save ~20 real SMS drafts, rescan, record the ready / zero-edit rate as the first baseline | F01, F05 | S | Planned | Needs the phone |
| AI-004 | Check E2B answers to the category prompt on 20 real merchants; log pick vs user's final category | F01 | S | Planned | Decides whether `withCategoryFromAi` stays |
| AI-005 | Apply learned account links on the `ai_fallback` and AI-drafted paths | F01 | S | Planned | Known gap |

## Next (shared platform, so voice and image do not fork the logic)

| ID | Item | Feature | Size | Status | Depends |
|---|---|---|---|---|---|
| AI-010 | Extract `DraftPipeline` stages (Verify, ResolveAccount, ResolveCategory, Learn) out of `AiAgent` so every source calls the same code | F04 | M | Planned | AI-002 |
| AI-011 | `CapturedInput` adapter type per source (SMS, VOICE, IMAGE, TEXT) with source-specific normalise + verify | F04 | M | Planned | AI-010 |
| AI-012 | Auto-commit policy object (per source, confidence thresholds, kill switch in Settings); remove the hard-coded `autoCommit = sourceType == "VOICE"` in `AiDraftViewModel` | F04 | S | Planned | AI-010 |
| AI-013 | Include learned account links and SMS-related learned data in backup/export (JSON, CSV bundle, Drive) | F04 | M | Planned | none |
| AI-014 | Edit-tracking: record which fields the user changed before saving a draft (field names + source only, no values); show per-source zero-edit rate in a debug screen | F05 | M | Planned | AI-010 |
| AI-015 | Golden-fixture harness: scrubbed inputs with expected draft, run in unit tests, per source | F05 | M | Planned | none |
| AI-016 | Duplicate detection across sources (bank SMS vs card SMS vs voice vs receipt for one purchase) | F06 | L | Planned | AI-010 |

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

## Done

| ID | Item | Feature | Verified |
|---|---|---|---|
| AI-D01 | SMS analyzer (debug only), masked export | F01 | Device |
| AI-D02 | Rule engine, 64 built-in rules, draft mapper, account matcher | F01 | Unit tests; corpus fit only |
| AI-D03 | `AiAgent` SMS wiring (reject, rule-first, AI fallback) | F01 | Unit tests |
| AI-D04 | `SmsVerifier`, learned account links, history + model category pick | F01 | Unit tests; not on device |
| AI-D05 | Existing text/image draft use cases, `AiClientRouter`, model download | pre-existing | Earlier work |
