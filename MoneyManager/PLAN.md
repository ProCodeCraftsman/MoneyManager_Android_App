# PLAN.md — Transaction Screen Refactoring

## Goal
Fix all bugs documented in BUGS.md and restructure the transaction UI layer to be maintainable
across 6 transaction types (expense, income, transfer, lend, borrow, savings).

---

## 3.1 New Package Structure

```
ui/screens/                            (existing package — unchanged public API)
├── TransactionsScreen.kt              MODIFIED  — Scaffold + top-bar + FAB only (~200 lines)
├── TransactionsViewModel.kt           MODIFIED  — add setTagFilter(), fix clearAllFilters()

ui/screens/transactions/               NEW sub-package
├── components/
│   ├── TransactionsPeriodHeader.kt    NEW  — prev/next navigation + period label
│   ├── TransactionsSearchBar.kt       NEW  — animated search bar extracted from screen
│   ├── TransactionsList.kt            NEW  — LazyColumn with date grouping + split cards
│   └── TransactionsSummaryRow.kt      NEW  — income/expense/count metrics row
└── (no new dialog sub-package in this phase — dialogs stay in ui/dialogs)

ui/dialogs/                            (existing package)
├── AddEditTransactionDialog.kt        MODIFIED  — fix BUG-01,02,06 + tag dropdown
├── TransactionFormConfig.kt           UNCHANGED
├── TransactionFormState.kt            NEW  — sealed class for typed form state
└── TransactionFormConverter.kt        NEW  — TransactionEntity ↔ TransactionFormState
```

---

## 3.2 Sealed Class for Form State (`TransactionFormState.kt`)

```kotlin
sealed class TransactionFormState {
    data class ExpenseIncome(type, amount, accountId, categoryId, date, note,
                             peerId, tagIds, receiptPath, isSplitParent,
                             splitRows, goalId, investmentPlatform)
    data class Transfer(fromAccountId, toAccountId, amount, date, note,
                        tagIds, receiptPath)
    data class LendBorrow(type, peerId, amount, accountId, dueDate, date,
                          note, tagIds, receiptPath)
    data class Savings(goalId, investmentPlatform, amount, accountId, date,
                       note, tagIds, receiptPath)
}
```

This is a **foundation** — the existing `AddEditTransactionDialog` uses flat state today.
The sealed class enables future migration to individual `*Form.kt` composables without a
big-bang rewrite (see Step 4 below).

---

## 3.3 Converter (`TransactionFormConverter.kt`)

Two extension functions:
- `TransactionEntity.toFormState(categories)` — detects type and maps fields, normalises
  transfer direction (always outgoing: `fromAccountId = accountId`).
- `TransactionFormState.toTransactionEntity(originalId, originalCreatedAt)` — converts back to
  entity ready for DAO insert/update.

---

## 3.4 Bug Fixes (in order of implementation)

### Step 1 — Fix BUG-01: Tag dropdown
Update `FormTagSection` to accept `filteredTags`, `showDropdown`, `onTagSelected`,
`onDismissDropdown`. Wrap the text field in a `Box` and add a `DropdownMenu` overlay.

### Step 2 — Fix BUG-02: Transfer direction normalisation
Before initialising `selectedAccountId`/`selectedToAccountId`, detect whether the transaction
being edited is the incoming leg (`note.contains("transfer from", ignoreCase=true)`). If so,
swap `accountId` and `toAccountId` so the form always shows source → destination.

### Step 3 — Fix BUG-03: Remove dead TransferDialog
Remove `var showTransferDialog`, the `if (showTransferDialog)` block, and the
`import …TransferDialog` line from `TransactionsScreen.kt`.

### Step 4 — Fix BUG-04: "All" period clears date filter
In `updatePeriodBasedOnFilter`, add an `"All"` branch that calls
`viewModel.setDateRangeFilter(null, null)`.

### Step 5 — Fix BUG-05: Add `setTagFilter` to ViewModel
Add `fun setTagFilter(id: Long?) { _filters.value = _filters.value.copy(tagId = id) }`.
Also fix `clearAllFilters()` to use named params.

