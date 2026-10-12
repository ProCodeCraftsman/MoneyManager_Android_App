package com.moneymanager.data.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives the reminder alarm, the boot broadcast and the app-update broadcast. Re-arming happens before the worker
 * is queued, so a worker failure cannot break the chain of alarms.
 */
@AndroidEntryPoint
class CaptureReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: CaptureReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.reschedule()
                if (intent.action == CaptureReminderScheduler.ACTION_REMIND) {
                    WorkManager.getInstance(context).enqueueUniqueWork(
                        WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequestBuilder<CaptureReminderWorker>().build(),
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val WORK_NAME = "capture_reminder"
    }
}
