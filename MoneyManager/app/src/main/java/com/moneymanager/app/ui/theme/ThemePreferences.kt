// ThemePreferences.kt
package com.moneymanager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.moneymanager.data.preferences.PreferencesManager

data class ThemeState(
    val theme: AppTheme = AppTheme.MIDNIGHT_BLUE,
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = true
)

@Composable
fun rememberThemePreferences(): ThemeState {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    val systemDarkMode = isSystemInDarkTheme()

    val theme by preferencesManager.selectedTheme.collectAsState(
        initial = AppTheme.MIDNIGHT_BLUE
    )

    val storedDarkMode by preferencesManager.darkMode.collectAsState(
        initial = false
    )

    val hasUserSetTheme by preferencesManager.hasUserSetTheme.collectAsState(
        initial = false
    )

    val effectiveDarkMode =
        if (hasUserSetTheme) storedDarkMode else systemDarkMode

    return ThemeState(
        theme = theme,
        isDarkMode = effectiveDarkMode,
        isLoading = false
    )
}