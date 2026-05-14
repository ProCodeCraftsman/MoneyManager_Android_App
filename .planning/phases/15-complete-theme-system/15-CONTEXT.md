# Phase 15: Complete Theme System - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Define actual color palettes for all 5 themes (light + dark variants). Each theme maps hex colors to Material 3 ColorScheme properties. This phase delivers the visual theming — infrastructure is Phase 14.

</domain>

<decisions>
## Implementation Decisions

### Theme Color Structure
- **D-01:** Each theme has dedicated light and dark ColorScheme
- **D-02:** ColorScheme maps: background, surface (card), onSurface (primary text), onSurfaceVariant (secondary text), income color, expense color, accent color, outline/divider
- **D-03:** Use helper functions to build ColorScheme per theme per mode

### Theme 1: Soft Neutral (Default)
- **D-04:** Light: Background #F6F7F9, Card #FFFFFF, Text #1F2937/#6B7280, Income #16A34A, Expense #DC2626, Accent #2563EB, Divider #E5E7EB
- **D-05:** Dark: Background #0F172A, Card #1E293B, Text #E5E7EB/#94A3B8, Income #22C55E, Expense #F87171, Accent #3B82F6

### Theme 2: Warm Finance
- **D-06:** Light: Background #FAF7F2, Card #FFFFFF, Text #2B2B2B/#7A7A7A, Income #2E7D32, Expense #C62828, Accent #F59E0B, Divider #E8E3DA
- **D-07:** Dark: Background #1C1917, Card #292524, Text #F5F5F4/#A8A29E, Income #4ADE80, Expense #FB7185, Accent #F59E0B

### Theme 3: Cool Blue Finance
- **D-08:** Light: Background #F4F8FF, Card #FFFFFF, Text #1E3A8A/#64748B, Income #059669, Expense #DC2626, Accent #2563EB, Divider #DBEAFE
- **D-09:** Dark: Background #020617, Card #0F172A, Text #E2E8F0/#94A3B8, Income #34D399, Expense #F87171, Accent #3B82F6

### Theme 4: Minimal Green Ledger
- **D-10:** Light: Background #F3FBF6, Card #FFFFFF, Text #064E3B/#6B7280, Income #16A34A, Expense #B91C1C, Accent #10B981, Divider #D1FAE5
- **D-11:** Dark: Background #022C22, Card #064E3B, Text #ECFDF5/#A7F3D0, Income #4ADE80, Expense #F87171, Accent #10B981

### Theme 5: Modern Muted
- **D-12:** Light: Background #F8FAFC, Card #FFFFFF, Text #0F172A/#64748B, Income #22C55E, Expense #EF4444, Accent #7C3AED, Divider #E2E8F0
- **D-13:** Dark: Background #020617, Card #111827, Text #F1F5F9/#94A3B8, Income #4ADE80, Expense #FB7185, Accent #8B5CF6

### Theme Switching Behavior
- **D-14:** Instant switch — colors update immediately when user selects new theme
- **D-15:** No restart required
- **D-16:** No animated transition (simplicity preferred)

### the agent's Discretion
- Exact function signatures for color scheme builders
- Whether to use extension functions or direct ColorScheme construction
- Helper color mappings (income/expense to tertiary/primary vs custom)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing Theme Infrastructure
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/AppTheme.kt` — Theme enum with 5 types
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/Theme.kt` — Current stub color scheme functions
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/ThemePreferences.kt` — Theme state holder

### Phase 14 Context
- `.planning/phases/14-theme-infrastructure/14-CONTEXT.md` — Infrastructure decisions

### Requirements
- `.planning/REQUIREMENTS.md` — THEM-06 to THEM-15 (color definitions)

### Roadmap
- `.planning/ROADMAP.md` — Phase 15 description and success criteria

[No external specs — all color values provided by user during milestone discussion]

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `getLightColorScheme(appTheme)` — Already exists as stub in Theme.kt
- `getDarkColorScheme(appTheme)` — Already exists as stub in Theme.kt
- `MoneyManagerTheme()` — Already accepts `appTheme` and `isDarkMode` parameters

### Established Patterns
- Color defined as `Color(0xFFXXXXXX)` format
- `lightColorScheme()` and `darkColorScheme()` from Material 3
- Status bar colors handled in MoneyManagerTheme

### Integration Points
- Color changes propagate via `MaterialTheme.colorScheme` to all composables
- Income/expense coloring (Phase 17) will reference theme income/expense colors

</code_context>

<specifics>
## Specific Ideas

- User provided exact hex values for all themes
- Soft Neutral is default for new users
- User's own recommendation: "Start with Soft Neutral + Warm Finance, they balance readability, comfort, trust, long-term usability"
- All 5 themes should be implemented in this phase

</specifics>

<deferred>
## Deferred Ideas

### From Milestone
- Theme presets (save/load) — Phase 16+ backlog
- AMOLED pure black themes — out of scope
- Theme switching animations — explicitly not wanted

None — all themes implemented in this phase

</deferred>

---

*Phase: 15-complete-theme-system*
*Context gathered: 2026-04-25*