package com.moneymanager.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.moneymanager.data.preferences.PreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class DownloadWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val preferencesManager: PreferencesManager,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "DownloadWorker"

        const val KEY_MODEL_NAME = "model_name"
        const val KEY_MODEL_URL = "model_url"
        const val KEY_MODEL_FILE = "model_file"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_WIFI_ONLY = "wifi_only"

        const val PROGRESS_RECEIVED = "progress_received"
        const val PROGRESS_TOTAL = "progress_total"
        const val PROGRESS_SPEED = "progress_speed"
        const val PROGRESS_REMAINING_MS = "progress_remaining_ms"

        const val OUTPUT_ERROR = "output_error"
        const val ERROR_AUTH = "auth_required"

        const val TAG_PREFIX = "download:"
        const val TAG_ALL = "model_download"

        fun tagFor(modelName: String) = "$TAG_PREFIX$modelName"
        fun uniqueWorkName(modelName: String) = "download_$modelName"

        private const val CHANNEL_ID = "model_download_worker"
        private const val NOTIFICATION_BASE = 3000

        fun buildRequest(
            modelName: String,
            modelUrl: String,
            modelFile: String,
            totalBytes: Long,
            accessToken: String?,
            wifiOnly: Boolean,
        ): OneTimeWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .build()
            return OneTimeWorkRequestBuilder<DownloadWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_MODEL_NAME to modelName,
                        KEY_MODEL_URL to modelUrl,
                        KEY_MODEL_FILE to modelFile,
                        KEY_TOTAL_BYTES to totalBytes,
                        KEY_ACCESS_TOKEN to (accessToken ?: ""),
                        KEY_WIFI_ONLY to wifiOnly,
                    )
                )
                .addTag(tagFor(modelName))
                .addTag(TAG_ALL)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()
        }
    }

    private val notifId: Int
        get() = NOTIFICATION_BASE + ((inputData.getString(KEY_MODEL_NAME) ?: "").hashCode() and 0xFFFF)

    override suspend fun doWork(): Result {
        val modelName = inputData.getString(KEY_MODEL_NAME) ?: return Result.failure()
        val modelUrl = inputData.getString(KEY_MODEL_URL) ?: return Result.failure()
        val modelFile = inputData.getString(KEY_MODEL_FILE) ?: return Result.failure()
        val totalBytes = inputData.getLong(KEY_TOTAL_BYTES, 0L)
        val accessToken = inputData.getString(KEY_ACCESS_TOKEN)?.takeIf { it.isNotEmpty() }

        Log.d(TAG, "Starting download: $modelName")
        setForeground(createForegroundInfo(modelName, 0))

        val outputFile = File(File(appContext.filesDir, "models"), modelFile)

        var lastReadBytes = 0L
        var lastSpeedMs = 0L

        val result = ModelDownloader.downloadFile(
            urlString = modelUrl,
            outputFile = outputFile,
            totalBytesHint = totalBytes,
            accessToken = accessToken,
            onProgress = { received, total ->
                val now = System.currentTimeMillis()
                var speed = 0L
                if (lastSpeedMs > 0L) {
                    val elapsed = now - lastSpeedMs
                    val delta = received - lastReadBytes
                    if (elapsed > 0) speed = delta * 1000L / elapsed
                }
                lastReadBytes = received
                lastSpeedMs = now
                val remaining = if (speed > 0L) (total - received) * 1000L / speed else 0L
                setProgress(
                    workDataOf(
                        PROGRESS_RECEIVED to received,
                        PROGRESS_TOTAL to total,
                        PROGRESS_SPEED to speed,
                        PROGRESS_REMAINING_MS to remaining,
                    )
                )
                val pct = if (total > 0L) (received * 100L / total).toInt() else 0
                updateNotification(modelName, pct)
            },
            isCancelled = { isStopped },
        )

        return when {
            isStopped -> {
                Log.d(TAG, "Cancelled: $modelName")
                Result.failure()
            }
            result.success -> {
                Log.d(TAG, "Complete: $modelName")
                preferencesManager.setLocalModelDownloaded(true)
                Result.success()
            }
            result.error?.contains("HTTP 401") == true ||
                result.error?.contains("HTTP 403") == true -> {
                Log.w(TAG, "Auth required: $modelName")
                Result.failure(workDataOf(OUTPUT_ERROR to ERROR_AUTH))
            }
            runAttemptCount < 3 -> {
                Log.w(TAG, "Retry $runAttemptCount: $modelName — ${result.error}")
                Result.retry()
            }
            else -> {
                Log.e(TAG, "Failed after retries: $modelName — ${result.error}")
                Result.failure(workDataOf(OUTPUT_ERROR to (result.error ?: "Download failed")))
            }
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val name = inputData.getString(KEY_MODEL_NAME) ?: "Model"
        return createForegroundInfo(name, 0)
    }

    private fun createForegroundInfo(modelName: String, progress: Int): ForegroundInfo {
        createChannel()
        val notification = buildNotif(modelName, progress)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(notifId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notifId, notification)
        }
    }

    private fun buildNotif(modelName: String, progress: Int): Notification {
        return NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("Downloading $modelName")
            .setContentText(if (progress > 0) "$progress%" else "Starting…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    private fun updateNotification(modelName: String, pct: Int) {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, buildNotif(modelName, pct))
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "AI Model Downloads", NotificationManager.IMPORTANCE_LOW)
                        .apply { setShowBadge(false) }
                )
            }
        }
    }
}
