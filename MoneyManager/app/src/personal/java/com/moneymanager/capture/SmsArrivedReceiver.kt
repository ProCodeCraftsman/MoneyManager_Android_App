package com.moneymanager.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.moneymanager.data.capture.CaptureLiveScanWorker

/**
 * Hears each incoming SMS (AI-060). It never reads the message: it queues the same scan that runs on app open, so the
 * message goes through the same cursor, rules and ledger. Declared in the personal manifest only.
 */
class SmsArrivedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        WorkManager.getInstance(context).enqueueUniqueWork(
            CaptureLiveScanWorker.WORK_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<CaptureLiveScanWorker>().build(),
        )
    }
}
