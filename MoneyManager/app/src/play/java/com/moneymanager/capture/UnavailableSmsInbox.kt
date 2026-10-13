package com.moneymanager.capture

import com.moneymanager.domain.capture.SmsInbox
import com.moneymanager.domain.capture.SmsInboxMessage

/** Play build: there is no SMS permission and no code that reads the inbox. */
object UnavailableSmsInbox : SmsInbox {
    override val isAvailable: Boolean = false

    override fun hasPermission(): Boolean = false

    override fun hasReceivePermission(): Boolean = false

    override suspend fun read(sinceMillis: Long, newestFirst: Boolean, limit: Int): List<SmsInboxMessage> = emptyList()
}
