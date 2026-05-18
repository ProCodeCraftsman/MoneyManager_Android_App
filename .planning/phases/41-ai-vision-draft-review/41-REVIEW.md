---
phase: 41-ai-vision-draft-review
reviewed: 2026-05-18T00:00:00Z
depth: standard
files_reviewed: 2
files_reviewed_list:
  - MoneyManager/app/src/test/java/com/moneymanager/ui/dialog/ConfidenceBannerStateTest.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt
findings:
  critical: 0
  warning: 0
  info: 2
  total: 2
fixed:
  critical: 1
  warning: 4
  commit: 0322a7c
  fixed_at: 2026-05-18T00:00:00Z
status: fixed
---

# Phase 41: Code Review Report

**Reviewed:** 2026-05-18T00:00:00Z
**Depth:** standard
**Files Reviewed:** 2
**Status:** issues_found

## Summary

Reviewed two files introduced in phase 41: the confidence-banner state test and the updated `AddEditTransactionDialog`. The predicate functions (`showReviewBanner`, `fieldIsLowConfidence`) are correctly implemented and their tests are structurally sound. The larger dialog file contains one critical correctness bug (a confidence key mismatch that silently prevents low-confidence tinting from ever firing for the `type` field), plus several warnings around integer overflow risk, missing medium-confidence test coverage, mismatched confidence key names, silent save failure, and an always-wrong `minutesAgo` display.

---

## Critical Issues

### CR-01: `fieldIsLowConfidence` called with key `"typeId"` but `TransactionDraft.confidence` is keyed by the field short-name

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt:516`

**Issue:** The call at line 516 is `fieldIsLowConfidence("typeId", initialDraft)`. The `confidence` map on `TransactionDraft` is populated by the AI vision pipeline. All other confidence key names used throughout the file match short, human-readable names: `"amount"`, `"date"`, `"accountName"`, `"categoryName"`, `"peerContactName"`, `"description"`, `"note"`. The AI pipeline would emit `"type"` (consistent with the draft field name `typeId` being the *ID* suffix attached to a logical field called `type`). Using `"typeId"` as the map key means this lookup will always return `false`; the type field will never receive an error-container background, silently defeating the low-confidence tint feature for the transaction-type row.

Cross-check: the `aiSuggestedFields` tracking uses `"type"` as the key (line 274, 402), confirming the logical name is `"type"`, not `"typeId"`. The confidence map lookup must match.

**Fix:**
```kotlin
// Line 516 — change:
fieldIsLowConfidence("typeId", initialDraft) && isTypeAiField ->
// to:
fieldIsLowConfidence("type", initialDraft) && isTypeAiField ->
```

---

## Warnings

### WR-01: Integer overflow in `minutesAgo` calculation for drafts older than ~35 days

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt:561`

**Issue:** The expression `((System.currentTimeMillis() - initialDraft.date) / 60_000).toInt()` truncates a `Long` to `Int`. `Int.MAX_VALUE` is 2,147,483,647 — this overflows when the draft's `date` is more than ~2.1 billion minutes (~4,000 years) in the past, which is not a realistic concern. However, the more practical risk is a **future** `date` value: if the AI populates `initialDraft.date` with a timestamp that is in the future (e.g., a mis-parsed date), the result of the subtraction is negative. The `.toInt()` on a large negative `Long` can overflow to a positive value, causing the UI to display a nonsensical large positive minutes count instead of "just now". The `minutesAgo >= 1` guard does not catch this because the overflowed positive value will pass the check.

**Fix:**
```kotlin
val elapsedMs = System.currentTimeMillis() - initialDraft.date
if (elapsedMs in 60_000..Long.MAX_VALUE) {
    val minutesAgo = elapsedMs / 60_000
    append(" · ${minutesAgo} minutes ago")
} else {
    append(" · just now")
}
```
This eliminates the `.toInt()` cast and correctly handles future timestamps.

---

### WR-02: `amountValid` check permits a non-numeric expression string through to the Save button, causing a silent no-op save

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt:995`

**Issue:** The Save button is enabled when `amountValid = amount.isNotEmpty()` (line 995). A user can type an unevaluated arithmetic expression (e.g., `"100+50"`) without pressing `=`, then tap Save. The button is enabled, `buildTransaction()` calls `amount.toDoubleOrNull()` which returns `null` for `"100+50"`, `buildTransaction` returns `null`, and the `onConfirm` callback is silently never called (lines 1000–1004). The user sees no error — the dialog just does nothing. This is a UX bug that will cause user confusion and unreported data loss.

**Fix:**
```kotlin
// In FormActionButtons, tighten the enabled guard:
amountValid = amount.isNotEmpty() && amount.toDoubleOrNull() != null
// Or, alternatively, evaluate and check before enabling Save:
amountValid = (amount.toDoubleOrNull() ?: runCatching { evaluateExpression(amount) }.getOrNull()) != null
```

---

### WR-03: `fieldIsLowConfidence` is called with `"accountName"` for the account field but `TransactionDraft` carries a numeric `accountId` and text `accountName` — key inconsistency across fields

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt:608`

