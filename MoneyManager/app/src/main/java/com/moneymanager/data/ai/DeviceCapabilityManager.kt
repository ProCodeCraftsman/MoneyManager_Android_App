package com.moneymanager.data.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.prompt.Generation
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext

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

    suspend fun resolveBackendTier(): AiBackend {
        Log.d(TAG, "Resolving AI backend tier...")
        if (!hasSufficientRam(MIN_RAM_FOR_LOCAL_MODEL_GB)) {
            Log.w(TAG, "Insufficient RAM (< ${MIN_RAM_FOR_LOCAL_MODEL_GB}GB). Tier = NONE")
            persistTier(AiBackend.NONE, "NEVER")
            return AiBackend.NONE
        }
        return when (val aicoreCode = checkAicoreStatusCode()) {
            0 -> { // AVAILABLE
                Log.d(TAG, "AICore status 0 (AVAILABLE). Tier = AICORE")
                persistTier(AiBackend.AICORE, "READY")
                AiBackend.AICORE
            }
            1 -> { // DOWNLOADABLE
                Log.d(TAG, "AICore status 1 (DOWNLOADABLE). Attempting download...")
                if (downloadAicore()) {
                    persistTier(AiBackend.AICORE, "READY")
                    AiBackend.AICORE
                } else resolveLocalModelTier()
            }
            2 -> { // DOWNLOADING
                Log.d(TAG, "AICore status 2 (DOWNLOADING). Waiting...")
                if (downloadAicore()) {
                    persistTier(AiBackend.AICORE, "READY")
                    AiBackend.AICORE
                } else resolveLocalModelTier()
            }
            else -> { // error or UNAVAILABLE
                Log.d(TAG, "AICore unavailable (code $aicoreCode). Trying local model.")
                resolveLocalModelTier()
            }
        }
    }

    suspend fun checkAndCacheAvailability() = resolveBackendTier()

    private var cachedNoneTier: Boolean? = null

    suspend fun resolveCurrentTier(): AiBackend {
        if (cachedNoneTier == true) return AiBackend.NONE
        val stored = preferencesManager.aiBackendTier.first()
        val tier = if (stored == "pending") resolveBackendTier() else AiBackend.fromId(stored)
        if (tier == AiBackend.NONE) cachedNoneTier = true
        return tier
    }

    fun invalidateTierCache() {
        cachedNoneTier = null
    }

    private suspend fun resolveLocalModelTier(): AiBackend {
        val selected = modelManager.selectModelForDevice()
        if (selected == null) {
            persistTier(AiBackend.NONE, "NEVER")
            return AiBackend.NONE
        }
        if (modelManager.isModelDownloaded()) {
            preferencesManager.setLocalModelDownloaded(true)
            persistTier(AiBackend.LOCAL_MODEL, "LOCAL_READY")
        } else {
            persistTier(AiBackend.LOCAL_MODEL, "LOCAL_DOWNLOADABLE")
        }
        return AiBackend.LOCAL_MODEL
    }

    private fun hasSufficientRam(minGb: Long): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return try {
            (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).let { am ->
                val info = ActivityManager.MemoryInfo()
                am.getMemoryInfo(info)
                info.totalMem / GB_BYTES >= minGb
            }
        } catch (e: Exception) {
            true
        }
    }

    private suspend fun checkAicoreStatusCode(): Int {
        return withContext(Dispatchers.IO) {
            try {
                Generation.getClient().checkStatus()
            } catch (e: Exception) {
                Log.e(TAG, "AICore check failed", e)
                -1
            }
        }
    }

    private suspend fun downloadAicore(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = Generation.getClient()
                val results = client.download().toList()
                results.any { it is DownloadStatus.DownloadCompleted }
            } catch (e: Exception) {
                Log.e(TAG, "AICore download failed", e)
                false
            }
        }
    }

    private suspend fun persistTier(tier: AiBackend, status: String) {
        preferencesManager.setAiBackendTier(tier.id)
        preferencesManager.setAiAvailabilityStatus(status)
    }
}
