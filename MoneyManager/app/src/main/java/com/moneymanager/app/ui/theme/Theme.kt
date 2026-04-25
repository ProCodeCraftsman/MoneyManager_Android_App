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

val ColorScheme.incomeColor: Color get() = secondary
val ColorScheme.expenseColor: Color get() = error
val ColorScheme.savingsColor: Color get() = tertiary
val ColorScheme.transferColor: Color get() = primary
val ColorScheme.lendingColor: Color get() = primaryContainer
val ColorScheme.borrowingColor: Color get() = errorContainer

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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            @Suppress("DEPRECATION")
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkMode
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

private fun getLightColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {
        AppTheme.SOFT_NEUTRAL -> softNeutralLight
        AppTheme.WARM_FINANCE -> warmFinanceLight
        AppTheme.COOL_BLUE -> coolBlueLight
        AppTheme.GREEN_LEDGER -> greenLedgerLight
        AppTheme.MODERN_MUTED -> modernMutedLight
    }
}

private fun getDarkColorScheme(theme: AppTheme): ColorScheme {
    return when (theme) {
        AppTheme.SOFT_NEUTRAL -> softNeutralDark
        AppTheme.WARM_FINANCE -> warmFinanceDark
        AppTheme.COOL_BLUE -> coolBlueDark
        AppTheme.GREEN_LEDGER -> greenLedgerDark
        AppTheme.MODERN_MUTED -> modernMutedDark
    }
}

// Theme 1: Soft Neutral (Default)
private val softNeutralLight = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF16A34A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF002106),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF400101),
    background = Color(0xFFF6F7F9),
    onBackground = Color(0xFF1F2937),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1F2937),
    surfaceVariant = Color(0xFFE5E7EB),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFE5E7EB),
    outlineVariant = Color(0xFFE5E7EB),
    inverseSurface = Color(0xFF1F2937),
    inverseOnSurface = Color(0xFFF9FAFB),
    inversePrimary = Color(0xFF93C5FD),
    surfaceTint = Color(0xFF2563EB)
)

private val softNeutralDark = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF22C55E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF166534),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF064E3B),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFF87171),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF334155),
    outlineVariant = Color(0xFF334155),
    inverseSurface = Color(0xFFE5E7EB),
    inverseOnSurface = Color(0xFF1F2937),
    inversePrimary = Color(0xFF2563EB),
    surfaceTint = Color(0xFF3B82F6)
)

// Theme 2: Warm Finance
private val warmFinanceLight = lightColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF2E7D32),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFFE8F5E9),
    onTertiaryContainer = Color(0xFF002106),
    error = Color(0xFFC62828),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF400101),
    background = Color(0xFFFAF7F2),
    onBackground = Color(0xFF2B2B2B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2B2B2B),
    surfaceVariant = Color(0xFFE8E3DA),
    onSurfaceVariant = Color(0xFF7A7A7A),
    outline = Color(0xFFE8E3DA),
    outlineVariant = Color(0xFFE8E3DA),
    inverseSurface = Color(0xFF2B2B2B),
    inverseOnSurface = Color(0xFFFAF7F2),
    inversePrimary = Color(0xFFFCD34D),
    surfaceTint = Color(0xFFF59E0B)
)

private val warmFinanceDark = darkColorScheme(
    primary = Color(0xFFF59E0B),
    onPrimary = Color(0xFF422006),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF166534),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF052E16),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFFB7185),
    onError = Color(0xFF4C0519),
    errorContainer = Color(0xFF9F1239),
    onErrorContainer = Color(0xFFFFE4E6),
    background = Color(0xFF1C1917),
    onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF292524),
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF44403C),
    onSurfaceVariant = Color(0xFFA8A29E),
    outline = Color(0xFF44403C),
    outlineVariant = Color(0xFF44403C),
    inverseSurface = Color(0xFFF5F5F4),
    inverseOnSurface = Color(0xFF1C1917),
    inversePrimary = Color(0xFFF59E0B),
    surfaceTint = Color(0xFFF59E0B)
)

