package com.moneymanager.data.capture

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.capture.CaptureReminderSchedule
import com.moneymanager.domain.capture.SmsInbox
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one inexact alarm armed for the next noon or evening slot (F07 E9). Each alarm fires the receiver, which
 * starts the worker and re-arms the next slot. Inexact on purpose: `setWindow` needs no exact-alarm permission.
 */
@Singleton
class CaptureReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: PreferencesManager,
    private val inbox: SmsInbox,
) {
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /** Matches the alarm to the saved setting. Safe to call on every app start, boot and update. */
    suspend fun reschedule() {
        if (!inbox.isAvailable || !preferences.captureRemindersEnabled.first()) {
            cancel()
            return
        }
        val trigger = CaptureReminderSchedule.nextTriggerMillis(System.currentTimeMillis(), ZoneId.systemDefault())
        alarms.setWindow(AlarmManager.RTC, trigger, CaptureReminderSchedule.WINDOW_MS, pendingIntent())
    }

    fun cancel() {
        alarms.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, CaptureReminderReceiver::class.java).setAction(ACTION_REMIND)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_REMIND = "com.moneymanager.app.capture.REMIND"
        private const val REQUEST_CODE = 4201
    }
}
