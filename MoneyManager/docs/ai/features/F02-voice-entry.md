# F02 Voice entry

State: Planned (a basic flow already exists) | Packages: `app/ui/aidraft`, `domain/ai`, new `domain/voice` | Depends on: F04 (AI-010, AI-011), F05

## Goal and non-goals
Goal: say "spent 250 on lunch at Swiggy yesterday" and get a draft that needs minimal editing. Non-goals: a conversational assistant, cloud speech, storing audio.

## Current state
`VoiceMemoScreen` uses Android `SpeechRecognizer` (offline preferred) to get text, then `AiDraftViewModel.generateDraft(text, "VOICE")` -> `AiAgent.processText`. The view model currently sets `autoCommit = sourceType == "VOICE"` as an example; it needs a real policy (AI-012). Voice text goes through the generic extractor and the model with the free-form JSON path, which is the path found least reliable on E2B. Not measured on the S24 yet.

## Design
- The recogniser is the capture step; its output is untrusted text.
- **Normalise** (deterministic, `domain/voice`): spoken numbers ("two fifty", "1.5k", "2 lakh"), relative dates ("yesterday", "last Friday"), currency words, filler words.
- **Extract** with the deterministic extractor first; the model only fills gaps by multiple choice (type, category), reusing F04 stages.
- **Verify**: amount and date must be derivable from the normalised transcript, same idea as `SmsVerifier`.
- **Resolve** account: spoken account name against the user's account names and aliases; otherwise the default account for the type (existing default-accounts feature).
- Show the transcript on the draft screen so the user sees what was heard.

## Phases
| Phase | Scope | Exit check | Status |
|---|---|---|---|
| V0 | Audit the existing flow on the S24 (AI-020) | 20 spoken samples, results written down | Not started |
| V1 | Normaliser + unit tests (AI-021) | Table-driven tests incl. Indian-English forms | Not started |
| V2 | Route through shared pipeline stages (needs AI-010/011) | Same Verify/Resolve/Learn as SMS | Not started |
| V3 | Multi-transaction utterances (AI-022) | Split drafts shown as a list | Not started |
| V4 | Confirmation + gated auto-commit (AI-023) | Zero-edit rate over threshold for 2 weeks | Not started |

## Acceptance
Zero-edit rate reported for voice; amount correct on at least 95% of clearly spoken samples; no audio stored.

## Risks and unknowns
Offline recogniser quality for Indian names and "lakh/crore"; recogniser availability varies by device; background noise; privacy expectations around the microphone.

## Unverified on device
All of it.

## Decisions log
- 2026-10-05: voice is a capture adapter on the shared pipeline, not its own AI flow.
