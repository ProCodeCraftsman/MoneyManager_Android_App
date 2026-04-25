package com.moneymanager.app.ui.util

import androidx.compose.ui.graphics.Color

fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}

fun generateDistinctColor(seed: Int): Color {
    val hues = listOf(
        0f, 30f, 60f, 90f, 120f, 150f, 180f, 210f, 240f, 270f, 300f, 330f
    )
    val hue = hues[seed % hues.size]
    return Color.hsl(hue, 0.7f, 0.6f)
}
