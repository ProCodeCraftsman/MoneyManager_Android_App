# Phase 22: Transactions Screen - Header, Search, Navigation & Interaction Enhancements

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Implementation of TransactionsScreen UI enhancements per user specification. All changes are UI behavior and interaction only. Single file target: `TransactionsScreen.kt`.

Key enhancements:
1. Header layout: Time filter on right side, search icon left of filter
2. Search bar: Hidden by default, toggle-controlled
3. Navigation bar: Sticky on scroll (stays visible while scrolling)
4. Move-to-top button: Floating button for quick navigation
5. Daily summary: Collapse/expand toggle per date group
6. Statistics: Always follow time filter

</domain>

<decisions>
## Implementation Decisions

### D-01: Time Filter Placement
- **Time filter dropdown moves to right side of header**
- Header order: [Title/Time Filter on LEFT] remains, BUT spec says time filter on RIGHT
- Spec explicitly states: Search Icon → Time Filter → Filter icons on RIGHT
- Order inside header right section: [Search] → [Time Filter ▼] → [Filter]
- Time filter still controls transactions list, statistics, and navigation calendar

### D-02: Search Bar Behavior
- **Hidden by default** (already implemented in Phase 21)
- New interaction: Search icon toggles visibility
- When search hidden while typing: clear search + hide bar
- Use AnimatedVisibility for smooth transitions

### D-03: Navigation Bar - Sticky on Scroll
- Navigation bar (◀ April 2026 ▶) becomes **sticky** when scrolling
- Implementation: Use `stickyHeader` inside LazyColumn
- Position: Immediately below header, stays visible while scrolling
- This is different from Phase 21 where it scrolls with content

### D-04: Move-to-Top Button
- Position: Bottom-right, above FloatingActionButton
- Show when: Scroll position > 1 screen height (threshold)
- Icon: Arrow Up (Icons.Default.KeyboardArrowUp)
- Behavior: animateScrollToItem(0)

### D-05: Collapse/Expand Daily Summary
- User can collapse or expand each daily date header
- Visual indicator: Chevron icon (▼ expanded, ▲ collapsed)
- State: `collapsedDates` already exists in code (line 119)
- Just expose toggle control via tap on date header

### D-06: Statistics Following Time Filter
- Statistics (Spent/Income/Items) must use **filtered transactions**
- Not total transactions from database
- Filter applied before calculation

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary File
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Only file to modify

### Key Sections in TransactionsScreen.kt
- Lines 119: `collapsedDates` state - already exists
- Lines 121-128: `lazyListState`, scroll detection
- Lines 135-140: timeFilter, timeFilterOptions
- Lines 246-321: topBar with compact header
- Lines 333-645: Scaffold body with LazyColumn

### Prior Phase Context
- `.planning/phases/21-transactions-ui-polish/21-CONTEXT.md` — Phase 21 decisions (carries forward)
- `.planning/phases/21-transactions-ui-polish/21-01-SUMMARY.md` — What was implemented

</canonical_refs>

<specifics>
## Specific Implementation Details

### Header Layout (Final Target)

```
[ Transactions    🔍   All Time ▼   Filter ]
```

- Left: Title "Transactions"
- Right section (ordered): Search Icon → Time Filter Dropdown → Filter Icon
- Time filter dropdown: All Time, Day, Week, Month, Year, Custom

### Search Bar
- Hidden initially (isSearchVisible = false)
- Press Search icon → show search bar
- Press again → hide + clear search text
- Position: Below header, inside topBar Surface
- Animation: AnimatedVisibility (Fade + Slide)

### Navigation Bar Sticky
```
◀   April 2026   ▶
```
- Uses stickyHeader inside LazyColumn
- Shows when timeFilter != All Time AND != Custom
- Left/Right arrows navigate periods

### Move-to-Top Button
- Bottom-right corner
- Above FAB
- Shows when firstVisibleItemIndex > 0 OR firstVisibleItemScrollOffset > threshold
- Press → smooth scroll to top

### Collapse/Expand Daily
- Date header: "Friday, Apr 24"
- Tap date header → toggle collapsed/expanded
- Chevron: ▼ (down) = expanded, ▲ (up) = collapsed
- Uses existing collapsedDates state

### Statistics Must Follow Time Filter
- Calculate from filtered transactions, not all
- timeFilter determines which transactions to include

</specifics>

<deferred>
## Deferred Ideas

Items explicitly out of scope for this phase:
- Database changes
- Entity modifications
- Transaction item layout changes
- Add/Edit dialog modifications
- Existing filter logic structure changes

</deferred>

---

*Phase: 22-transactions-header-nav*
*Context gathered: 2026-04-27*