// Theme 3: Cool Blue Finance
private val coolBlueLight = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF002106),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF400101),
    background = Color(0xFFF4F8FF),
    onBackground = Color(0xFF1E3A8A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E3A8A),
    surfaceVariant = Color(0xFFDBEAFE),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFDBEAFE),
    outlineVariant = Color(0xFFDBEAFE),
    inverseSurface = Color(0xFF1E3A8A),
    inverseOnSurface = Color(0xFFF4F8FF),
    inversePrimary = Color(0xFF93C5FD),
    surfaceTint = Color(0xFF2563EB)
)

private val coolBlueDark = darkColorScheme(
    primary = Color(0xFF3B82F6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = Color(0xFF34D399),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF065F46),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFF87171),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF020617),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF0F172A),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E3A8A),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E3A8A),
    outlineVariant = Color(0xFF1E3A8A),
    inverseSurface = Color(0xFFE2E8F0),
    inverseOnSurface = Color(0xFF020617),
    inversePrimary = Color(0xFF2563EB),
    surfaceTint = Color(0xFF3B82F6)
)

// Theme 4: Minimal Green Ledger
private val greenLedgerLight = lightColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF022C22),
    secondary = Color(0xFF16A34A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF002106),
    error = Color(0xFFB91C1C),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF400101),
    background = Color(0xFFF3FBF6),
    onBackground = Color(0xFF064E3B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF064E3B),
    surfaceVariant = Color(0xFFD1FAE5),
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1FAE5),
    outlineVariant = Color(0xFFD1FAE5),
    inverseSurface = Color(0xFF064E3B),
    inverseOnSurface = Color(0xFFF3FBF6),
    inversePrimary = Color(0xFF6EE7B7),
    surfaceTint = Color(0xFF10B981)
)

private val greenLedgerDark = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF022C22),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFF6EE7B7),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF166534),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFF87171),
    onError = Color.White,
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    background = Color(0xFF022C22),
    onBackground = Color(0xFFECFDF5),
    surface = Color(0xFF064E3B),
    onSurface = Color(0xFFECFDF5),
    surfaceVariant = Color(0xFF064E3B),
    onSurfaceVariant = Color(0xFFA7F3D0),
    outline = Color(0xFF064E3B),
    outlineVariant = Color(0xFF064E3B),
    inverseSurface = Color(0xFFECFDF5),
    inverseOnSurface = Color(0xFF022C22),
    inversePrimary = Color(0xFF10B981),
    surfaceTint = Color(0xFF10B981)
)

// Theme 5: Modern Muted
private val modernMutedLight = lightColorScheme(
    primary = Color(0xFF7C3AED),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = Color(0xFF22C55E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002106),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF002106),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF400101),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    outlineVariant = Color(0xFFE2E8F0),
    inverseSurface = Color(0xFF0F172A),
    inverseOnSurface = Color(0xFFF8FAFC),
    inversePrimary = Color(0xFFC4B5FD),
    surfaceTint = Color(0xFF7C3AED)
)

private val modernMutedDark = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF166534),
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = Color(0xFF86EFAC),
    onTertiary = Color(0xFF022C22),
    tertiaryContainer = Color(0xFF14532D),
    onTertiaryContainer = Color(0xFFD1FAE5),
    error = Color(0xFFFB7185),
    onError = Color.White,
    errorContainer = Color(0xFF9F1239),
    onErrorContainer = Color(0xFFFFE4E6),
    background = Color(0xFF020617),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E293B),
    outlineVariant = Color(0xFF1E293B),
    inverseSurface = Color(0xFFF1F5F9),
    inverseOnSurface = Color(0xFF020617),
    inversePrimary = Color(0xFF7C3AED),
    surfaceTint = Color(0xFF8B5CF6)
)