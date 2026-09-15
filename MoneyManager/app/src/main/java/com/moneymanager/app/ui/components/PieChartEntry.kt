package com.moneymanager.app.ui.components

data class PieChartEntry(
    val label: String,
    val value: Double,
    /** Index into the current theme's CategoryPalettes; null = neutral "Others" bucket. */
    val colorIndex: Int? = null,
    val percentage: Double = 0.0
)