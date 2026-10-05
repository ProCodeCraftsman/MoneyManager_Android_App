# F03 Image and receipt entry

State: Planned (a vision flow already exists) | Packages: `domain/ai` (`GenerateDraftFromImageUseCase`), `data/ai`, new `domain/receipt` | Depends on: F04, F05 (AI-015)

## Goal and non-goals
Goal: attach a receipt or payment screenshot and get a draft with total, date, merchant and category suggested. Non-goals: full line-item accounting at first, cloud OCR.

## Current state
`GenerateDraftFromImageUseCase` sends raw image bytes to the model through `GenAiClient.generateDraftFromImage` with a system instruction and the tool set, then parses and validates JSON (no OCR step). See memory note "AI Image Feature" for the file list. This relies on the model reading the image and returning structured JSON in one shot, the pattern least suited to a 2 GB on-device model. Not benchmarked.

## Design options (decide at AI-030, not before)
| Option | Pros | Cons |
|---|---|---|
| A. Vision model reads the image (current) | One step, no OCR dependency | Slow, hard to verify, free-form output |
| B. ML Kit text recognition, then the text pipeline | Verifiable against the OCR text, reuses F01/F04, fast, deterministic parsing of totals and dates | OCR errors on crumpled receipts; layout lost |
| C. B first, A only as fallback when B finds no total | Best coverage | Two paths to maintain |
Leaning C. The Verify step requires text, so B is the base.

## Phases
| Phase | Scope | Exit check | Status |
|---|---|---|---|
| I0 | Benchmark on 30 real receipts: A vs B (AI-030) | Table of amount/date/merchant accuracy and latency | Not started |
| I1 | Payment-app screenshot parser, deterministic (AI-032) | Fixtures pass | Not started |
| I2 | Receipt parser: total, date, merchant, tax; Verify against OCR (AI-031) | Zero-edit rate reported | Not started |
| I3 | Line items to split transactions (AI-033) | Optional | Not started |

## Acceptance
Total correct on at least 90% of legible receipts; draft shows the attached image; image stays app-private; fixtures use synthetic or scrubbed images.

## Risks and unknowns
Receipt formats vary widely; multi-page or long receipts; GST lines vs total; model latency and memory on the vision path; fixture privacy (receipts carry names and card digits).

## Unverified on device
All of it.

## Decisions log
- 2026-10-05: do not extend the current vision path until the benchmark says it beats OCR + text.
