// AppTheme.kt
package com.moneymanager.app.ui.theme

enum class AppTheme(val displayName: String) {
    COCO_BROWN("Coco Brown"),
    CALM_GREEN("Calm Green"),
    MIDNIGHT_BLUE("Midnight Blue");

    companion object {
        fun fromString(value: String): AppTheme {
            return entries.find { it.name == value } ?: CALM_GREEN
        }
    }
}