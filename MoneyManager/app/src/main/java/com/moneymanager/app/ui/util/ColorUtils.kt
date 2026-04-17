package com.moneymanager.app.ui.util

import androidx.compose.ui.graphics.Color

fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}
