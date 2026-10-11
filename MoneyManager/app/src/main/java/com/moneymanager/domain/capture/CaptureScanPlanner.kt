package com.moneymanager.domain.capture

/** Where one inbox scan starts. [anchorMode] means there is no cursor yet, so the scan walks back from the newest message. */
data class ScanPlan(val lowerBound: Long, val anchorMode: Boolean)

/**
 * Chooses the window for an inbox scan (F07 "Scan window"). With a cursor the scan re-reads a 2 h overlap so
 * late or out-of-order SMS are not missed; the ledger hash makes the overlap harmless. Without one, the scan is
 * anchored: it walks back from the newest message until a few known transactions show it has caught up.
 */
object CaptureScanPlanner {
    const val OVERLAP_MS = 2 * 60 * 60 * 1000L
    const val MAX_WINDOW_MS = 31L * 24 * 60 * 60 * 1000
    const val MAX_MESSAGES = 500
    const val ANCHOR_RUN = 3

    fun plan(lastScanned: Long?, startDate: Long?, now: Long): ScanPlan {
        val floor = maxOf(startDate ?: 0L, now - MAX_WINDOW_MS)
        return if (lastScanned == null) {
            ScanPlan(lowerBound = floor, anchorMode = true)
        } else {
            ScanPlan(lowerBound = maxOf(lastScanned - OVERLAP_MS, floor), anchorMode = false)
        }
    }
}
