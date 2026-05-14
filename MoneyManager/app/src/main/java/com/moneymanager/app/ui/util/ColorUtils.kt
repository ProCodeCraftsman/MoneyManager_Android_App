package com.moneymanager.app.ui.util

import androidx.compose.ui.graphics.Color

val pieChartPalette = listOf(
    Color(0xFF2563EB),  // Blue   — cool
    Color(0xFFD97706),  // Amber  — warm
    Color(0xFF059669),  // Emerald— cool
    Color(0xFFDB2777),  // Pink   — warm
    Color(0xFF7C3AED),  // Violet — cool
    Color(0xFFEA580C),  // Orange — warm
    Color(0xFF0891B2),  // Cyan   — cool
    Color(0xFF65A30D),  // Lime   — warm
    Color(0xFFDC2626),  // Red    — warm
    Color(0xFF4F46E5),  // Indigo — cool
)

fun parseColor(colorString: String, fallbackColor: Color = Color(0xFF90A4AE)): Color {
    return try {
        val color = android.graphics.Color.parseColor(colorString)
        Color(color)
    } catch (e: Exception) {
        fallbackColor
    }
}

fun generateDistinctColor(seed: Int): Color {
    return pieChartPalette[seed % pieChartPalette.size]
}
