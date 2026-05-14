---
phase: 29-status-pane
fixed_at: 2026-04-30T14:30:00Z
review_path: .planning/phases/29-status-pane/29-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 29: Code Review Fix Report

**Fixed at:** 2026-04-30T14:30:00Z
**Source review:** .planning/phases/29-status-pane/29-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 3
- Fixed: 3
- Skipped: 0

## Fixed Issues

### CR-01: Nested scrolling conflict between Column and LazyVerticalGrid in StatusPaneScreen

**Files modified:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneFigureGrid.kt`
**Commit:** 2aeb92d
**Applied fix:** Replaced LazyVerticalGrid with non-lazy Column/Row layout using chunked items to resolve scrolling conflicts.

### WR-01: Locale-sensitive currency formatting causes incorrect decimal separators

**Files modified:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneHero.kt`, `MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneFigure.kt`
**Commit:** 76a5568
**Applied fix:** Added `import java.util.Locale` and replaced `"%.2f".format(value)` with `String.format(Locale.US, "%.2f", value)` in both files.

### IN-01: Unused import in InsightsScreen.kt

**Files modified:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsScreen.kt`
**Commit:** c10737b
**Applied fix:** Removed unused `import androidx.compose.runtime.collectAsState` line.

---

_Fixed: 2026-04-30T14:30:00Z_
_Fixer: OpenCode (gsd-code-fixer)_
_Iteration: 1_
