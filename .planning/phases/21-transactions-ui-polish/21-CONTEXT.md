# Phase 21: Transactions UI Polish - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Complete the TransactionsScreen visual redesign that phases 19-20 started but did not finish. All changes are layout and visual only — no business logic, ViewModel, or data model changes. Single file target: `TransactionsScreen.kt`.

Seven items from post-phase-20 review:
- Proper header-summary scroll choreography
- Search as hidden button near filters
- True single-surface theme discipline
- Single-color + shadow separation model
- Summary behavior (sticky / scroll interaction)
- Clean bottom navigation separation
- Bottom spacing fix

</domain>

<decisions>
## Implementation Decisions

### D-01: Scroll Choreography — topBar content
- **topBar contains only the compact header row** (the 56dp/48dp animated row with title, time filter dropdown, search icon, filter icon)
- Summary row and time navigation bar are **removed from topBar**
- They become items inside the LazyColumn (item 0, before the transaction groups)
- Result: when scrolled, only the compact header row stays sticky; summary + time nav scroll away naturally

### D-02: Summary Surface Treatment
- Summary row (Spent / Income / Items) is a **full-width Surface panel** with `shadowElevation = 1.dp`
- **No rounded card shape** — no `RoundedCornerShape`, no `surfaceVariant.copy(alpha = 0.5f)` alpha hack
- Use `MaterialTheme.colorScheme.surface` or `surfaceContainer` — a proper theme token, no alpha modification
- It scrolls as a regular list item; it is NOT a stickyHeader

### D-03: Search — Hidden by Default
- **No always-visible search bar** in the content area
- Search is a **search icon (🔍) in the header row**, placed left of the filter icon
- When tapped: an animated search bar expands **below the header row** (inside topBar, below the header Row, using `AnimatedVisibility`)
- The header row remains visible while search is open
- Dismiss (X button or back): search bar collapses, `searchText` cleared

### D-04: Filter Indicator
- When filters are active: **badge on the filter icon only** (existing BadgedBox + Badge pattern)
- No filter chip row anywhere
- Users open the filter sheet to view/clear individual filters

### D-05: Bottom Navigation Separation
- **Color match, no gap** — list background and nav bar use the same surface color
- Reduce `contentPadding = PaddingValues(bottom = 56.dp)` to the actual nav bar height (likely `WindowInsets.navigationBars` or a fixed value matching the nav bar)
- No visible color seam between the last transaction and the nav bar

### D-06: Single-Surface Discipline
- Remove all `surfaceVariant.copy(alpha = 0.5f)` usages in the layout area (search bar background, summary background)
- Use proper Material 3 color tokens: `surfaceContainer`, `surfaceContainerLow`, or plain `surface`
- Elevation (shadowElevation, tonalElevation) provides separation — not background color differences

### Claude's Discretion
- Exact animation duration for search expand/collapse (recommend 200-300ms)
- Whether `searchActive` state is `rememberSaveable` (recommend yes — survives recomposition)
- Exact shadowElevation value for summary panel (1dp decided, but 0.5dp acceptable if 1dp is too heavy)
- contentPadding bottom value — use `WindowInsets.navigationBars` if available, else match nav bar height constant

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary File
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Only file to modify

### Key Sections in TransactionsScreen.kt (current line numbers, may shift)
- Lines 134–143: `lazyListState`, `isScrolled`, `animatedHeaderHeight` — scroll state already wired
- Lines 259–397: `Scaffold { topBar = ... }` — current topBar with Surface containing header + time nav + summary
- Lines 322–335: Filter icon with existing BadgedBox (search icon to be added here)
- Lines 367–386: Summary Surface (currently rounded card — to be replaced per D-02)
- Lines 338–365: Time navigation bar (to be moved into LazyColumn)
- Lines 399–482: Current always-visible search bar (to be removed — replaced by D-03)
- Lines 497–500: LazyColumn with `state = lazyListState`, `contentPadding = PaddingValues(bottom = 56.dp)`

### Prior Phase Context
- `.planning/phases/20-transactions-ui-enhancement/20-CONTEXT.md` — Phase 20 decisions (D-01 through D-05 carry forward)
- `.planning/phases/20-transactions-ui-enhancement/20-01-SUMMARY.md` — What phase 20 actually implemented

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `lazyListState` + `isScrolled` + `animatedHeaderHeight` — already implemented, use as-is
- `BadgedBox` + `Badge` on filter icon — already present at lines 323-335, add search icon alongside
- `AnimatedVisibility` — import may be needed for search bar expand/collapse
- `searchText` state at line 144 — already exists, just need to wire to new UI

### Established Patterns
- `derivedStateOf` for scroll state
- `animateDpAsState` for animated dimensions
- `MaterialTheme.colorScheme.*` throughout — single theme token pattern locked
- `stickyHeader {}` in LazyColumn — already in use for date headers

### Integration Points
- Summary row and time nav bar: move from topBar `Column` into `LazyColumn` as `item {}` blocks before the `groupedTransactions.forEach`
- Search visibility: add `var searchActive by rememberSaveable { mutableStateOf(false) }` state; wire to header icon tap and `AnimatedVisibility`
- contentPadding: update bottom value to match nav bar height

### Known Constraints
- `TransactionsScreen.kt` is 2,459 lines — large file, changes are surgical (topBar restructure + LazyColumn item additions + search removal)
- `BasicTextField` for search already exists at line 421 — reuse it inside the new expandable bar

</code_context>

<specifics>
## Specific Layout Target

**At-rest (not scrolled, no search):**
```
┌──────────────────────────┐
│ Transactions    [🔍] [≡] │  ← topBar, 56dp, animates to 48dp
├──────────────────────────┤  ← shadow from topBar Surface
│ Spent | Income | Items   │  ← LazyColumn item 0, full-width, 1dp shadow
┃──────────────────────────┃
│ ◀  This month  ▶         │  ← LazyColumn item 1, time nav (conditional)
├──────────────────────────┤
│ Mon, Apr 25              │  ← stickyHeader (date)
│ Coffee         -₹120     │
│ Salary        +₹45,000   │
└──────────────────────────┘

**Scrolled (summary + time nav off-screen):**
┌──────────────────────────┐
│ Transactions    [🔍] [≡] │  ← topBar, 48dp
├──────────────────────────┤
│ Mon, Apr 25              │  ← stickyHeader stays
│ Coffee         -₹120     │
│ Salary        +₹45,000   │
└──────────────────────────┘

**Search active:**
┌──────────────────────────┐
│ Transactions    [🔍] [≡] │  ← topBar header row
├──────────────────────────┤
│ 🔍 Search...         [×] │  ← AnimatedVisibility, expands below header
└──────────────────────────┘
```

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 21-transactions-ui-polish*
*Context gathered: 2026-04-25*
