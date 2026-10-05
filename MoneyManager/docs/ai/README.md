# On-device AI workstream

How the AI features of MoneyManager are planned, built and tracked. Start here, then read `BACKLOG.md` (what is next) and the feature file you are working on (`features/`).

## Files

| File | Purpose | Changes |
|---|---|---|
| `BACKLOG.md` | Single ordered list of next items across all AI features. Short rows, no design detail | Often |
| `features/F0x-*.md` | One per feature: goal, current state, design, phases, acceptance, risks, unverified items | When design or state changes |
| `../FRD-08-SMS-Parsing.md` | Detailed spec of the SMS rule engine (older, stays as the SMS reference) | When SMS behaviour changes |

Rule of thumb: a **decision** goes in the feature file, a **to-do** goes in the backlog, a **status** lives in both and must agree. If they disagree, the feature file's "State" line wins; fix the backlog.

## Target device and model (design constraint)

Gemma 4 E2B-it (2.4 GB, LiteRT-LM) on a Galaxy S24 Ultra. Other devices fall back through `AiClientRouter` (ML Kit GenAI / none). Everything must still work with **no model at all**; AI only improves a draft that already exists.

## Principles

1. **Deterministic first, model last.** Order of trust: exact rules and learned data, then history, then the model. Each stage runs only if the earlier one left a gap.
2. **The model chooses, the app writes.** The small model never produces ids, amounts, dates or free text that go into a draft. It picks one option from a short list the app built (one-number answers). Free-form JSON and tool calls are unreliable at this size; do not add new features that depend on them.
3. **Verify every deterministic result against the source.** A rule that matches but captures the wrong thing is the main failure when formats drift. `SmsVerifier` is the pattern; other sources need their equivalent.
4. **Fail safe.** Any doubt, timeout or missing model gives "no suggestion", never a guess. A model-influenced field sets `needsReview` and is flagged (`ai_category`, `ai_fallback`, ...).
5. **Learn from the user's saves, not from the model.** What the user keeps or edits is the ground truth (merchant memory, account links). Learned data is local, small and inspectable.
6. **One pipeline, many sources.** SMS, voice, image, pasted text and any future source differ only in the capture/normalise step. Verify, resolve and learn are shared (see F04).
7. **Drafts, not entries.** Nothing is saved without the user unless an explicit, per-source, confidence-gated policy allows it (F04). Default is review.
8. **Measure on real data without storing it.** Quality is tracked as the share of drafts saved with zero edits, per source. Raw SMS, audio, receipts and names never leave the device or enter fixtures unscrubbed.

## Working agreement

- Plan before code for anything new: add or update the feature file, add backlog rows, then build.
- One backlog item = one logical change = one commit (conventional prefix, see root `CLAUDE.md`). Commit only when asked.
- Every item that changes behaviour updates its feature file and, if SMS, the FRD.
- Every item states how it is verified. "Unit tests pass" and "checked on the S24" are different lines in the item's notes; never merge them.
- Items touching Room need a migration, version bump and backup/export update. Prefer DataStore for small learned maps, but record it as a backup gap if it is not exported.
- New model prompts get a unit test for the reply parser (clean, empty, prose, contradictory replies) and a timeout.
- Privacy gates are hard: `READ_SMS` debug only; no raw message/audio/image persistence by dev tooling; scrub fixtures.

## Status vocabulary

`Idea` (one line, no design) -> `Planned` (feature file has design + phases) -> `In progress` -> `Built` (code + unit tests, not seen on device) -> `Verified` (seen working on the S24 with real data) -> `Shipped`.

## Feature file template

```
# F0x Title
State: <status> | Owner area: <packages> | Depends on: <ids>
## Goal and non-goals
## Current state            (what exists in code now, with paths)
## Design                   (pipeline, model task if any, data stored)
## Phases                   (table: phase, scope, exit check, status)
## Acceptance               (measurable)
## Risks and unknowns
## Unverified on device
## Decisions log            (date, decision, why)
```
