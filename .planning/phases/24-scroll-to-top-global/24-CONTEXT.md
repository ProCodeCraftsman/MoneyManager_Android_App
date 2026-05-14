# Phase 24: Scroll-to-Top (Global)

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Implementation of a **global reusable scroll-to-top feature** that appears when users scroll down on any scrollable screen in the app. This is a modifier-based approach that automatically wraps LazyColumn and provides scroll-to-top behavior.

Key aspects:
1. Reusable modifier that wraps any LazyColumn
2. Appears after 1 screen scroll threshold
3. Positioned center-bottom
4. Applied to all scrollable screens automatically

</domain>

<decisions>
## Implementation Decisions

### D-01: Button Visibility
- **Threshold:** Appears after scrolling 1 screen height (~50dp offset)
- Hidden when at top of list

### D-02: Button Position
- **Position:** Center-bottom of screen
- Floating above LazyColumn content
- 16dp vertical margin from bottom

### D-03: Component Implementation
- **Approach:** Modifier-based
- `Modifier.scrollToTop(lazyListState)` or `Modifier.withScrollToTop(lazyListState, onClick = { ... })`
- Automatically handles visibility based on scroll position
- Works with any LazyColumn/List

### D-04: Default Screens
- **Applied to:** All screens with LazyColumn automatically
- No manual opt-in required
- Screens: DashboardScreen, TransactionsScreen, AccountsScreen, BudgetsScreen, ReportsScreen, RecurringListScreen, GoalsScreen, CategoriesScreen, TagsScreen, TemplatesScreen, PeerListScreen, BorrowLendScreen, SettingsScreen

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary Files
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Shows existing scroll detection pattern (lines 149-151)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardScreen.kt` — Another scrollable screen example

### Key Implementation Reference
- Lines 149-151 in TransactionsScreen.kt show LazyColumn with scroll detection:
  - `lazyListState` with `derivedStateOf` for scroll position
  - Pattern to detect if scrolled past threshold

### Existing Components (for reference)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/` — Directory for reusable components

### Prior Phase Context
- `.planning/phases/22-transactions-header-nav/22-CONTEXT.md` — Phase 22 move-to-top (Transactions only, specific implementation)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- 16+ screens use LazyColumn — good candidates for modifier application
- Existing lazyListState pattern in TransactionsScreen can be generalized

### Integration Points
- Modifier applies to LazyColumn → connects to Scaffold body
- Uses derivedStateOf for performance-efficient scroll detection

### Implementation Pattern
```
val lazyListState = rememberLazyListState()
val showScrollToTop by remember {
    derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > threshold }
}
```

</code_context>

<specifics>
## Specific Implementation Details

### Modifier Pattern (Final Target)
```kotlin
@Composable
fun Modifier.scrollToTop(
    lazyListState: LazyListState,
    threshold: Int = 50
): Modifier
```

### Visual Design
- Center-bottom floating button
- Arrow up icon (Icons.Default.KeyboardArrowUp)
- Material 3 floating surface
- Smooth fade-in/out animation

### All Eligible Screens
1. DashboardScreen
2. TransactionsScreen
3. AccountsScreen
4. BudgetsScreen
5. ReportsScreen
6. RecurringListScreen
7. GoalsScreen
8. CategoriesScreen
9. TagsScreen
10. TemplatesScreen
11. PeerListScreen
12. BorrowLendScreen
13. SettingsScreen

</specifics>

<deferred>
## Deferred Ideas

- None for this phase

</deferred>

---

*Phase: 24-scroll-to-top-global*
*Context gathered: 2026-04-27*