### Step 6 — Fix BUG-06: Populate `subCategoryId`
In `buildTransaction()`, add
`subCategoryId = if (TransactionFeature.CATEGORY in features) selectedSubCategory?.id else null`.

---

## 3.5 Screen Size Reduction

### Step 7 — Extract `TransactionsList.kt`
Move the `LazyColumn` with date-grouping headers, `SplitTransactionCard`, `TransactionItem`, and
the paging load-state indicators into `TransactionsList.kt`. Accept:
```kotlin
@Composable fun TransactionsList(
    groupedTransactions: Map<Long, List<TransactionEntity>>,
    pagingTransactions: LazyPagingItems<TransactionEntity>,
    expandedSplitIds: Set<Long>,
    collapsedDates: Set<Long>,
    currencyFormat: NumberFormat,
    uiState: TransactionsUiState,
    onToggleDateCollapse: (Long) -> Unit,
    onToggleSplitExpand: (Long) -> Unit,
    onTransactionClick: (TransactionEntity) -> Unit,
    onEditSplitParent: (TransactionEntity) -> Unit,
)
```

### Step 8 — Extract `TransactionsPeriodHeader.kt`
Move the "← April 2025 →" navigation row + `updatePeriodBasedOnFilter` into a standalone
composable that receives `timeFilter`, `currentPeriodName`, `onPrevious`, `onNext`.

### Step 9 — Extract `TransactionsSummaryRow.kt`
Move the income/expense/count metrics row + `SummaryMetricCompact` into its own file.

---

## 3.6 Safe Rollout Order

1. Create `TransactionFormState.kt` — no side effects, new file.
2. Create `TransactionFormConverter.kt` — no side effects, new file.
3. Apply bug fixes (Steps 1–6) to existing files — isolated edits.
4. Extract `TransactionsList.kt` (Step 7) — extract only; keep function signatures identical.
5. Extract period header and summary (Steps 8–9).
6. Run `./gradlew assembleDebug` after each step.
7. Manual test matrix (see §5).
8. Delete `TransferDialog.kt` only after confirming it is unused by search.

---

## 3.7 Fields not requiring schema changes

All fields needed for all 6 types already exist in `TransactionEntity`:

| Type     | Key Fields Used                                            |
|----------|------------------------------------------------------------|
| expense  | accountId, categoryId, amount, date, note, tagIds          |
| income   | accountId, categoryId, amount, date, note, tagIds          |
| transfer | accountId, toAccountId, isTransfer, amount, date, note     |
| lend     | accountId, peerContactId, expectedReturnDate, amount, date |
| borrow   | accountId, peerContactId, expectedReturnDate, amount, date |
| savings  | accountId, goalId, investmentPlatform, amount, date        |

No DB migration required.

---

## Phase 5 — Verification Checklist

- [ ] Add new expense: amount, category, account, date, note save correctly
- [ ] Edit expense: all fields pre-filled; save updates record + balance
- [ ] Add new income: works
- [ ] Edit income: pre-fills correctly
- [ ] Add transfer A→B: two transactions created, balances adjusted
- [ ] Edit transfer (open outgoing leg): From=A, To=B displayed correctly
- [ ] Edit transfer (open incoming leg): From=A, To=B displayed correctly (BUG-02 fixed)
- [ ] Add lend: peer, amount, due date saved; peer.totalGiven updated
- [ ] Edit lend: peer, amount, due date pre-filled
- [ ] Add borrow: peer, amount saved; peer.totalReceived updated
- [ ] Edit borrow: peer, amount pre-filled
- [ ] Add savings: goal, platform, amount saved; balance adjusted
- [ ] Edit savings: goal, platform pre-filled
- [ ] Tags: typing in tag search shows dropdown; selecting a tag adds chip (BUG-01 fixed)
- [ ] Split transaction: editing preserves all child rows
- [ ] Screen rotation during edit: form state preserved
- [ ] "All" period filter: shows all transactions, not filtered by date (BUG-04 fixed)
- [ ] TransferDialog dead code removed; no import errors (BUG-03 fixed)
- [ ] Filters: search, type, account, date range all work independently and together
- [ ] No crashes on any transaction type add or edit
