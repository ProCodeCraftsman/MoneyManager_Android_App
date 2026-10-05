# F04 Shared draft pipeline and policy

State: Planned (SMS-specific version exists inside `AiAgent`) | Packages: `domain/ai`, `domain/sms`, `data/repository`, `data/preferences` | Depends on: none (enables F02, F03, F06)

## Goal
Every capture source produces the same thing: a `TransactionDraft` that went through the same Verify, Resolve and Learn stages and obeys one commit policy. New sources add an adapter, not a new flow.

## Current state
`AiAgent.processText` branches on `sourceType == "SMS"` and holds the SMS pipeline inline (reject, rule engine, `SmsVerifier`, account links, memory, model category). Voice and image use older paths and do not get Verify or learned links. `autoCommit` is a flag chosen by the caller.

## Design
```
Source adapter (SMS | VOICE | IMAGE | TEXT)
   capture + normalise  ->  CapturedInput(text, source, sender?, attachment?, reference time)
Extract    source-specific deterministic parse (rules / extractor / OCR parser)
Verify     check each extracted field against the captured text; withdraw or flag (SmsVerifier pattern)
Resolve    account (learned links, names, default account), category (memory, history, model multiple choice)
Policy     review | auto-commit, from per-source thresholds and a Settings kill switch
Learn      on save: merchant memory, account links, edit tracking (F05)
```
- Each stage is a small class with a pure function where possible, unit-tested on the JVM.
- `AiAgent` becomes the orchestrator only (status events, cancellation, error mapping).
- The model is called through narrow, single-purpose choosers (like `SmsCategoryChooser`), each with a prompt builder, a strict parser and a timeout. No shared "do everything" prompt.
- Learned data (account links, merchant memory) gets one backup/export story (AI-013).

## Phases
| Phase | Scope | Exit check | Status |
|---|---|---|---|
| P1 | Extract stages from `AiAgent`, SMS behaviour unchanged (AI-010) | Existing SMS tests pass untouched | Not started |
| P2 | `CapturedInput` adapters (AI-011) | SMS and TEXT on adapters | Not started |
| P3 | Commit policy (AI-012) | VOICE hard-coding removed; policy unit-tested | Not started |
| P4 | Backup of learned data (AI-013) | Round-trip test on export/import | Not started |

## Acceptance
Behaviour of F01 unchanged after P1; adding a source needs only an adapter and its Verify rules; no `sourceType ==` branching inside stages.

## Risks and unknowns
Refactor risk while SMS is unverified on device: do P1 only after AI-002/003 give a baseline, so regressions are visible. Backup format change needs the data-fidelity checklist from memory.

## Decisions log
- 2026-10-05: keep model calls as separate narrow choosers; do not build a general tool-calling agent for E2B.
- 2026-10-05: refactor after the first device baseline, not before.
