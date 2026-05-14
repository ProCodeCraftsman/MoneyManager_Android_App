---
status: testing
phase: 29-status-pane
source: [29-01-SUMMARY.md, 29-02-SUMMARY.md, 29-03-SUMMARY.md]
started: 2026-04-30T00:00:00Z
updated: 2026-04-30T00:00:00Z
---

## Current Test

number: 2
name: Status Pane Empty State
expected: |
  When there are no transactions recorded (fresh install or cleared data),
  the Status pane shows an empty state message "No financial activity recorded
  yet" instead of any figures or hero display.
awaiting: user response

## Tests

### 1. Status Pane Navigation
expected: Open the app and tap Insights in the bottom nav bar. The Insights screen opens with three tabs visible (Status, Risks, Trends). The Status tab is selected by default and its indicator/underline is highlighted.
result: pass

### 2. Status Pane Empty State
expected: When there are no transactions recorded (fresh install or cleared data), the Status pane shows an empty state message "No financial activity recorded yet" instead of any figures or hero display.
result: pass

### 3. Status Pane Hero Display
expected: With at least one transaction recorded, the Status pane shows a prominent "net position" hero value (income minus expenses) at the top. Positive net = displayed in primary/brand color; negative net = displayed in red/error color.
result: [pending]

### 4. Status Pane Figure Grid
expected: Below the hero, financial figures (income, expenses, etc.) appear in a 2-column grid layout. Items are evenly spaced with visible separation between columns.
result: [pending]

### 5. Currency Symbol Display
expected: All monetary values in the Status pane show a currency symbol (e.g., ₹ or $) rendered in the primary/brand color, not plain black/white.
result: [pending]

### 6. Tab Swipe Navigation
expected: While on the Status tab, swiping left transitions to the next pane (Risks). The tab indicator moves to highlight the Risks tab. Swiping right returns to Status.
result: [pending]

### 7. Tab Tap Navigation
expected: Tapping the second tab (Risks) or third tab (Trends) navigates to those panes. The tab indicator correctly highlights the tapped tab.
result: [pending]

## Summary

total: 7
passed: 1
issues: 0
skipped: 0
pending: 6

## Gaps

[none yet]
