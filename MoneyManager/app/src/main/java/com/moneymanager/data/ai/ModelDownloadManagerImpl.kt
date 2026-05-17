package com.moneymanager.data.ai

import android.content.Context
import android.os.StatFs
import android.util.Log
import com.moneymanager.domain.ai.DownloadProgress
import com.moneymanager.domain.ai.ModelDownloadManager
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelDownloadManagerImpl @Inject constructor(
    private val modelManager: LiteRtModelManager,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
) : ModelDownloadManager {

    companion object {
        private const val MIN_DISK_SPACE_BYTES = 1_073_741_824L
        private const val MODEL_RELATIVE_PATH = "models/gemma3_1b_int4.task"
        private const val MAX_RETRIES = 3
        private const val BASE_DELAY_MS = 5000L
        private const val TAG = "ModelDownloadMgr"
    }

    override fun download(): Flow<DownloadProgress> = callbackFlow {
        val diskSpace = getAvailableDiskSpace()
        if (diskSpace < MIN_DISK_SPACE_BYTES) {
            trySend(DownloadProgress(0, 0, false, "Insufficient storage — need at least 1 GB free"))
            close()
            return@callbackFlow
        }

        var lastError: String? = null
        for (attempt in 1..MAX_RETRIES) {
            if (attempt > 1) {
                delay(BASE_DELAY_MS * (1L shl (attempt - 2)))
            }

            val model = modelManager.selectModelForDevice()
            if (model == null) {
                trySend(DownloadProgress(0, 0, false, "No compatible model for device"))
                close()
                return@callbackFlow
            }

            data class State(var last: com.moneymanager.data.ai.DownloadProgress? = null)
            val state = State()
            val channel = Channel<com.moneymanager.data.ai.DownloadProgress>(Channel.CONFLATED)

            val job = launch {
                for (p in channel) {
                    trySend(DownloadProgress(p.bytesDownloaded, p.totalBytes, p.isComplete, p.error))
                }
            }

            modelManager.downloadModel(
                model = model,
                onProgress = { p -> channel.trySend(p) },
            ).fold(
                onSuccess = { file ->
                    job.cancel()
                    trySend(DownloadProgress(file.length(), file.length(), true))
                    close()
                    return@callbackFlow
                },
                onFailure = { e ->
                    job.cancel()
                    lastError = e.message ?: "Download failed"
                    if (isStorageError(e)) {
                        trySend(DownloadProgress(0, 0, false, lastError))
                        close()
                        return@callbackFlow
                    }
                    Log.w(TAG, "$lastError (attempt $attempt/$MAX_RETRIES)")
                }
            )
        }
        trySend(DownloadProgress(0, 0, false, lastError ?: "Download failed after $MAX_RETRIES attempts"))
        close()
        awaitClose { }
    }

    override suspend fun isModelDownloaded(): Boolean = modelManager.isModelDownloaded()

    override suspend fun getModelPath(): String =
        File(context.filesDir, MODEL_RELATIVE_PATH).absolutePath

    override suspend fun cancelDownload() {
        ModelDownloadService.cancel(context)
    }

    override suspend fun deleteModel() {
        val model = modelManager.selectModelForDevice()
        if (model != null) modelManager.deleteModel(model)
    }

    private fun getAvailableDiskSpace(): Long {
        val dir = context.filesDir.parentFile ?: context.filesDir
        return try {
            val stat = StatFs(dir.absolutePath)
            stat.availableBytes
        } catch (e: Exception) {
            Long.MAX_VALUE
        }
    }

    private fun isStorageError(e: Throwable): Boolean {
        val msg = e.message?.lowercase() ?: ""
        return msg.contains("space") || msg.contains("storage") || msg.contains("disk")
    }
}
