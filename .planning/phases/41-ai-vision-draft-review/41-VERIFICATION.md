---
phase: 41-ai-vision-draft-review
verified: 2026-05-18T15:00:00Z
status: human_needed
score: 8/8
overrides_applied: 0
human_verification:
  - test: "Open AddEditTransactionDialog with a TransactionDraft where needsReview=true"
    expected: "Amber tertiaryContainer banner with Warning icon and text 'Some fields need your review — AI was not confident about highlighted values.' appears between the source banner and the Amount/Date/Account card"
    why_human: "Compose UI rendering cannot be verified by grep — visual placement and amber color only verifiable on device or instrumented test"
  - test: "Open the dialog with a draft where confidence[amount]='low' and amount is in aiSuggestedFields"
    expected: "Amount/Date/Account card has errorContainer background (visually red/error tinted), not the normal primary alpha tint"
    why_human: "Two-tier tint distinction (errorContainer vs primary.copy(0.08f)) requires visual inspection or instrumented Compose test"
  - test: "Open the dialog with a TransactionDraft where needsReview=false (or null initialDraft)"
    expected: "No amber banner appears; form looks identical to a manually-opened dialog"
    why_human: "Banner absence on null/false path requires visual or instrumented verification"
  - test: "Edit a low-confidence amount field by tapping a numpad key"
    expected: "errorContainer tint clears from the Amount card (aiSuggestedFields removes 'amount')"
    why_human: "Dynamic tint-clear on edit requires UI interaction"
---

# Phase 41: AI Vision Draft Review — Verification Report

