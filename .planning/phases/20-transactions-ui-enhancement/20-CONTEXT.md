# Phase 20: Transactions UI Enhancement - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

UI/UX enhancements to TransactionsScreen: collapsible header, sticky date summary, scroll-based layout, single background with elevation separation. Only visual/layout changes — no business logic, database, or API changes.

</domain>

<decisions>
## Implementation Decisions

### Header Text Visibility
- **D-01:** Use theme-aware colors — `MaterialTheme.colorScheme.onSurface` for automatic light/dark support

### Collapsible Header
- **D-02:** Use scroll-based collapse — LazyColumn with scroll state detection to reduce header height on scroll

### Sticky Date/Summary
- **D-03:** Sticky date summary moves up and down based on scrolling — uses LazyColumn stickyHeaders

### Bottom Color Gap
- **D-04:** Remove trailing spacer above bottom navigation

### Single Background
- **D-05:** Use one continuous background color with elevation for separation — remove section-specific backgrounds, use shadow/elevation for component separation

### Agent Discretion
- Exact scroll threshold for collapse (e.g., after N items scrolled)
- Animation timing for smooth transitions
- Exact elevation values (recommend 2-3dp)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Transaction Screen
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Main file to modify
- Lines ~260-335: Current header implementation with Surface
- Lines ~410-481: Search bar implementation
- Lines ~498-564: Date header sticky rendering

### Patterns from Phase 19
- `shadowElevation = 2.dp` already used on sticky date headers
- Theme-aware colors via `MaterialTheme.colorScheme`

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- WindowCompat API already imported and used (from Phase 19)
- LazyColumn with stickyHeaders already implemented
- Surface with tonalElevation/shadowElevation patterns in place

### Established Patterns
- Header uses Surface with shadowElevation
- Date header uses stickyHeader block
- searchText state already exists

### Integration Points
- Scaffold topBar parameter
- LazyColumn stickyHeaders block
- search state management

</code_context>

<specifics>
## Specific Ideas

- Use `rememberScrollState()` on LazyColumn for scroll detection
- Add `firstVisibleItemIndex` to trigger header collapse
- Use `animateSizeAsState()` for smooth transitions
- Remove `color = ...` from Surface/Row modifiers, rely on `MaterialTheme.colorScheme.surface`

</specifics>

<deferred>
## Deferred Ideas

- Search toggle (hidden by default) — was discussed in Phase 19 requirements but not selected in this phase

---

*Phase: 20-transactions-ui-enhancement*
*Context gathered: 2026-04-25*