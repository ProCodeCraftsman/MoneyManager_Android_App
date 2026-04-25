package com.moneymanager.app.ui.theme

enum class AppTheme(val displayName: String) {
    SOFT_NEUTRAL("Soft Neutral"),
    WARM_FINANCE("Warm Finance"),
    COOL_BLUE("Cool Blue Finance"),
    GREEN_LEDGER("Minimal Green Ledger"),
    MODERN_MUTED("Modern Muted");

    companion object {
        fun fromString(value: String): AppTheme {
            return entries.find { it.name == value } ?: SOFT_NEUTRAL
        }
    }
}