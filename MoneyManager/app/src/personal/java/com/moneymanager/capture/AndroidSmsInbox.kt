package com.moneymanager.capture

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.moneymanager.domain.capture.SmsInbox
import com.moneymanager.domain.capture.SmsInboxMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Reads the device inbox through the SMS provider. Personal build only; returns nothing without READ_SMS. */
class AndroidSmsInbox @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsInbox {

    override val isAvailable: Boolean = true

    override fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

    override fun hasReceivePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED

    override suspend fun read(sinceMillis: Long, newestFirst: Boolean, limit: Int): List<SmsInboxMessage> =
        withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()

            val order = if (newestFirst) "DESC" else "ASC"
            val cursor = context.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                "${Telephony.Sms.DATE} >= ?",
                arrayOf(sinceMillis.toString()),
                "${Telephony.Sms.DATE} $order LIMIT $limit",
            ) ?: return@withContext emptyList()

            cursor.use {
                val addressIndex = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
                buildList {
                    while (it.moveToNext()) {
                        add(
                            SmsInboxMessage(
                                sender = it.getString(addressIndex),
                                body = it.getString(bodyIndex).orEmpty(),
                                date = it.getLong(dateIndex),
                            )
                        )
                    }
                }
            }
        }
}
