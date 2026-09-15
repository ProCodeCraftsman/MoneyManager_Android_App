package com.moneymanager.app.ui.util

import androidx.compose.ui.graphics.Color

fun parseColor(colorString: String, fallbackColor: Color = Color(0xFF90A4AE)): Color {
    return try {
        val color = android.graphics.Color.parseColor(colorString)
        Color(color)
    } catch (e: Exception) {
        fallbackColor
    }
}
