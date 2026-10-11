package com.moneymanager.domain.capture

/** One inbox message as the scanner sees it. Held in memory for the scan only; never persisted. */
data class SmsInboxMessage(val sender: String?, val body: String, val date: Long)

/**
 * Read access to the device inbox. The personal build implements it on top of READ_SMS; the play build has no
 * implementation that reads anything, so no SMS code path can touch the inbox there.
 */
interface SmsInbox {
    /** False when this build cannot read the inbox at all, so the UI can hide the feature. */
    val isAvailable: Boolean

    /** Messages received at or after [sinceMillis]. Empty when the permission is not granted. */
    suspend fun read(sinceMillis: Long, newestFirst: Boolean, limit: Int): List<SmsInboxMessage>
}
