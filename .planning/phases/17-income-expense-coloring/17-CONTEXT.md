# Phase 17: Income/Expense Coloring - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Replace all hardcoded income/expense colors with theme-aware colors. All income displays should use theme's income color, all expense displays should use theme's expense color. This includes amounts, icons, labels, and visual indicators.

</domain>

<decisions>
## Implementation Decisions

### Color Strategy
- **D-01:** Income color = `colorScheme.secondary` (already defined in all themes)
- **D-02:** Expense color = `colorScheme.error` (already defined in all themes)
- **D-03:** No new color properties needed — use existing Material 3 color roles

### Hardcoded Color Replacements
- **D-04:** Replace all `Color(0xFF00C853)` (green) with `colorScheme.secondary`
- **D-05:** Replace all `Color(0xFF4CAF50)` with `colorScheme.secondary`
- **D-06:** Keep theme-aware color references (colorScheme.error, colorScheme.secondary) as-is

### Scope
- **D-07:** Replace colors in ALL screens (Dashboard, Transactions, Reports, Accounts, etc.)
- **D-08:** Replace colors in ALL components (TrendLineChart, RemindersWidget, etc.)
- **D-09:** Preserve other hardcoded colors that are NOT income/expense related

### the agent's Discretion
- Exact replacement patterns (exact match vs regex)
- Any edge cases where theme colors shouldn't apply
- Ordering of file modifications

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Theme Definitions
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/Theme.kt` — All theme color schemes

### Code with Hardcoded Colors
- `DashboardScreen.kt` — Multiple Color(0xFF00C853) references
- `TransactionsScreen.kt` — Color(0xFF4CAF50) reference
- `TrendLineChart.kt` — expenseColor = colorScheme.error (already theme-aware)
- `RemindersWidget.kt` — Uses colorScheme.secondary/error (already theme-aware)

### Requirements
- `.planning/REQUIREMENTS.md` — COL-01 to COL-04

### Roadmap
- `.planning/ROADMAP.md` — Phase 17 description

[No external specs]

</canonical_refs>

<code_context>
## Existing Code Insights

### Already Theme-Aware
- `colorScheme.secondary` — income color in all themes
- `colorScheme.error` — expense color in all themes
- Most screens already use these correctly

### Hardcoded Colors to Replace
| Color | Files | Use |
|-------|-------|-----|
| `Color(0xFF00C853)` | DashboardScreen.kt (14 instances) | Income/positive indicators |
| `Color(0xFF4CAF50)` | TransactionsScreen.kt (1 instance) | Income/positive indicators |
| `Color(0xFFFF9800)` | DashboardScreen.kt (1 instance) | Warning, keep as-is |

### Integration Points
- Replace inline color references in composable functions
- No API changes needed
- Theme colors automatically update when user switches themes

</code_context>

<specifics>
## Specific Ideas

- User wants ALL hardcoded income/expense colors replaced
- Green color #00C853 should become theme secondary color
- This ensures consistency when themes change

</specifics>

<deferred>
## Deferred Ideas

None — Phase 17 is final phase of v2.1 milestone

</deferred>

---

*Phase: 17-income-expense-coloring*
*Context gathered: 2026-04-25*