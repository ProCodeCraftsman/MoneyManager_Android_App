package com.moneymanager.app.ui.components

import androidx.compose.ui.graphics.Color

data class PieChartEntry(
    val label: String,
    val value: Double,
    val color: Color,
    val percentage: Double = 0.0
)