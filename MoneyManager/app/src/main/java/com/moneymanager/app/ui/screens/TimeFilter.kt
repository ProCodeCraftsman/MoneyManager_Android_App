package com.moneymanager.app.ui.screens

enum class TimeFilter(val displayName: String = "") {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All Time"),
    LAST_MONTH("Last Month"),
    THIS_QUARTER("This Quarter"),
    LAST_QUARTER("Last Quarter"),
    TODAY("Today"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    CUSTOM("Custom")
}
