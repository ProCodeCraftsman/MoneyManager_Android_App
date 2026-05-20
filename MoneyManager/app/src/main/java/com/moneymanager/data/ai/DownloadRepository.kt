package com.moneymanager.data.ai

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface DownloadRepository {
    /** Enqueues a download. Returns false if already running (KEEP policy). */
    fun downloadModel(model: ModelEntry): Boolean

    /** Cancels an active download by model name. */
    fun cancelDownload(modelName: String)

    /** Cancels all active model downloads. */
    fun cancelAll()

    /** Observe WorkInfo for all model downloads. */
    fun observeAllDownloads(): Flow<List<WorkInfo>>

    /** Observe WorkInfo for a specific model. */
    fun observeDownload(modelName: String): Flow<WorkInfo?>

    /** True if a download is RUNNING or ENQUEUED for this model. */
    fun isActive(modelName: String): Boolean
}

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
) : DownloadRepository {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    override fun downloadModel(model: ModelEntry): Boolean {
        val wifiOnly = preferencesManager.getWifiOnlyDownloadSync()
        val token = preferencesManager.getHfAccessTokenSync()
        val request = DownloadWorker.buildRequest(
            modelName = model.name,
            modelUrl = model.downloadUrl,
            modelFile = model.modelFile,
            totalBytes = model.sizeBytes,
            accessToken = token.ifEmpty { null },
            wifiOnly = wifiOnly,
        )
        workManager.enqueueUniqueWork(
            DownloadWorker.uniqueWorkName(model.name),
            ExistingWorkPolicy.KEEP,
            request,
        )
        return true
    }

    override fun cancelDownload(modelName: String) {
        workManager.cancelUniqueWork(DownloadWorker.uniqueWorkName(modelName))
    }

    override fun cancelAll() {
        workManager.cancelAllWorkByTag(DownloadWorker.TAG_ALL)
    }

    override fun observeAllDownloads(): Flow<List<WorkInfo>> =
        workManager.getWorkInfosByTagFlow(DownloadWorker.TAG_ALL)

    override fun observeDownload(modelName: String): Flow<WorkInfo?> =
        workManager.getWorkInfosByTagFlow(DownloadWorker.tagFor(modelName))
            .map { it.firstOrNull() }

    override fun isActive(modelName: String): Boolean {
        val infos = workManager.getWorkInfosByTag(DownloadWorker.tagFor(modelName)).get()
        return infos?.any {
            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
        } == true
    }
}
