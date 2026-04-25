# Phase 16: Theme Settings UI - Context

**Gathered:** 2026-04-25
**Status:** Ready for planning

<domain>
## Phase Boundary

Add theme selection UI and dark mode toggle to the existing Settings screen. Users can select from 5 themes and toggle dark mode, with immediate visual feedback.

</domain>

<decisions>
## Implementation Decisions

### Settings Layout
- **D-01:** Create "Appearance" section in Settings screen
- **D-02:** Theme dropdown and dark mode toggle both in Appearance section
- **D-03:** SettingsCard with dropdown for theme selection
- **D-04:** SettingsCard with Switch for dark mode toggle

### Theme Selection UI
- **D-05:** Use ExposedDropdownMenuBox for theme dropdown
- **D-06:** Show theme names using `AppTheme.displayName` property
- **D-07:** Current theme highlighted (selected) in dropdown
- **D-08:** Instant switch — colors update immediately on selection

### Dark Mode Toggle
- **D-09:** Switch component below theme dropdown
- **D-10:** When toggled, update dark mode AND set `hasUserSetTheme` to true
- **D-11:** Smart default preserved: new users get system theme, returning users keep preference

### Default Theme
- **D-12:** Default for new users: Soft Neutral
- **D-13:** Existing users keep their stored preference

### the agent's Discretion
- Exact UI placement within the Appearance section (before/after existing sections)
- Dropdown styling (colors, icons if any)
- Whether to add a preview of theme colors in dropdown options

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing Settings Code
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt` — Existing settings UI
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsViewModel.kt` — Settings ViewModel with darkMode state

### Theme Infrastructure
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/AppTheme.kt` — Theme enum with displayName
- `MoneyManager/app/src/main/java/com/moneymanager/data/preferences/PreferencesManager.kt` — setSelectedTheme(), setDarkMode(), setUserHasSetTheme()
- `.planning/phases/14-theme-infrastructure/14-CONTEXT.md` — Infrastructure decisions

### Requirements
- `.planning/REQUIREMENTS.md` — UI-01 to UI-05 (settings UI requirements)

### Roadmap
- `.planning/ROADMAP.md` — Phase 16 description and success criteria

[No external specs]

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `SettingsCard()` — Reusable card composable for settings items
- `SettingsClickableCard()` — For navigation items
- `SettingsViewModel.setDarkMode()` — Already exists
- `PreferencesManager.setSelectedTheme()` — Already exists
- `PreferencesManager.setUserHasSetTheme()` — For tracking manual changes

### Existing Dark Mode Toggle
- Line 237-257 in SettingsScreen.kt has existing dark mode toggle
- Uses `SettingsCard` with Switch component
- Calls `viewModel.setDarkMode()`

### Integration Points
- Add new Appearance section to SettingsScreen
- Update SettingsUiState to include selectedTheme
- Add setSelectedTheme() to SettingsViewModel

</code_context>

<specifics>
## Specific Ideas

- User wants simple selection list (dropdown), not visual theme cards
- Appearance section to group theme-related settings
- Dark mode toggle should set hasUserSetTheme to preserve smart default

</specifics>

<deferred>
## Deferred Ideas

### From Milestone
- Theme presets (save/load) — Phase backlog
- Visual theme preview cards — explicitly not wanted for this phase

None — Phase 16 scope is clear

</deferred>

---

*Phase: 16-theme-settings-ui*
*Context gathered: 2026-04-25*