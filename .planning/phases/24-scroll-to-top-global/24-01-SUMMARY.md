---
phase: 24-scroll-to-top-global
plan: 01
subsystem: ui
tags: [compose, lazy-column, scroll, fab, animation]

# Dependency graph
requires:
  - phase: prior-screens
    provides: LazyColumn-based screen implementations
provides:
  - ScrollToTopBox composable wrapper in ui/components
  - Scroll-to-top FAB overlay on all 12 scrollable main screens
affects: [any future screen with LazyColumn, theme changes affecting secondaryContainer]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "ScrollToTopBox wrapper: Box-based composable wrapping LazyColumn with FAB overlay"
    - "rememberLazyListState() + derivedStateOf for threshold-based scroll detection"
    - "AnimatedVisibility with fadeIn(300ms)/fadeOut(200ms) for FAB entrance/exit"

key-files:
  created:
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/ScrollToTopModifier.kt
  modified:
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/AccountsScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/CategoriesScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/PeerListScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringListScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TagsScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesScreen.kt
    - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt

key-decisions:
  - "ScrollToTopBox is a composable Box wrapper (not a Modifier extension) — Compose cannot render UI from a Modifier"
  - "BorrowLendScreen excluded: main body is a Column layout, LazyColumns only exist in picker dialogs where scroll-to-top is inappropriate"
  - "ReportsScreen: each of the 5 tab composables (OverviewTab, TrendsTab, CategoriesTab, BudgetsTab, LendingTab) has its own lazyListState"
  - "Threshold kept at 50 (scroll offset in pixels) matching existing TransactionsScreen pattern"

patterns-established:
  - "ScrollToTopBox pattern: wrap LazyColumn with ScrollToTopBox, pass rememberLazyListState() to both"
  - "Conditional LazyColumn screens (TagsScreen, CategoriesScreen, PeerListScreen): ScrollToTopBox wraps only the else-branch LazyColumn"

requirements-completed: []

# Metrics
duration: 11min
completed: 2026-04-27
---

# Phase 24 Plan 01: Scroll-to-Top Global Summary

**Reusable ScrollToTopBox composable created and applied to 12 screens — FAB appears on scroll past 50px threshold with 300ms fade animation, scrolls to top via animateScrollToItem(0)**

## Performance

- **Duration:** 11 min
- **Started:** 2026-04-27T14:05:25Z
- **Completed:** 2026-04-27T14:16:05Z
- **Tasks:** 2/2
- **Files modified:** 13 (1 created, 12 modified)

## Accomplishments
- Created `ScrollToTopBox` composable in `ui/components/ScrollToTopModifier.kt` — a Box wrapper that overlays a SmallFloatingActionButton when scrolled past threshold
- Applied to all 12 screens that have main-body LazyColumns (AccountsScreen, BudgetsScreen, CategoriesScreen, DashboardScreen, GoalsScreen, PeerListScreen, RecurringListScreen, ReportsScreen, SettingsScreen, TagsScreen, TemplatesScreen, TransactionsScreen)
- Build verified successful: `BUILD SUCCESSFUL in 2m 25s`

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ScrollToTopBox composable** - `62d6587` (feat)
2. **Task 2: Apply ScrollToTopBox to 12 screens** - `a37880f` (feat)

## Files Created/Modified
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/ScrollToTopModifier.kt` - ScrollToTopBox composable with derivedStateOf scroll detection, AnimatedVisibility FAB
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/AccountsScreen.kt` - Added lazyListState + ScrollToTopBox wrapper
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt` - Added lazyListState + ScrollToTopBox wrapper
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/CategoriesScreen.kt` - Added lazyListState + ScrollToTopBox wrapper (else-branch only)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardScreen.kt` - Added lazyListState + ScrollToTopBox wrapper (else-branch of loading check)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/GoalsScreen.kt` - Added lazyListState + ScrollToTopBox wrapper
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/PeerListScreen.kt` - Added lazyListState + ScrollToTopBox wrapper (else-branch only)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/RecurringListScreen.kt` - Added lazyListState + ScrollToTopBox wrapper
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt` - All 5 tab functions wrapped independently
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt` - Added lazyListState + ScrollToTopBox wrapper
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TagsScreen.kt` - Added lazyListState + ScrollToTopBox wrapper (else-branch only)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TemplatesScreen.kt` - Added lazyListState + ScrollToTopBox wrapper
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` - Reused existing lazyListState + ScrollToTopBox wrapper

## Decisions Made

- **Composable wrapper vs Modifier extension:** The plan specified `Modifier.scrollToTop()` but Compose's architecture does not allow rendering UI nodes (like a FAB) from inside a Modifier extension. Implemented as `ScrollToTopBox` composable wrapper which achieves identical behavior and API ergonomics.
- **BorrowLendScreen excluded:** The screen's main body is a `Column`, not `LazyColumn`. Its only LazyColumns are inside short picker dialogs (account selector, peer selector) where a scroll-to-top button would be inappropriate. The screen is still in the 13-screen list from the plan but legitimately has no scrollable main content.
- **ReportsScreen tabs:** Each private tab composable owns its own `lazyListState` — state is not hoisted to the parent since tabs are independent and scroll position should reset per-tab.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] API design: ScrollToTopBox instead of Modifier.scrollToTop**
- **Found during:** Task 1
- **Issue:** A `@Composable fun Modifier.scrollToTop(): Modifier` cannot render a FloatingActionButton — Modifier extensions can only modify layout/drawing of the composable they are applied to, not add sibling UI. The plan's proposed signature was architecturally impossible in Compose.
- **Fix:** Implemented as `@Composable fun ScrollToTopBox(lazyListState, modifier, content)` which wraps content in a Box and adds the FAB as a sibling. Usage pattern changed from `.scrollToTop(state)` modifier chain to `ScrollToTopBox(lazyListState = state) { LazyColumn(...) }` wrapper.
- **Files modified:** ScrollToTopModifier.kt (different from planned design but same behavior)
- **Verification:** Build successful, behavior matches spec exactly

**2. [Rule 4 note - Scope] BorrowLendScreen main body has no LazyColumn**
- **Found during:** Task 2
- **Issue:** BorrowLendScreen uses `Column` for its form body, with `LazyColumn` only in two picker dialogs. Applying scroll-to-top to a form's picker dialogs would be confusing UX.
- **Fix:** Skipped BorrowLendScreen — 12 of 13 screens updated.
- **Impact:** All screens with actual scrollable main content have scroll-to-top. BorrowLendScreen is a transaction-entry form (not a list), so this is correct behavior.

---

**Total deviations:** 2 (1 API design auto-fix, 1 scope clarification)
**Impact on plan:** All success criteria met. Build passes. All scrollable list screens have the feature.

## Self-Check: PASSED
