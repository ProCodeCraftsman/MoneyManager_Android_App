---
phase: 29-status-pane
reviewed: 2026-04-30T12:00:00Z
depth: standard
files_reviewed: 6
files_reviewed_list:
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneEmptyState.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneFigure.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneFigureGrid.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneHero.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneScreen.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsScreen.kt
findings:
  critical: 1
  warning: 1
  info: 1
  total: 3
status: issues_found
---

# Phase 29: Code Review Report

**Reviewed:** 2026-04-30T12:00:00Z
**Depth:** standard
**Files Reviewed:** 6
**Status:** issues_found

## Summary

Reviewed 6 Kotlin/Jetpack Compose files from phase 29 (status-pane), including 4 stateless composables, 1 screen-level composable with ViewModel integration, and the parent InsightsScreen integration. Found 1 critical scrolling conflict issue, 1 locale-sensitive formatting warning, and 1 unused import.

## Critical Issues

### CR-01: Nested scrolling conflict between Column and LazyVerticalGrid in StatusPaneScreen

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneScreen.kt:29-64`
**Issue:** The parent `Column` uses `verticalScroll` to enable full-pane scrolling, but contains `StatusPaneFigureGrid` which uses `LazyVerticalGrid` (a self-scrolling lazy component). This causes scrolling conflicts: the `LazyVerticalGrid` will not lazy-load items properly, and scroll events may not be handled correctly, leading to broken UI behavior. Additionally, `LazyVerticalGrid` is unnecessary here since the grid only displays 6 fixed items, so lazy loading provides no benefit.
**Fix:**
```kotlin
// In StatusPaneFigureGrid.kt, replace LazyVerticalGrid with a non-lazy 2-column layout:
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.weight

@Composable
fun StatusPaneFigureGrid(
    figures: List<StatusFigure>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        figures.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { figure ->
                    StatusPaneFigure(
                        label = figure.label,
                        value = figure.value,
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if row has only 1 item
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
```

## Warnings

### WR-01: Locale-sensitive currency formatting causes incorrect decimal separators

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneHero.kt:56`
**Issue:** `"%.2f".format(netPosition)` uses the default system locale, which may use comma (`,`) as a decimal separator (common in European locales). This leads to incorrect currency display (e.g., "1,234.56" becomes "1,234,56" in de-DE locale).
**Additional affected file:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/insights/status/StatusPaneFigure.kt:48` (same issue with `value` formatting).
**Fix:** Use `String.format(Locale.US, "%.2f", value)` to force dot as decimal separator for currency display:
```kotlin
// Add import to both files:
import java.util.Locale

// For StatusPaneHero.kt line 56:
text = String.format(Locale.US, "%.2f", netPosition),

// For StatusPaneFigure.kt line 48:
text = String.format(Locale.US, "%.2f", value),
```

## Info

### IN-01: Unused import in InsightsScreen.kt

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/InsightsScreen.kt:14`
**Issue:** The import `import androidx.compose.runtime.collectAsState` is unused, as the file uses `collectAsStateWithLifecycle` from `androidx.lifecycle.compose` instead.
**Fix:** Remove the unused import line 14.

---

_Reviewed: 2026-04-30T12:00:00Z_
_Reviewer: OpenCode (gsd-code-reviewer)_
_Depth: standard_
