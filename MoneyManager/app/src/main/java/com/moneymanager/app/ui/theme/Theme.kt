// Theme.kt
package com.moneymanager.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

data class CategoryColors(
    val expense: Color,
    val income: Color,
    val transfer: Color,
    val lending: Color,
    val savings: Color
)

@Composable
fun MoneyManagerTheme(
    appTheme: AppTheme = AppTheme.MIDNIGHT_BLUE,
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {

    val colorScheme = when {
        isDarkMode -> getDarkColorScheme(appTheme)
        else -> getLightColorScheme(appTheme)
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()

            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()

            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightStatusBars = !isDarkMode

            WindowCompat
                .getInsetsController(window, view)
                .isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    val categoryColors = when (appTheme) {
        AppTheme.CALM_GREEN -> calmGreenCategoryColors
        AppTheme.COCO_BROWN -> cocoBrownCategoryColors
        AppTheme.MIDNIGHT_BLUE -> midnightBlueCategoryColors
    }

    CompositionLocalProvider(LocalCategoryColors provides categoryColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private fun getLightColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {

        AppTheme.COCO_BROWN -> cocoBrownLight
        AppTheme.CALM_GREEN -> calmGreenLight
        AppTheme.MIDNIGHT_BLUE -> midnightBlueLight
    }
}

private fun getDarkColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {

        AppTheme.COCO_BROWN -> cocoBrownDark
        AppTheme.CALM_GREEN -> calmGreenDark
        AppTheme.MIDNIGHT_BLUE -> midnightBlueDark
    }
}

/* ------------------------------------------------ */
/* CALM GREEN */
/* ------------------------------------------------ */

private val calmGreenCategoryColors = CategoryColors(
    expense = Color(0xFFE5484D),  // soft red
    income = Color(0xFF30A46C),   // vivid green
    transfer = Color(0xFF3B82F6), // bright blue
    lending = Color(0xFF8E4EC6),  // purple
    savings = Color(0xFFF5D90E)   // gold
)

private val calmGreenLight = lightColorScheme(
    primary = Color(0xFF16A34A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),

    secondary = Color(0xFF2563EB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDBEAFE),
    onSecondaryContainer = Color(0xFF1E3A8A),

    tertiary = Color(0xFF14B8A6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF134E4A),

    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),

    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF18181B),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18181B),

    surfaceVariant = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFF52525B),

    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFD1D5DB),

    inverseSurface = Color(0xFF18181B),
    inverseOnSurface = Color(0xFFF5F5F7),

    surfaceTint = Color(0xFF16A34A)
)


private val calmGreenDark = darkColorScheme(
    /* Green: softer, pastel‑like primary */
    primary = Color(0xFF86EFAC),
    onPrimary = Color(0xFF14532D),
    primaryContainer = Color(0xFF1B3D2B),
    onPrimaryContainer = Color(0xFFBBF7D0),

    /* Blue: light, airy secondary */
    secondary = Color(0xFF93C5FD),
    onSecondary = Color(0xFF1E3A8A),
    secondaryContainer = Color(0xFF1E3A8A),
    onSecondaryContainer = Color(0xFFDBEAFE),

    /* Teal: gentle tertiary */
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF134E4A),
    tertiaryContainer = Color(0xFF134E4A),
    onTertiaryContainer = Color(0xFFCCFBF1),

    /* Red: softened error */
    error = Color(0xFFFCA5A5),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),

    /* Deep, calm green‑black backgrounds */
    background = Color(0xFF0F1A14),
    onBackground = Color(0xFFE2E3E0),

    surface = Color(0xFF151B17),
    onSurface = Color(0xFFE2E3E0),

    surfaceVariant = Color(0xFF2A332D),
    onSurfaceVariant = Color(0xFFC1C9C3),

    outline = Color(0xFF8B938B),
    outlineVariant = Color(0xFF3F4741),

    inverseSurface = Color(0xFFE2E3E0),
    inverseOnSurface = Color(0xFF121714),

    surfaceTint = Color(0xFF86EFAC)
)

/* ------------------------------------------------ */
/* Coco Brown */
/* ------------------------------------------------ */
private val cocoBrownCategoryColors = CategoryColors(
    expense = Color(0xFFC2410C),  // burnt orange‑red
    income = Color(0xFF5F9C6B),   // muted sage green
    transfer = Color(0xFF4B8BBE), // dusty blue
    lending = Color(0xFFA162BF),  // soft purple
    savings = Color(0xFFE9A23B)   // warm amber/gold
)