**Issue:** Line 608 uses key `"accountName"` to test for low confidence on the account field. The `TransactionDraft` data class exposes both `accountId: Long?` and `accountName: String?`. The AI vision pipeline determines what confidence keys it emits. If the pipeline emits confidence under `"accountId"` (the field that is actually resolved and used), the lookup under `"accountName"` will silently miss it, just as CR-01 does for `"typeId"`. This is the same class of key-mismatch bug. Without seeing the pipeline's emitted keys, the reviewer cannot confirm which is right; but the inconsistency across fields (`"amount"` vs `"accountName"` vs `"categoryName"` vs `"peerContactName"`) suggests the key naming convention is unspecified and fragile.

**Fix:** Define constants for all confidence map keys in `TransactionDraft` or a companion object, and use those constants both when populating the map in the AI pipeline and when reading it in the UI:
```kotlin
// In TransactionDraft companion:
object ConfidenceKey {
    const val TYPE      = "type"
    const val AMOUNT    = "amount"
    const val DATE      = "date"
    const val ACCOUNT   = "accountId"   // or "accountName" — decide once
    const val CATEGORY  = "categoryId"  // or "categoryName"
    const val PEER      = "peerContactId"
    const val NOTE      = "note"
}
```

---

### WR-04: `LaunchedEffect(initialDraft)` re-fires and overwrites user edits whenever the parent recomposes with a new `initialDraft` reference

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt:377`

**Issue:** The `LaunchedEffect` keyed on `initialDraft` runs whenever the `initialDraft` argument reference changes. If the parent composable (caller of `AddEditTransactionDialog`) recomposes and creates a new `TransactionDraft` instance (even with identical field values — common with `remember` misuse at the call site), the effect fires again, silently resetting every field the user has already edited back to the draft values. `TransactionDraft` is a `data class` so structural equality is used for the `LaunchedEffect` key comparison only if the object is directly the key; in Compose, `LaunchedEffect` uses `===` referential equality for its key. Any caller that allocates a new draft on each composition will trigger repeated resets.

**Fix:** Use `rememberSaveable` or `remember` at the call site to stabilize the draft reference, or guard the effect with a `var draftApplied by rememberSaveable { mutableStateOf(false) }` flag:
```kotlin
var draftApplied by rememberSaveable { mutableStateOf(false) }
LaunchedEffect(initialDraft) {
    if (initialDraft != null && !draftApplied) {
        draftApplied = true
        // ... populate fields
    }
}
```

---

## Info

### IN-01: Test suite has no coverage for `"medium"` confidence values

**File:** `MoneyManager/app/src/test/java/com/moneymanager/ui/dialog/ConfidenceBannerStateTest.kt:40`

**Issue:** The `fieldIsLowConfidence` doc comment (dialog line 75) explicitly lists `"medium"` as a non-low value that must return `false`. The test suite covers only `"low"` (returns true) and `"high"` (returns false), leaving the `"medium"` case untested. While the implementation is a simple equality check and will behave correctly, the stated contract includes `"medium"` and testing it costs one line.

**Fix:**
```kotlin
@Test
fun `fieldIsLowConfidence returns false when confidence for field is medium`() {
    val draft = TransactionDraft(confidence = mapOf("amount" to "medium"))
    assertFalse(fieldIsLowConfidence("amount", draft))
}
```

---

### IN-02: Tags low-confidence tinting is never applied — no `fieldIsLowConfidence` check for the tags section

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt:927`

**Issue:** Every other AI-suggested field section (type, amount/date/account, category, peer, note) has a `fieldIsLowConfidence` check to apply `errorContainer` tinting when confidence is low. The tags section (lines 914–954) applies only the generic `primary.copy(alpha = 0.08f)` background for any AI-suggested tags, with no low-confidence tint. If the AI marks tag confidence as low, the user receives no visual warning. This is an omission relative to the consistent pattern established for every other field.

**Fix:** Add a low-confidence check in the tags section:
```kotlin
val isTagsLowConfidence = isTagsAiField && fieldIsLowConfidence("tags", initialDraft)
Box(
    modifier = if (isTagsLowConfidence) Modifier.background(
        MaterialTheme.colorScheme.errorContainer,
        RoundedCornerShape(4.dp)
    ).padding(horizontal = 2.dp)
    else if (isTagsAiField) Modifier.background(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        RoundedCornerShape(4.dp)
    ).padding(horizontal = 2.dp)
    else Modifier
) { ... }
```

---

_Reviewed: 2026-05-18T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