**Phase Goal:** Extend AddEditTransactionDialog to surface low-confidence AI draft fields with a distinct error tint and a top-level review banner — delivers AGENT-01 and AGENT-02.
**Verified:** 2026-05-18T15:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | "Review suggested" banner (amber/warning surface) appears at the top of AddEditTransactionDialog when initialDraft.needsReview == true | VERIFIED | `if (showReviewBanner(initialDraft))` at line 576; banner uses `tertiaryContainer` background + `Icons.Default.Warning` icon with exact plan text |
| 2 | Banner is absent when initialDraft.needsReview == false or initialDraft is null | VERIFIED | `showReviewBanner(null) = false` and `showReviewBanner(draft(needsReview=false)) = false` — the `if` guard makes this guaranteed by the predicate logic |
| 3 | Amount field container tinted errorContainer when initialDraft.confidence["amount"] == "low" | VERIFIED | Lines 606-624: composite low-confidence check for amount/date/accountName drives `errorContainer` at the Amount/Date/Account card level |
| 4 | Type field container tinted when initialDraft.confidence["typeId"] == "low" | VERIFIED | Lines 515-521: `fieldIsLowConfidence("typeId", initialDraft) && isTypeAiField -> errorContainer` |
| 5 | Date field container tinted when initialDraft.confidence["date"] == "low" | VERIFIED | Line 607: `fieldIsLowConfidence("date", initialDraft) && "date" in aiSuggestedFields` included in composite check driving errorContainer at lines 622-624 |
| 6 | Fields with confidence "high" or "medium" use existing AI tint (primary.copy(alpha=0.08f)) — not error tint | VERIFIED | All `when` blocks follow pattern: low+aiField → errorContainer; aiField only → primary.copy(alpha=0.08f); else → null. `fieldIsLowConfidence` returns false for "high"/"medium" per its implementation |
| 7 | Editing a low-confidence field clears only its tint (same as existing aiSuggestedFields behaviour) | VERIFIED | 13 occurrences of `aiSuggestedFields = aiSuggestedFields - "fieldName"` in edit handlers (lines 274, 430, 455, 663, 708, 755, 899, 943, 945, 966, 978, 979, 981, 1023). Since tint is conditional on `fieldName in aiSuggestedFields`, removing the field clears the tint automatically |
| 8 | All changes to AddEditTransactionDialog are strictly additive — null initialDraft path is identical to current behaviour | VERIFIED | `initialDraft: TransactionDraft? = null` default at line 109; `aiSuggestedFields` starts as `emptySet()` (line 115); all tint conditions require both low-confidence AND field-in-aiSuggestedFields — with empty set, all `else -> null` branches are taken, producing zero background modifier |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt` | Confidence review banner + per-field error tint for low-confidence AI drafts; contains "needsReview" | VERIFIED | File exists; `needsReview` referenced at lines 70 and 576; `errorContainer` at 7 locations; `tertiaryContainer` at 5 locations (2 pre-existing for savings accent, 3 new for banner) |
| `MoneyManager/app/src/test/java/com/moneymanager/ui/dialog/ConfidenceBannerStateTest.kt` | Unit tests for AGENT-01 and AGENT-02 banner/tint visibility logic | VERIFIED | File exists with 6 tests covering showReviewBanner (3) and fieldIsLowConfidence (3); imports from `com.moneymanager.app.ui.dialogs` package confirmed |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| TransactionDraft.needsReview | AddEditTransactionDialog review banner visibility | `initialDraft?.needsReview == true` | WIRED | `showReviewBanner(initialDraft)` at line 576 calls the predicate which is `draft?.needsReview == true`; TransactionDraft.needsReview field exists at line 25 of TransactionDraft.kt |
| TransactionDraft.confidence[field] | field container color | if confidence == "low" → errorContainer else if in aiSuggestedFields → primary.copy(0.08f) else null | WIRED | `fieldIsLowConfidence(fieldName, initialDraft)` used at 5 distinct field locations (typeId, amount/date/accountName composite, categoryName, peerContactName, description/note); each correctly drives the three-branch `when` |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| AddEditTransactionDialog.kt | `initialDraft` | Passed as parameter from AiDraftViewModel / navigation (established in Phase 36) | Yes — populated by Phase 35/36 vision/ask-image pipeline | FLOWING |
| AddEditTransactionDialog.kt | `aiSuggestedFields` | Populated via `LaunchedEffect(initialDraft)` from Phase 36-03/04 work | Yes — set contains field names from draft at dialog open | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| `showReviewBanner(TransactionDraft(needsReview=true))` returns true | Covered by ConfidenceBannerStateTest test 1 | Pass — test exists and runs GREEN per commit 5158006 | PASS |
| `fieldIsLowConfidence("amount", TransactionDraft(confidence=mapOf("amount" to "low")))` returns true | Covered by ConfidenceBannerStateTest test 4 | Pass — test exists and runs GREEN per commit 5158006 | PASS |
| `fieldIsLowConfidence("amount", null)` returns false | Covered by ConfidenceBannerStateTest test 6 | Pass — null safety verified by predicate `draft?.confidence?.get(fieldName) == "low"` | PASS |
| Build compiles with new code | `./gradlew :app:compileDebugKotlin` | SUMMARY reports BUILD SUCCESSFUL; commit 5158006 adds 102 lines with no syntax errors visible in source | PASS |

### Probe Execution

No probe scripts declared or present for this phase. Step 7c: SKIPPED (no probe files found).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| AGENT-01 | 41-01-PLAN.md | Review banner in AddEditTransactionDialog when draft.needsReview == true | SATISFIED | Banner implemented at lines 576-601 of AddEditTransactionDialog.kt |
| AGENT-02 | 41-01-PLAN.md | Per-field errorContainer tint for low-confidence AI draft fields | SATISFIED | errorContainer tint applied at 5 field locations (typeId, amount/date/accountName, categoryName, peerContactName, description/note) |

**NOTE — Traceability gap (WARNING):** AGENT-01 and AGENT-02 are declared in the PLAN frontmatter and referenced in ROADMAP.md Phase 41, but neither requirement ID appears in REQUIREMENTS.md. The v3.2 requirement set (AGENT-01 through AGENT-09) is listed in ROADMAP.md under the milestone summary count ("10 total") but no v3.2 section exists in REQUIREMENTS.md. The ROADMAP.md success criteria for Phase 41 directly describe the same intent as AGENT-01/AGENT-02, so the implementation satisfies the roadmap contract. The missing REQUIREMENTS.md entries are a documentation gap, not an implementation gap.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| No anti-patterns found | — | — | — | — |

No `TODO`, `FIXME`, `TBD`, `XXX`, `HACK`, or `PLACEHOLDER` markers found in either modified file. No empty return stubs or hardcoded empty props detected.

### Human Verification Required

#### 1. Review Banner — Visual Presence and Placement

**Test:** Open `AddEditTransactionDialog` on a device or emulator by navigating through the vision/ask-image path, and construct a `TransactionDraft` with `needsReview = true` (e.g. via the existing ask-image flow or a debug shortcut).
**Expected:** An amber-tinted strip appears between the source banner and the Amount/Date/Account card, showing a Warning icon (16dp) and the text "Some fields need your review — AI was not confident about highlighted values." in `bodySmall` style with `onTertiaryContainer` color.
**Why human:** Compose visual rendering, color token resolution (tertiaryContainer in the active theme), and layout placement cannot be verified by static code analysis.

#### 2. Low-Confidence Field Error Tint — Visual Distinction

**Test:** Open the dialog with a draft where `confidence["amount"] = "low"` and `"amount"` is in `aiSuggestedFields`.
**Expected:** The Amount/Date/Account card background is visibly different (error-red tint) compared to the normal blue-primary-alpha tint seen on a normal AI-suggested field.
**Why human:** Two-tier tint contrast requires visual or instrumented Compose test — `errorContainer` vs `primary.copy(alpha=0.08f)` distinction is a Material 3 color comparison.

#### 3. Banner Absent on Null/False Path

**Test:** Open the dialog normally (tap the + FAB, not the AI draft FAB).
**Expected:** No amber banner appears; the form opens with blank fields and no tinting, identical to pre-Phase-41 behavior.
**Why human:** Absence of a visual element on the null path requires device verification.

#### 4. Tint Clears on Field Edit

**Test:** Open the dialog with a low-confidence amount draft. Tap any numpad key to edit the amount.
**Expected:** The errorContainer tint on the Amount card disappears immediately (aiSuggestedFields removes "amount"); normal surface background is restored.
**Why human:** Dynamic Compose state transition triggered by user input requires UI interaction testing.

### Gaps Summary

No code gaps found. All 8 must-have truths are VERIFIED in the codebase. The only open item is a documentation gap: AGENT-01 and AGENT-02 are not formally defined in REQUIREMENTS.md (they appear only in ROADMAP.md). This does not block the phase goal but should be remediated to keep REQUIREMENTS.md current with the v3.2 milestone scope.

The phase status is `human_needed` because 4 behavioral tests require visual/interactive verification on a device or via instrumented Compose tests — standard for any Compose UI phase.

---

_Verified: 2026-05-18T15:00:00Z_
_Verifier: Claude (gsd-verifier)_
