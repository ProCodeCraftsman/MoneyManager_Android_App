package com.moneymanager.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceCapabilityManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val modelManager: LiteRtModelManager,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "DeviceCapability"
        private const val MIN_RAM_FOR_LOCAL_MODEL_GB = 6L
        private const val GB_BYTES = 1_073_741_824L
    }

    suspend fun resolveBackendTier(): AiBackendTier {
        Log.d(TAG, "Resolving AI backend tier...")

        if (!hasSufficientRam(MIN_RAM_FOR_LOCAL_MODEL_GB)) {
            Log.w(TAG, "Insufficient RAM (< ${MIN_RAM_FOR_LOCAL_MODEL_GB}GB). Tier = NONE")
            preferencesManager.setAiBackendTier(AiBackendTier.NONE.id)
            return AiBackendTier.NONE
        }

        return when (val aicoreStatus = checkAicoreAvailability()) {
            FeatureStatus.AVAILABLE -> {
                Log.d(TAG, "AICore AVAILABLE. Tier = AICORE")
                preferencesManager.setAiBackendTier(AiBackendTier.AICORE.id)
                preferencesManager.setAiAvailabilityStatus("READY")
                AiBackendTier.AICORE
            }
            FeatureStatus.UNAVAILABLE -> {
                Log.d(TAG, "AICore UNAVAILABLE. Checking local model tier.")
                resolveLocalModelTier()
            }
            FeatureStatus.DOWNLOADABLE -> {
                Log.d(TAG, "AICore DOWNLOADABLE. Attempting download...")
                val downloaded = tryDownloadAicore()
                if (downloaded) {
                    preferencesManager.setAiBackendTier(AiBackendTier.AICORE.id)
                    preferencesManager.setAiAvailabilityStatus("READY")
                    AiBackendTier.AICORE
                } else {
                    Log.d(TAG, "AICore download failed/missed. Falling back to LOCAL_MODEL.")
                    preferencesManager.setAiBackendTier(AiBackendTier.LOCAL_MODEL.id)
                    preferencesManager.setAiAvailabilityStatus("NEVER")
                    AiBackendTier.LOCAL_MODEL
                }
            }
            FeatureStatus.DOWNLOADING -> {
                Log.d(TAG, "AICore already DOWNLOADING. Awaiting completion...")
                val downloaded = waitForAicoreDownload()
                if (downloaded) {
                    preferencesManager.setAiBackendTier(AiBackendTier.AICORE.id)
                    preferencesManager.setAiAvailabilityStatus("READY")
                    AiBackendTier.AICORE
                } else {
                    preferencesManager.setAiBackendTier(AiBackendTier.LOCAL_MODEL.id)
                    preferencesManager.setAiAvailabilityStatus("NEVER")
                    AiBackendTier.LOCAL_MODEL
                }
            }
            else -> {
                Log.d(TAG, "AICore status unknown/error. Falling back to LOCAL_MODEL.")
                preferencesManager.setAiBackendTier(AiBackendTier.LOCAL_MODEL.id)
                preferencesManager.setAiAvailabilityStatus("NEVER")
                AiBackendTier.LOCAL_MODEL
            }
        }
    }

    /** Called by MoneyManagerApp at startup on Dispatchers.IO. */
    suspend fun checkAndCacheAvailability() = resolveBackendTier()

    suspend fun resolveCurrentTier(): AiBackendTier {
        val stored = preferencesManager.aiBackendTier.first()
        return if (stored == "pending") resolveBackendTier() else AiBackendTier.fromId(stored)
    }

    private suspend fun resolveLocalModelTier(): AiBackendTier {
        if (modelManager.selectModelForDevice() == null) {
            preferencesManager.setAiBackendTier(AiBackendTier.NONE.id)
            preferencesManager.setAiAvailabilityStatus("NEVER")
            Log.d(TAG, "RAM < 6 GB. Tier = NONE")
            return AiBackendTier.NONE
        }
        preferencesManager.setAiBackendTier(AiBackendTier.LOCAL_MODEL.id)
        return if (modelManager.isModelDownloaded()) {
            preferencesManager.setAiAvailabilityStatus("LOCAL_READY")
            preferencesManager.setLocalModelDownloaded(true)
            Log.d(TAG, "Local model downloaded. Tier = LOCAL_MODEL/READY")
            AiBackendTier.LOCAL_MODEL
        } else {
            preferencesManager.setAiAvailabilityStatus("LOCAL_DOWNLOADABLE")
            preferencesManager.setLocalModelDownloaded(false)
            Log.d(TAG, "Local model not downloaded. Tier = LOCAL_MODEL/DOWNLOADABLE")
            AiBackendTier.LOCAL_MODEL
        }
    }

    private fun hasSufficientRam(minGb: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val totalRamGb = info.totalMem / GB_BYTES
            Log.d(TAG, "Total RAM: ${totalRamGb}GB")
            totalRamGb >= minGb
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read RAM info", e)
            true // assume enough if we can't check
        }
    }

    private fun checkAicoreAvailability(): FeatureStatus {
        return try {
            val client = Generation.getClient()
            client.checkStatus()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking AICore availability", e)
            FeatureStatus.UNAVAILABLE
        }
    }

    private suspend fun tryDownloadAicore(): Boolean {
        return try {
            val client = Generation.getClient()
            preferencesManager.setAiAvailabilityStatus("PENDING")
            var totalBytes = 0L
            var completed = false
            client.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadCompleted -> { completed = true }
                    is DownloadStatus.DownloadFailed -> { completed = false }
                    is DownloadStatus.DownloadStarted -> totalBytes = status.bytesToDownload
                    else -> {}
                }
            }
            completed
        } catch (e: Exception) {
            Log.e(TAG, "AICore download failed", e)
            false
        }
    }

    private suspend fun waitForAicoreDownload(): Boolean {
        return try {
            val client = Generation.getClient()
            var completed = false
            client.download().collect { status ->
                when (status) {
                    is DownloadStatus.DownloadCompleted -> { completed = true }
                    is DownloadStatus.DownloadFailed -> { completed = false }
                    else -> {}
                }
            }
            completed
        } catch (e: Exception) {
            Log.e(TAG, "AICore download wait failed", e)
            false
        }
    }
}
