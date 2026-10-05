# F05 Quality measurement and evals

State: Planned | Packages: `app/smsanalyzer`, `domain/ai`, test sources | Depends on: none for fixtures, F04 for edit tracking

## Goal
Know, with numbers, whether each source and each model step is good enough, and catch regressions when formats, prompts or models change.

## Current state
SMS analyzer reports template coverage and dry-run draft quality (account/category matched, `verifyIssues`) from the debug build. The rule-engine corpus result (100%) is on the same data the rules were fit to, so it is a regression guard, not an accuracy measure. No edit tracking; no fixtures for voice or image; no model-answer checks.

## Design
1. **Golden fixtures** per source: scrubbed input plus expected draft fields, in `src/test/resources`. Table-driven tests. Real-derived fixtures must be scrubbed (names, numbers, amounts).
2. **Zero-edit rate** (the product metric): when a draft is saved, record source, which fields changed, and whether a model step contributed (flags). Store counts only, locally, no values. Debug screen shows rates per source and per flag (`ai_category`, `ai_fallback`, `verify:*`).
3. **Model checks**: for each chooser, a fixed list of merchants with expected category groups, run manually on the device; record pick, final user category and latency. Re-run when the model, prompt or runtime changes.
4. **Drift watch**: the analyzer's `verifyIssues` and unmatched-template counts compared between scans.

## Phases
| Phase | Scope | Status |
|---|---|---|
| Q1 | Baseline from first device scan (AI-002/003) | Not started |
| Q2 | Fixture harness (AI-015) | Not started |
| Q3 | Edit tracking + debug screen (AI-014) | Not started |
| Q4 | Model check list and a stored result table for E2B (AI-004) | Not started |

## Acceptance
A number exists for each source before its auto-commit policy is considered; model/prompt changes are compared against the stored result table.

## Risks and unknowns
Privacy of any stored telemetry (counts only); small personal data set means noisy rates, so report counts with the rate.
