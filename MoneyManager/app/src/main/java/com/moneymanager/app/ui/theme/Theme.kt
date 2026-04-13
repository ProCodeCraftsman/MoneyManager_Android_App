package com.moneymanager.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Ink = Color(0xFF0f0e0c)
val Paper = Color(0xFFf5f1ea)
val Cream = Color(0xFFede8df)
val WarmMid = Color(0xFFc8bfae)
val Accent = Color(0xFFc8420a)
val AccentLight = Color(0xFFf5ddd5)
val Accent2 = Color(0xFF2a6049)
val Accent2Light = Color(0xFFd4ebe1)
val Gold = Color(0xFFb8860b)
val GoldLight = Color(0xFFf5ecd0)
val Card = Color(0xFFfaf8f4)
val Border = Color(0xFFddd8ce)
val TextMuted = Color(0xFF7a7162)

val InkDark = Color(0xFFe8e3da)
val PaperDark = Color(0xFF141210)
val CreamDark = Color(0xFF1e1b17)
val WarmMidDark = Color(0xFF5a5448)
val CardDark = Color(0xFF1a1815)
val BorderDark = Color(0xFF2a2620)

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentLight,
    onPrimaryContainer = Accent,
    secondary = Accent2,
    onSecondary = Color.White,
    secondaryContainer = Accent2Light,
    onSecondaryContainer = Accent2,
    tertiary = Gold,
    onTertiary = Color.White,
    tertiaryContainer = GoldLight,
    onTertiaryContainer = Gold,
    background = Paper,
    onBackground = Ink,
    surface = Card,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = TextMuted,
    outline = Border,
    outlineVariant = Cream
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFe05a24),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF2d1a12),
    onPrimaryContainer = Color(0xFFe05a24),
    secondary = Color(0xFF3a8060),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF122a1e),
    onSecondaryContainer = Color(0xFF3a8060),
    tertiary = Color(0xFFc8960c),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF2a200a),
    onTertiaryContainer = Color(0xFFc8960c),
    background = PaperDark,
    onBackground = InkDark,
    surface = CardDark,
    onSurface = InkDark,
    surfaceVariant = CreamDark,
    onSurfaceVariant = Color(0xFF7a7060),
    outline = BorderDark,
    outlineVariant = CreamDark
)

@Composable
fun MoneyManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}