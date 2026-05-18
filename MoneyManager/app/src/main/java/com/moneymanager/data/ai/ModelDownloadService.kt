package com.moneymanager.data.ai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.moneymanager.app.MainActivity
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@AndroidEntryPoint
class ModelDownloadService : Service() {

    @Inject lateinit var preferencesManager: PreferencesManager
    @Inject lateinit var modelManager: LiteRtModelManager

    companion object {
        private const val TAG = "ModelDownloadSvc"
        private const val CHANNEL_ID = "ai_model_download"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_CANCEL = "com.moneymanager.action.CANCEL_DOWNLOAD"
        private const val MODELS_DIR = "models"

        fun start(context: Context, modelName: String) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                putExtra("model_name", modelName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun cancel(context: Context) {
            val intent = Intent(context, ModelDownloadService::class.java).apply {
                action = ACTION_CANCEL
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private var cancelled = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancelled = true
            downloadJob?.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val modelName = intent?.getStringExtra("model_name") ?: run {
            stopSelf()
            return START_NOT_STICKY
        }

        // Look up model from allowlist (embedded or remote)
        // In a foreground service we can't suspend, so resolve synchronously
        val model = runBlocking { modelManager.getModelByName(modelName) }
        if (model == null) { stopSelf(); return START_NOT_STICKY }

        val notification = buildNotification("Starting download...", 0, false)
        startForeground(NOTIFICATION_ID, notification)

        cancelled = false
        downloadJob = serviceScope.launch {
            downloadModel(model)
            serviceScope.cancel()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        downloadJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun downloadModel(model: ModelEntry) {
        try {
            val wifiOnly = preferencesManager.getWifiOnlyDownloadSync()
            if (wifiOnly && !isOnWifi()) {
                Log.w(TAG, "Not on WiFi. Download paused.")
                updateNotification("Waiting for WiFi...", 0, false, true)
                waitForWifiConnection()
                if (cancelled) return
                updateNotification("WiFi connected. Resuming...", 0, false)
            }

            var lastReadBytes = 0L
            var lastSpeedCalcMs = 0L

            val accessToken = preferencesManager.getHfAccessTokenSync()
            val outputFile = File(File(filesDir, MODELS_DIR), model.modelFile)
            val result = ModelDownloader.downloadFile(
                urlString = model.downloadUrl,
                outputFile = outputFile,
                totalBytesHint = model.sizeBytes,
                accessToken = accessToken.ifEmpty { null },
                onProgress = { read, total ->
                    val pct = if (total > 0) read * 100 / total else 0
                    val progress = if (total > 0) read.toFloat() / total else 0f
                    preferencesManager.setLocalModelDownloadProgress(progress)
                    preferencesManager.setLocalModelDownloadReceived(read)
                    preferencesManager.setLocalModelDownloadTotal(total)

                    val now = System.currentTimeMillis()
                    if (lastSpeedCalcMs > 0L) {
                        val elapsed = now - lastSpeedCalcMs
                        val deltaBytes = read - lastReadBytes
                        if (elapsed > 0) {
                            val speed = (deltaBytes * 1000L) / elapsed
                            preferencesManager.setLocalModelDownloadSpeed(speed)
                        }
                    }
                    lastReadBytes = read
                    lastSpeedCalcMs = now

                    updateNotification("Downloading ${model.modelFile}", pct.toInt(), false)
                },
                isCancelled = { cancelled },
            )

            if (result.success) {
                preferencesManager.setLocalModelDownloaded(true)
                preferencesManager.setLocalModelDownloadProgress(1f)
                updateNotification("${model.modelFile} ready", 100, true)
            } else {
                if (!cancelled) {
                    updateNotification("Download failed: ${result.error}", 0, true, error = true)
                    preferencesManager.setLocalModelDownloadProgress(0f)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            updateNotification("Download failed: ${e.message}", 0, true, error = true)
            preferencesManager.setLocalModelDownloadProgress(0f)
        }
    }

    private fun isOnWifi(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private suspend fun waitForWifiConnection() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val latch = java.util.concurrent.CountDownLatch(1)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                latch.countDown()
            }
        }
        cm.registerNetworkCallback(networkRequest, callback)
        if (isOnWifi()) {
            cm.unregisterNetworkCallback(callback)
            return
        }
        while (!cancelled) {
            if (latch.await(5, java.util.concurrent.TimeUnit.SECONDS)) break
        }
        try { cm.unregisterNetworkCallback(callback) } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "AI Model Download", NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String, progress: Int, done: Boolean, error: Boolean = false): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelIntent = PendingIntent.getService(
            this, 1, Intent(this, ModelDownloadService::class.java).apply { action = ACTION_CANCEL },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (error) "Download failed" else if (done) "Model ready" else "Downloading AI model")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(!done && !error)
            .setProgress(100, progress, false)
            .setContentIntent(openPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
    }

    private fun updateNotification(text: String, progress: Int, done: Boolean, error: Boolean = false) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(text, progress, done, error))
    }
}
