# Phase 14: Theme Infrastructure - Plan

**Phase:** 14-theme-infrastructure  
**Status:** Ready to execute  
**Context:** `14-CONTEXT.md` ✅ | **Research:** N/A (infrastructure phase)  
**Created:** 2026-04-25

---

## Phase Boundary

Establish the theme infrastructure for the multi-theme system:
- Theme data model (enum)
- DataStore persistence for theme + dark mode
- Startup theme application (no flicker)
- Integration with existing PreferencesManager

**Scope:** Foundation only. Actual theme color definitions are Phase 15.

---

## Implementation Tasks

### 14.1 Create AppTheme Enum

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/AppTheme.kt`

```kotlin
enum class AppTheme(val displayName: String) {
    SOFT_NEUTRAL("Soft Neutral"),
    WARM_FINANCE("Warm Finance"),
    COOL_BLUE("Cool Blue Finance"),
    GREEN_LEDGER("Minimal Green Ledger"),
    MODERN_MUTED("Modern Muted");

    companion object {
        fun fromString(value: String): AppTheme {
            return entries.find { it.name == value } ?: SOFT_NEUTRAL
        }
    }
}
```

**Verification:**
- [ ] File created with all 5 theme types
- [ ] `displayName` property for UI display
- [ ] `fromString()` fallback to SOFT_NEUTRAL

---

### 14.2 Extend PreferencesManager with Theme Preference

**File:** `MoneyManager/app/src/main/java/com/moneymanager/data/preferences/PreferencesManager.kt`

Add:
```kotlin
companion object {
    // ... existing keys ...
    private val SELECTED_THEME = stringPreferencesKey("selected_theme")
    private val HAS_USER_SET_THEME = booleanPreferencesKey("has_user_set_theme")
}

// New flows
val selectedTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
    AppTheme.fromString(preferences[SELECTED_THEME] ?: AppTheme.SOFT_NEUTRAL.name)
}

val hasUserSetTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[HAS_USER_SET_THEME] ?: false
}

// New setters
suspend fun setSelectedTheme(theme: AppTheme) {
    context.dataStore.edit { preferences ->
        preferences[SELECTED_THEME] = theme.name
    }
}

suspend fun setUserHasSetTheme() {
    context.dataStore.edit { preferences ->
        preferences[HAS_USER_SET_THEME] = true
    }
}
```

**Verification:**
- [ ] `selectedTheme` Flow returns correct AppTheme
- [ ] `hasUserSetTheme` tracks manual changes
- [ ] New setters update DataStore correctly
- [ ] Existing `darkMode` preference preserved

---

### 14.3 Create ThemePreferences State Holder

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/ThemePreferences.kt`

```kotlin
data class ThemeState(
    val theme: AppTheme = AppTheme.SOFT_NEUTRAL,
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = true
)

@Composable
fun rememberThemePreferences(): ThemeState {
    val preferencesManager = LocalContext.current.preferencesManager
    val systemDarkMode = isSystemInDarkTheme()
    
    val theme by preferencesManager.selectedTheme.collectAsState(initial = AppTheme.SOFT_NEUTRAL)
    val storedDarkMode by preferencesManager.darkMode.collectAsState(initial = false)
    val hasUserSetTheme by preferencesManager.hasUserSetTheme.collectAsState(initial = false)
    
    val isDarkMode = if (hasUserSetTheme) storedDarkMode else systemDarkMode
    val isLoading = false // Flows start immediately
    
    return ThemeState(theme, isDarkMode, isLoading)
}
```

**Verification:**
- [ ] Composable function available
- [ ] Smart default logic (system → stored) working
- [ ] No recomposition during load (initial values provided)

---

### 14.4 Update MoneyManagerTheme Composable

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/ui/theme/Theme.kt`

Modify signature:
```kotlin
@Composable
fun MoneyManagerTheme(
    appTheme: AppTheme = AppTheme.SOFT_NEUTRAL,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        isDarkMode -> getDarkColorScheme(appTheme)
        else -> getLightColorScheme(appTheme)
    }
    // ... existing status bar logic with isDarkMode
}
```

Add stub color scheme functions (actual colors in Phase 15):
```kotlin
private fun getLightColorScheme(theme: AppTheme): ColorScheme = LightColorScheme
private fun getDarkColorScheme(theme: AppTheme): ColorScheme = DarkColorScheme
```

**Verification:**
- [ ] Composable accepts appTheme parameter
- [ ] isDarkMode determines light/dark
- [ ] Status bars update with correct dark/light setting
- [ ] Existing LightColorScheme/DarkColorScheme used as fallback

---

### 14.5 Block Startup Until Theme Loaded

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/MainActivity.kt` (or Application)

Use blocking collect or snapshotFlow with timeout:

```kotlin
@Composable
fun MoneyManagerApp() {
    val themeState = rememberThemePreferences()
    
    // Simple blocking approach - flows emit immediately on cold start
    MoneyManagerTheme(
        appTheme = themeState.theme,
        isDarkMode = themeState.isDarkMode
    ) {
        MainContent()
    }
}
```

For true blocking (if needed):
```kotlin
val preferencesManager = remember { context.preferencesManager }
val themeState by produceState(
    initialValue = ThemeState(),
    producer = {
        combine(
            preferencesManager.selectedTheme,
            preferencesManager.darkMode,
            preferencesManager.hasUserSetTheme
        ) { theme, darkMode, hasSet ->
            ThemeState(
                theme = theme,
                isDarkMode = if (hasSet) darkMode else isSystemDarkMode,
                isLoading = false
            )
        }.first().also { value = it }
    }
)
```

**Verification:**
- [ ] Theme applied before first frame
- [ ] No flicker visible
- [ ] Works on cold start
- [ ] Works on hot reload

---

### 14.6 Update All Theme Usages

**File:** `MoneyManager/app/src/main/java/com/moneymanager/app/MainActivity.kt`

Find and update:
```kotlin
MoneyManagerTheme(
    appTheme = currentTheme,
    isDarkMode = isDarkMode
) {
    // existing content
}
```

**Verification:**
- [ ] All MoneyManagerTheme calls updated
- [ ] No hardcoded `darkTheme` parameter

---

## Verification Checklist

| Criterion | Method |
|-----------|--------|
| THEM-01: Material 3 dynamic color | Code review: ColorScheme used |
| THEM-02: Theme colors defined | Code review: getLight/DarkColorScheme functions exist |
| THEM-03: DataStore persistence | Test: change theme, restart, verify persists |
| THEM-04: Dark mode applies | Test: toggle dark mode, verify color change |
| THEM-05: Startup application | Visual: no flicker on cold start |

---

## Success Criteria

1. **Material 3 color theming** — App uses Material 3 ColorScheme for all theme colors
2. **Theme enum exists** — AppTheme enum with all 5 themes
3. **DataStore persistence** — Theme + dark mode persist across restarts
4. **Smart default** — New users get system theme, returning users keep preference
5. **No flicker** — Theme applied before first frame visible

---

## Dependencies

- Phase 13 (v2.0) — provides existing codebase structure
- No new external dependencies

## Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Startup delay from blocking | Low | Medium | Flows emit immediately, no actual delay |
| Backward compatibility | Low | High | Keep existing darkMode key |
| Flicker on hot reload | Medium | Low | Acceptable for development |

---

*Plan created: 2026-04-25*  
*Phase: 14-theme-infrastructure*