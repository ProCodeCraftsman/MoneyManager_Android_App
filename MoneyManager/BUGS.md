# BUGS.md — MoneyManager Transaction System

## Critical Bugs (Causes Wrong Behavior)

### [BUG-01] Tag selection dropdown never renders
**File:** `ui/dialogs/AddEditTransactionDialog.kt`
**Reproducible:** Open Add Transaction → Enable Tags → Type in tag search box → nothing appears.
**Root cause:**
`filteredTags` and `showTagDropdown` are computed at the top of the dialog composable but never
passed to `FormTagSection`. The `FormTagSection` composable has no `DropdownMenu` for picking
existing tags. Users can type a query but cannot select a result.
```kotlin
// These exist but are never passed to FormTagSection:
val filteredTags = remember(tagQuery, tags) { … }
var showTagDropdown by rememberSaveable { mutableStateOf(false) }
```

---

### [BUG-02] Transfer editing shows reversed From/To accounts for the "incoming" leg
**File:** `ui/dialogs/AddEditTransactionDialog.kt`, `ui/screens/TransactionsScreen.kt`
**Reproducible:** Add transfer A→B, open the transaction list, tap the "Transfer from Account"
entry, tap Edit → the dialog shows From=B and To=A (reversed).
**Root cause:**
A transfer is stored as two `TransactionEntity` rows:
- Outgoing leg: `accountId=A, toAccountId=B, note="Transfer to Account"`
- Incoming leg: `accountId=B, toAccountId=A, note="Transfer from Account"`

When the user opens the incoming leg for editing, `selectedAccountId = B` (destination displayed as
source) and `selectedToAccountId = A` (source displayed as destination). The dialog needs to
normalise the direction before populating the form.

---

### [BUG-03] TransferDialog is dead/unreachable code
**File:** `ui/screens/TransactionsScreen.kt`
**Root cause:**
`showTransferDialog` is initialised to `false` and is **never set to `true`** anywhere in the
screen. The `TransferDialog` import and conditional block are completely unreachable. Transfer
creation already works through `AddEditTransactionDialog` (type = "transfer"), making
`TransferDialog` redundant.

---

### [BUG-04] Selecting "All" time filter does not clear the date range in the ViewModel
**File:** `ui/screens/TransactionsScreen.kt` — `onPeriodTypeSelected` lambda
**Reproducible:** Select Month, then switch period filter to "All" → transactions are still
filtered by the previous month's date range.
**Root cause:**
`updatePeriodBasedOnFilter("All")` is called but the `when` block has no branch for `"All"`, so
`viewModel.setDateRangeFilter(null, null)` is never invoked. The ViewModel's filter retains the
previous `startDate/endDate`.

---

### [BUG-05] `setTagFilter` method missing from ViewModel
**File:** `ui/screens/TransactionsViewModel.kt`
**Root cause:**
`FilterState` contains a `tagId` field and the DAO query supports tag filtering, but
`TransactionsViewModel` has no `setTagFilter(id: Long?)` method. The tag filter is permanently
stuck at `null` and cannot be changed from the UI.

---

## Moderate Bugs

### [BUG-06] `subCategoryId` never populated in `buildTransaction()`
**File:** `ui/dialogs/AddEditTransactionDialog.kt`
**Root cause:**
`buildTransaction()` correctly resolves the effective category leaf ID into `categoryId`, but
`subCategoryId` is always left `null`. Any UI component or query that relies on `subCategoryId`
will not see a sub-category even when one was selected.
```kotlin
// Missing:
subCategoryId = if (TransactionFeature.CATEGORY in features) selectedSubCategory?.id else null,
```

---

### [BUG-07] Split children loading race condition on configuration change (rotation)
**File:** `ui/dialogs/AddEditTransactionDialog.kt`
**Root cause:**
`splitRows` is a `rememberSaveable`. On first composition, `splitChildren` is `emptyList()` so
the saver captures the two default empty rows `[SplitRowData(0), SplitRowData(1)]`. The
`LaunchedEffect(splitChildren)` correctly overwrites them when data arrives, but on rotation the
*saved* (empty) state is restored before the effect fires again, potentially overwriting in-flight
edits.

---

### [BUG-08] Transfer direction detected by note string — fragile
**File:** `ui/screens/TransactionsViewModel.kt`
**Reproducible:** Add a transfer with note "Transfer from my wallet" → on update/delete, the code
detects `note.contains("Transfer to")` as `false` and may reverse balance in the wrong direction.
**Root cause:**
```kotlin
val isOutgoing = old.note.contains("Transfer to", ignoreCase = true)
```
Direction is inferred from the note instead of a dedicated `isOutgoingLeg: Boolean` flag. The
`isTransfer` flag exists but does not encode direction.

---

### [BUG-09] Account/Category/Type filter pickers in filter sheet are stub lambdas
**File:** `ui/screens/TransactionsScreen.kt`
```kotlin
onSelectAccount = { /* Logic to show account picker if needed */ },
onSelectCategory = { /* Logic to show category picker */ },
onSelectTransactionType = { /* Logic to show type picker */ },
```
The DAO and ViewModel fully support these filters, but the UI has no picker connected to them.

---

## Minor / Code-Quality Issues

### [BUG-10] `TransactionsScreen.kt` is 764 lines (screen + period logic + dialogs)
Mixed concerns: period navigation, search bar, grouped LazyColumn, filter sheet, three different
dialog orchestrations — all in a single file.

### [BUG-11] `AddEditTransactionDialog.kt` is 1290 lines (all 6 types in one composable)
No sealed-class form state; every state variable is at the top level regardless of type.
Type-switching resets state by mutation rather than swapping a typed state object.

### [BUG-12] `SplitRowData` is declared in the dialog file but publicly imported in the screen
`SplitRowData` is a dialog-internal detail that leaks into `TransactionsScreen.kt` via import.

### [BUG-13] `clearAllFilters()` uses positional FilterState constructor
```kotlin
_filters.value = FilterState("", null, null, null, null, null, null, null)
```
Adding a field to `FilterState` silently produces wrong results here.

### [BUG-14] `onTypeSelected` resets type-specific fields unconditionally
Switching types during editing clears `peerId`, `goalId`, `expectedReturnDate`, etc. even if the
user taps the same general category of type. Not a hard crash but leads to data loss if the user
accidentally taps the wrong type tab.
