# Phase 19: Transactions UI Fixes - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Visual/UI corrections to TransactionsScreen. Fix header, sticky date headers, search bar, and filter components. Only visual changes — no business logic or data model modifications.

</domain>

<decisions>
## Implementation Decisions

### Header/Status Bar
- **D-01:** Match status bar color to header background using WindowCompat API

### Date Header Elevation
- **D-02:** Add BOTH elevation and padding to sticky date headers
- **D-02a:** Add shadow elevation (2-3dp) behind date header
- **D-02b:** Add vertical padding to create spacing gap

### Search Bar
- **D-03:** Center placeholder text vertically within current search bar height
- **D-04:** Keep search icon position as-is (current right-side position)

### Filter Chips
- **D-05:** Match filter chip height to text field height

### Filter Indicator
- **D-06:** Light weight indicator: 30% opacity background, reduced horizontal padding

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Transaction Screen
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Main file to modify
- Lines ~247-260: Scaffold with Surface for header
- Lines ~498-564: Date header sticky rendering with collapsedDates
- Lines ~566+: Transaction LazyColumn items

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Icons.Default.KeyboardArrowDown` / `KeyboardArrowUp` — already used in date headers
- `Surface` with tonalElevation — used in header
- `collapsedDates` state pattern — uses mutableStateOf(setOf<Long>())

### Established Patterns
- Header uses Row with 56.dp height
- Date header has 1.dp tonalElevation currently
- Search bar has BasicTextField

### Integration Points
- Scaffold topBar parameter for header
- stickyHeader block in LazyColumn for date headers

</code_context>

<specifics>
## Specific Ideas

- WindowCompat status bar color: Use window.statusBarWindowsCompat.setAppearance parameters or EdgeToEdge pattern
- Date header elevation: Add shadowElevation = 2.dp, padding(vertical = 6.dp)
- Search placeholder: Use contentAlignment.CenterVertically or adjust modifier

</specifics>

<deferred>
## Deferred Ideas

- None — all requested corrections captured

---

*Phase: 19-transactions-ui-fixes*
*Context gathered: 2026-04-25*