private val cocoBrownLight = lightColorScheme(
    primary = Color(0xFF7A4E3A), // warm brown tone ~40-ish
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFBE6D2), // same as dark onPrimaryContainer
    onPrimaryContainer = Color(0xFF35200C),

    secondary = Color(0xFF8C5A4B), // warm terracotta
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCE), // from dark onSecondaryContainer
    onSecondaryContainer = Color(0xFF4F2E1F),

    tertiary = Color(0xFF8A4D60), // muted pink
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD9E2), // from dark onTertiaryContainer
    onTertiaryContainer = Color(0xFF4C1E2B),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    background = Color(0xFFFBF6F0),
    onBackground = Color(0xFF201A15),

    surface = Color(0xFFFBF6F0),
    onSurface = Color(0xFF201A15),

    surfaceVariant = Color(0xFFE7DDD2),
    onSurfaceVariant = Color(0xFF4F4439),

    outline = Color(0xFF837A6F),
    outlineVariant = Color(0xFFD4C9BD),

    inverseSurface = Color(0xFF35200C),
    inverseOnSurface = Color(0xFFFBF6F0),

    surfaceTint = Color(0xFF7A4E3A)
)

private val cocoBrownDark = darkColorScheme(
    primary = Color(0xFFDDB892),
    onPrimary = Color(0xFF35200C),
    primaryContainer = Color(0xFF543C28),
    onPrimaryContainer = Color(0xFFFBE6D2),

    secondary = Color(0xFFF0BFA9),
    onSecondary = Color(0xFF4F2E1F),
    secondaryContainer = Color(0xFF6F4735),
    onSecondaryContainer = Color(0xFFFFDBCE),

    tertiary = Color(0xFFEDA4B7),
    onTertiary = Color(0xFF4C1E2B),
    tertiaryContainer = Color(0xFF663644),
    onTertiaryContainer = Color(0xFFFFD9E2),

    error = Color(0xFFFFB4A8),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = Color(0xFF2E2723),
    onBackground = Color(0xFFEDE0D5),

    surface = Color(0xFF211C19),
    onSurface = Color(0xFFEDE0D5),

    surfaceVariant = Color(0xFF534A43),
    onSurfaceVariant = Color(0xFFD1C5BB),

    outline = Color(0xFFA0968E),
    outlineVariant = Color(0xFF3E3832),

    inverseSurface = Color(0xFFEDE0D5),
    inverseOnSurface = Color(0xFF1A1614),

    surfaceTint = Color(0xFFDDB892)
)

/* ------------------------------------------------ */
/* MIDNIGHT BLUE */
/* ------------------------------------------------ */

private val midnightBlueCategoryColors = CategoryColors(
    expense = Color(0xFFF87171),  // light red
    income = Color(0xFF4ADE80),   // fresh green
    transfer = Color(0xFF60A5FA), // sky blue
    lending = Color(0xFFA78BFA),  // periwinkle purple
    savings = Color(0xFFFBBF24)   // amber yellow
)

private val midnightBlueLight = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),

    secondary = Color(0xFF0F766E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF134E4A),

    tertiary = Color(0xFF14B8A6),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFE6FFFA),
    onTertiaryContainer = Color(0xFF115E59),

    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),

    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),

    surfaceVariant = Color(0xFFEFF6FF),
    onSurfaceVariant = Color(0xFF64748B),

    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFD1D5DB),

    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),

    surfaceTint = Color(0xFF2563EB)
)


private val midnightBlueDark = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),

    secondary = Color(0xFF14B8A6),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF134E4A),
    onSecondaryContainer = Color(0xFFCCFBF1),

    tertiary = Color(0xFF22C55E),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),

    error = Color(0xFFFB7185),
    onError = Color.Black,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),

    background = Color(0xFF0B1220),
    onBackground = Color(0xFFF8FAFC),

    surface = Color(0xFF111827),
    onSurface = Color(0xFFF8FAFC),

    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),

    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF475569),

    inverseSurface = Color(0xFFF8FAFC),
    inverseOnSurface = Color(0xFF0B1220),

    surfaceTint = Color(0xFF3B82F6)
)

// Place after your existing ColorScheme definitions
val LocalCategoryColors = staticCompositionLocalOf {
    CategoryColors(
        expense = Color.Unspecified,
        income = Color.Unspecified,
        transfer = Color.Unspecified,
        lending = Color.Unspecified,
        savings = Color.Unspecified
    )
}