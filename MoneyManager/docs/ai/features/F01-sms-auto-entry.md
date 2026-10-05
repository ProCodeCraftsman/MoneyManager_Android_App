# F01 SMS auto-entry

State: Built (unit tests, debug+release compile); not Verified on device | Packages: `domain/sms`, `domain/ai`, `app/smsanalyzer`, `app/src/debug` | Depends on: none

Detailed rule-engine spec: `../../FRD-08-SMS-Parsing.md`. Analyzer runbook: `../../SMS-ANALYZER.md`. Per-sender coverage: `../../SMS-PATTERN-CATALOG.md`.

## Goal and non-goals
Goal: a message pasted or shared into the app becomes a draft that needs minimal editing to post. Non-goals: reading the inbox in the release build (Play policy), auto-saving without review (see F04 policy).

## Current state
Pipeline: normalise -> rule engine (sender-scoped, sender-agnostic, generic) -> reject non-transactions -> **Verify** -> **Resolve account** (learned links, then name match) -> **Resolve category** (merchant memory, history, model multiple choice) -> user edits -> **Learn** (merchant memory, account link).
Key files: `SmsRuleEngine`, `BuiltInSmsRules` (64 rules), `SmsVerifier`, `SmsAccountLinks`, `SmsCategoryPrompt`, `SmsCategoryChooser`, `SmsDraftMapper`, `AiAgent.processText`.
Dry-run on the real inbox before the redesign: 0 of 1,228 drafts ready (account matched 67%, category 0%).

## Design (decisions that matter)
- Model picks from a numbered list of the user's own categories only; account is never given to the model (it cannot know which account is "HDFC").
- Verify withdraws a parse whose amount is not in the text; direction conflicts only flag.
- Learned links key: `institution|INSTRUMENT|last4`; stored in DataStore, not in backups.

## Phases
| Phase | Scope | Exit check | Status |
|---|---|---|---|
| A | Debug analyzer, masked export | Export from the S24 | Done |
| B | Rule engine and rules | Corpus 100% (fit data) | Done |
| C | Agent wiring | Flow tests | Done |
| C2 | Verify, links, history + model category | Unit tests | Built |
| C3 | Device verification: AI-002..004 | Verify rejects near 0; ready rate reported; E2B picks reviewed | Not started |
| C4 | Links on AI paths (AI-005) | Test + device | Not started |
| D | Learned templates (AI-040) | Proposed rule passes its own sample and the user's approval | Not started |
| E | Inbox import, non-Play build (AI-042) | Policy decision first | Not started |

## Acceptance
- Zero-edit rate on saved SMS drafts reported per rule-matched vs AI-assisted (needs AI-014).
- Verify reject rate on a fresh scan under 1% of rule matches (otherwise tune, do not disable).
- No raw SMS stored or exported anywhere.

## Risks and unknowns
- Rules were fit to masked templates; real-message edge cases will appear.
- E2B category quality unmeasured.
- Verify false positives on real data unknown.
- Bank SMS and card SMS for one purchase create two drafts (F06).

## Unverified on device
Everything except the analyzer scan itself. Named regex groups need API 26 (minSdk is 26).

## Decisions log
- 2026-10: model limited to multiple choice because free-form JSON from E2B is unreliable.
- 2026-10: account choice by learned links, not by the model.
- 2026-10: card repayment SMS maps to transfer; source account left to the user.
