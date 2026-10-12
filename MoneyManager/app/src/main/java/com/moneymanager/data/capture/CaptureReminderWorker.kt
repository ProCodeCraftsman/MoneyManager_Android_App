package com.moneymanager.data.capture

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneymanager.app.MainActivity
import com.moneymanager.app.R
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.capture.ScanInboxUseCase
import com.moneymanager.domain.capture.SmsInbox
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

/**
 * Noon / evening reminder (F07 E9). Scans first, then notifies only when drafts are waiting. A count of zero
 * means no notification at all.
 */
@HiltWorker
class CaptureReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val scanInbox: ScanInboxUseCase,
    private val store: CaptureStore,
    private val inbox: SmsInbox,
    private val preferences: PreferencesManager,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!inbox.isAvailable || !preferences.captureRemindersEnabled.first()) return Result.success()

        try {
            scanInbox()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // A failed scan still leaves the drafts already waiting; notify about those.
        }

        val pending = store.observePendingCount().first()
        if (pending > 0) notify(pending)
        return Result.success()
    }

    private fun notify(pending: Int) {
        val manager = NotificationManagerCompat.from(appContext)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel()

        val open = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle(appContext.resources.getQuantityString(R.plurals.capture_reminder_title, pending, pending))
            .setContentText(appContext.getString(R.string.capture_reminder_text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()

        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS was revoked between the check and the post.
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.capture_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.capture_reminder_channel_description)
        }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "capture_reminder_channel"
        const val NOTIFICATION_ID = 1002
    }
}
