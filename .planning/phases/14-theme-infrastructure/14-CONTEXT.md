# Phase 14: Theme Infrastructure - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Establish the theme infrastructure for the multi-theme system. This includes:
- Theme data model (sealed class/enum)
- DataStore persistence for theme selection and dark mode
- Startup theme application (no flicker)
- Integration with existing PreferencesManager

This phase delivers the foundation. Actual theme color schemes (THEM-06 to THEM-15) are Phase 15.

</domain>

<decisions>
## Implementation Decisions

### Theme Data Model
- **D-01:** Use sealed class or enum for theme types — type-safe, IDE support, future-proof
- **D-02:** Track `isDarkMode: Boolean` separately from theme selection
- **D-03:** Theme types: SOFT_NEUTRAL, WARM_FINANCE, COOL_BLUE, GREEN_LEDGER, MODERN_MUTED

### DataStore Structure
- **D-04:** Extend existing PreferencesManager with `selectedTheme` preference key
- **D-05:** Store theme as string using enum name for serialization
- **D-06:** Maintain backward compatibility with existing `darkMode` boolean

### Startup Behavior
- **D-07:** Block startup until theme preference is loaded from DataStore
- **D-08:** No visual flicker on theme application
- **D-09:** Apply theme before first composition

### System Theme Handling
- **D-10:** Smart default: respect system theme for new users (first launch)
- **D-11:** After user manually toggles dark mode, use stored preference
- **D-12:** Track `hasUserSetTheme` preference to determine "smart default" vs stored

### the agent's Discretion
- Exact implementation of blocking startup (splash screen vs lazy load)
- State hoisting approach for theme preferences (ViewModel vs direct DataStore access)
- Whether to use SharedPreferences initially then migrate to DataStore

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing Theme Code
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/Theme.kt` — Current theme implementation
- `MoneyManager/app/src/main/java/com/moneymanager/data/preferences/PreferencesManager.kt` — Existing DataStore setup

### Requirements
- `.planning/REQUIREMENTS.md` — THEM-01 to THEM-05 are this phase's scope

### Roadmap
- `.planning/ROADMAP.md` — Phase 14 description and success criteria

[No external specs — requirements captured in decisions above]

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MoneyManagerTheme()` composable — Already handles status bar/navigation bar colors
- `PreferencesManager` — Already has DataStore infrastructure, dark mode preference

### Established Patterns
- Dark/Light color schemes are defined as `LightColorScheme` and `DarkColorScheme` in Theme.kt
- `isSystemInDarkTheme()` used for system default

### Integration Points
- Theme will need to be applied in MainActivity or Application class
- Settings screen will read/write theme preference (Phase 16)
- All UI composables use `MaterialTheme.colorScheme`

</code_context>

<specifics>
## Specific Ideas

- User wants Soft Neutral as default theme for new users
- Smart default behavior: new users see system theme, existing users keep their preference
- Block startup to prevent flicker — this is acceptable for this app

</specifics>

<deferred>
## Deferred Ideas

### From Milestone
- Theme presets (save/load theme configurations) — Phase backlog
- Auto theme (system follows device setting) — v2 requirements
- AMOLED themes — out of scope for v2.1

None — discussion stayed within Phase 14 scope

</deferred>

---

*Phase: 14-theme-infrastructure*
*Context gathered: 2026-04-25*