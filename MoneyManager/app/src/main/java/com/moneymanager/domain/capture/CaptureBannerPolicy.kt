package com.moneymanager.domain.capture

/**
 * Home banner visibility (F07 E8). The banner only says something useful when spends are waiting; "today" is the
 * local epoch day, so a dismiss lasts until local midnight.
 */
object CaptureBannerPolicy {
    fun isVisible(pending: Int, dismissedEpochDay: Long?, hidden: Boolean, todayEpochDay: Long): Boolean =
        !hidden && pending > 0 && dismissedEpochDay != todayEpochDay
}
