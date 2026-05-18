---
phase: 41-ai-vision-draft-review
plan: "01"
subsystem: ui-dialogs
tags: [tdd, ai-draft, confidence, review-banner, field-tint, compose]
dependency_graph:
  requires:
    - 36-04 (aiSuggestedFields + primary alpha tint pattern in AddEditTransactionDialog)
    - 32-xx (TransactionDraft domain model with confidence/needsReview/flags fields)
  provides:
    - showReviewBanner(draft): Boolean — pure predicate for review banner visibility
    - fieldIsLowConfidence(field, draft): Boolean — pure predicate for errorContainer tint
    - Review banner composable (tertiaryContainer strip with Warning icon)
    - Per-field errorContainer tint for low-confidence AI draft fields
  affects:
    - AddEditTransactionDialog.kt (all AI-tinted fields now have two tint tiers)
tech_stack:
  added: []
  patterns:
    - Two-tier AI field tinting: errorContainer (low confidence) > primary.copy(0.08f) (normal AI) > surface
    - Review banner: amber tertiaryContainer strip, purely informational, never gates save action
key_files:
  created:
    - MoneyManager/app/src/test/java/com/moneymanager/ui/dialog/ConfidenceBannerStateTest.kt
  modified:
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt
decisions:
  - "Pure predicate functions (showReviewBanner, fieldIsLowConfidence) added as internal top-level functions in AddEditTransactionDialog.kt for direct testability without Compose instrumentation"
  - "Test imports functions from com.moneymanager.app.ui.dialogs package — consistent with the dialog's actual package, no separate helper file needed"
  - "Low-confidence tint applied at the BadgedBox wrapper level, same as existing AI tint — avoids duplication and keeps tint logic co-located with the badge indicator"
  - "Tags field not updated with low-confidence tint: confidence map uses 'amount', 'date', 'typeId', 'categoryName', 'accountName', 'peerContactName', 'description'/'note' — no 'tags' key"
  - "amount/date/account card uses a composite low-confidence check (any of the three sub-fields is low-confidence) to determine errorContainer at the combined card level"
metrics:
  duration_minutes: 18
  completed_date: "2026-05-18"
  tasks_completed: 2
  tasks_total: 2
  files_created: 1
  files_modified: 1
---

# Phase 41 Plan 01: Confidence Banner and Per-Field Error Tint Summary

**One-liner:** Additive confidence review banner (tertiaryContainer + Warning icon) and two-tier field tinting (errorContainer for low-confidence, primary alpha for normal AI) in AddEditTransactionDialog, delivering AGENT-01 and AGENT-02.

## Tasks Completed

| Task | Name | Commit | Type | Files |
|------|------|--------|------|-------|
| 1 | ConfidenceBannerStateTest — failing tests | ea6439c | test (RED) | ConfidenceBannerStateTest.kt |
| 2 | Extend AddEditTransactionDialog | 5158006 | feat (GREEN) | AddEditTransactionDialog.kt + test update |

## Implementation Details

### Part A — Pure Predicate Functions

Added as `internal` top-level functions in `AddEditTransactionDialog.kt` (package `com.moneymanager.app.ui.dialogs`):

```kotlin
internal fun showReviewBanner(draft: TransactionDraft?): Boolean = draft?.needsReview == true

internal fun fieldIsLowConfidence(fieldName: String, draft: TransactionDraft?): Boolean =
    draft?.confidence?.get(fieldName) == "low"
```

These are directly importable by unit tests without Compose instrumentation.

### Part B — Per-Field Error Tint

Updated the following BadgedBox wrapper tint logic from single-tier to two-tier:
- **type field** — checks `fieldIsLowConfidence("typeId", initialDraft)`
- **amount/date/account combined card** — checks any of amount/date/accountName low confidence
- **category field** — checks `fieldIsLowConfidence("categoryName", initialDraft)`
- **peer field** — checks `fieldIsLowConfidence("peerContactName", initialDraft)`
- **note field** — checks description or note low confidence

Pattern applied in each location:
```kotlin
when {
    fieldIsLowConfidence(key, initialDraft) && isFieldAiField -> colorScheme.errorContainer
    isFieldAiField -> colorScheme.primary.copy(alpha = 0.08f)
    else -> null  // no background modifier applied
}
```

### Part C — Review Banner

Placed after the existing Source Banner and before form field 1:

```
[Source Banner: "Draft from vision · ..." ]   ← existing, unchanged
[Review Banner: amber strip + Warning icon ]  ← NEW, shown when needsReview == true
[ Amount / Date / Account Card              ]
```

Banner uses `tertiaryContainer` background (amber in Material 3 light theme), `onTertiaryContainer` icon/text tint, `RoundedCornerShape(8.dp)`, and `Warning` icon at 16.dp.

## Verification Results

| Check | Result |
|-------|--------|
| grep -c "needsReview" AddEditTransactionDialog.kt | 1 |
| grep -c "errorContainer" AddEditTransactionDialog.kt | 7 |
| grep -c "tertiaryContainer" AddEditTransactionDialog.kt | 5 |
| ./gradlew :app:testDebugUnitTest --tests *.ConfidenceBannerStateTest | BUILD SUCCESSFUL (6/6 pass) |
| ./gradlew :app:compileDebugKotlin | BUILD SUCCESSFUL |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Test import for predicate functions**
- **Found during:** Task 2 (GREEN)
- **Issue:** ConfidenceBannerStateTest.kt was written to call `showReviewBanner`/`fieldIsLowConfidence` without imports, expecting them to be in the same package (`com.moneymanager.ui.dialog`). The functions were added to `AddEditTransactionDialog.kt` in `com.moneymanager.app.ui.dialogs`.
- **Fix:** Added explicit imports `import com.moneymanager.app.ui.dialogs.showReviewBanner` and `import com.moneymanager.app.ui.dialogs.fieldIsLowConfidence` to the test file.
- **Files modified:** ConfidenceBannerStateTest.kt
- **Commit:** 5158006

## Known Stubs

None — all logic wired to `initialDraft.needsReview` and `initialDraft.confidence` which are populated by the existing vision/ask-image pipeline (Phase 35 work).

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes introduced. The two predicate functions read from `TransactionDraft.confidence` (LLM-controlled string). Unknown values fall through to safe defaults per T-41-01.

## Self-Check: PASSED

- ConfidenceBannerStateTest.kt: FOUND (MoneyManager/app/src/test/java/com/moneymanager/ui/dialog/)
- AddEditTransactionDialog.kt: FOUND (MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/)
- Commit ea6439c: FOUND (test RED)
- Commit 5158006: FOUND (feat GREEN)
