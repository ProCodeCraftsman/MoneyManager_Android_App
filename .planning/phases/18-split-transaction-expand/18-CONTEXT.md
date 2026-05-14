# Phase 18: Split Transaction Expandable View - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning
**Source:** User feature request

<domain>
## Phase Boundary

Add expandable/collapsible UI for split type transactions in the transaction list screen. Currently split transactions display only as a parent transaction in a shadow box. Need to add ability to expand and view all child transactions that make up the split.

</domain>

<decisions>
## Implementation Decisions

### Core Behavior
- **SPLIT-BEHAVIOR-01**: Split transactions default to COLLAPSED state (closed) — child transactions are hidden by default
- **SPLIT-BEHAVIOR-02**: Expand icon (chevron down) visible on collapsed split parent transactions
- **SPLIT-BEHAVIOR-03**: Collapse icon (chevron up) visible when split is expanded
- **SPLIT-BEHAVIOR-04**: Tapping expand/collapse icon toggles visibility of child transactions
- **SPLIT-BEHAVIOR-05**: Child transactions displayed directly below parent, indented

### Child Transaction Display
- **SPLIT-CHILD-01**: Each child transaction shows its own category icon and name
- **SPLIT-CHILD-02**: Each child transaction shows its own amount
- **SPLIT-CHILD-03**: Each child transaction shows its note/description if present
- **SPLIT-CHILD-04**: Child transactions use same dense card style as regular transactions but with slight left indent

### Visual Design
- **SPLIT-VISUAL-01**: Expand icon: `Icons.Default.KeyboardArrowDown` (same as date headers)
- **SPLIT-VISUAL-02**: Collapse icon: `Icons.Default.KeyboardArrowUp`
- **SPLIT-VISUAL-03**: Child container has slight left padding/margin to indicate hierarchy
- **SPLIT-VISUAL-04**: No shadow box on expanded children — just visual grouping with indent

### the agent's Discretion
- Exact indent amount for child transactions (suggest 16-24dp)
- Animation for expand/collapse (if any)
- Whether to show total count of children on collapsed parent ("3 items")
- Whether children are swipe-editable

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Transaction Screen
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Main transaction list, contains `TransactionCardDense`, `TransactionItem`, date collapsing logic
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsViewModel.kt` — ViewModel with `getSplitChildren` method

### Data Model
- `MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt` — Entity with `isSplitParent`, `isSplitChild`, `parentTransactionId` fields

### Existing Patterns
- Date header collapsing: Uses `collapsedDates` state set with `mutableStateOf(setOf<Long>())` pattern (lines 128, 538-558)

</canonical_refs>

<specifics>
## Specific References

- Current `TransactionCardDense` composable (lines 853-1069) renders individual transaction cards
- `TransactionItem` wraps `TransactionCardDense` with swipe-to-dismiss (lines 759-850)
- Split children are fetched via `viewModel.getSplitChildren(parentId)` returning a `Flow<List<TransactionEntity>>` (seen in edit dialog, line 653-654)
- Type icon for split: `ICON_SPLIT = "🔀"` (line 79)
- Split parent indicator: `transaction.isSplitParent` check (line 882)

</specifics>

<deferred>
## Deferred Ideas

- Animation for expand/collapse (not critical for MVP)
- Child transaction swipe-to-edit (can be added later)
- Total amount display on collapsed parent ("Split: ₹500 across 3 items")

</deferred>

---

*Phase: 18-split-transaction-expand*
*Context gathered: 2026-04-25 via feature request*