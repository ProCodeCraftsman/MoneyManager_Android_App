# Phase 19: Transactions UI Fixes - Research

**Researched:** 2026-04-25
**Phase:** 19-transactions-ui-fixes

---

## Domain Analysis

### What This Phase Delivers
Visual/UI corrections to TransactionsScreen — fixes for header status bar, sticky date headers, search bar placeholder, and filter components. Pure UI changes, no business logic modifications.

### Technical Stack
- Jetpack Compose UI (Material 3)
- WindowCompat API for status bar coloring
- Compose LazyColumn with stickyHeader

---

## Key Patterns from CONTEXT.md

### D-01: Status Bar Color
- Use `WindowCompat.getInsetsController()` or EdgeToEdge pattern
- Set status bar appearance to match header background

### D-02: Date Header Elevation
- Add `shadowElevation = 2.dp` to Surface/Modifier
- Add vertical padding (6.dp) for spacing gap
- Current: 1.dp tonalElevation

### D-03: Search Placeholder Centering
- Use `contentAlignment.CenterVertically` on BasicTextField
- Or adjust modifier with `.align(Alignment.CenterVertically)`

### D-04: Filter Chip Height Matching
- Match filter chip height to search text field height
- Adjust via `.height()` modifier or intrinsic size

### D-05: Active Filter Indicator
- 30% opacity background
- Reduced horizontal padding vs default

---

## Technical Insights

### Status Bar Color Approaches
1. **WindowCompat approach:**
```kotlin
WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
```

2. **EdgeToEdge with systemBarsController:**
```kotlin
WindowInsetsControllerCompat(window, view).apply {
    appearance = WindowInsetsController APPEARANCE_DARK_NAVIGATION_BAR
}
```

### Date Header Elevation
Current pattern (from CONTEXT.md):
- `collapsedDates` state uses `mutableStateOf(setOf<Long>())`
- Surface with tonalElevation currently 1.dp
- Expand icon uses `Icons.Default.KeyboardArrowDown/Up`

Best approach: Add both shadow elevation AND padding to create visible separation.

### Search Field Centering
- BasicTextField has `contentAlignment` parameter
- Or use `Modifier.align(Alignment.CenterVertically)` on text child

---

## Implementation Notes

### Files to Modify
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt`
  - Lines ~247-260: Header with Scaffold topBar
  - Lines ~498-564: Date header sticky rendering
  - Lines ~566+: Transaction list items

### Verification Checkpoints
1. Status bar matches header background when TransactionsScreen is visible
2. Date headers cast visible shadow and have spacing gap
3. Search placeholder centered in available height
4. Filter chips align with search bar height
5. Active filter has subtle 30% opacity background

---

## Validation Architecture

### Test Strategy
Manual UI verification required (visual changes):
1. Open TransactionsScreen
2. Check status bar color matches header
3. Scroll to see sticky date headers and verify shadow/spacing
4. Check search placeholder vertical centering
5. Apply filter and check chip height alignment

---

*Research complete: Ready for planning*