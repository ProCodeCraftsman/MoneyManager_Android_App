---
status: partial
phase: 41-ai-vision-draft-review
source: [41-VERIFICATION.md]
started: 2026-05-18T15:05:00Z
updated: 2026-05-18T15:05:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. Review banner visual presence and placement
expected: Amber tertiaryContainer strip with Warning icon and text "Some fields need your review — AI was not confident about highlighted values." renders between the source banner and the first form card when draft.needsReview == true
result: [pending]

### 2. Low-confidence errorContainer tint
expected: Fields with confidence == "low" show a visually distinct errorContainer background (red-tinted) compared to the normal primary.copy(alpha=0.08f) AI tint on medium/high confidence fields
result: [pending]

### 3. Banner absent on null/false path
expected: Manually opened AddEditTransactionDialog (no AI draft) shows no review banner and no errorContainer tints — existing behavior fully preserved
result: [pending]

### 4. Tint clears on field edit
expected: When user edits a low-confidence field, its errorContainer background disappears (field removed from aiSuggestedFields set), leaving the field with plain surface color
result: [pending]

## Summary

total: 4
passed: 0
issues: 0
pending: 4
skipped: 0
blocked: 0

## Gaps
