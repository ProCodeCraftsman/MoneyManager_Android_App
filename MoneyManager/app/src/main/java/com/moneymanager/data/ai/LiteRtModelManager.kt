package com.moneymanager.data.ai

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// ─── Runtime model data classes ───────────────────────────────────────────────

data class ModelConfig(
    val topK: Int = 64,
    val topP: Float = 0.95f,
    val temperature: Float = 1.0f,
    val maxTokens: Int = 1024,
    val maxContextLength: Int? = null,
    val accelerators: String = "gpu,cpu",
    val visionAccelerator: String = "gpu",
)

data class UpdatableModelFile(
    val fileName: String,
    val commitHash: String,
)

data class ModelEntry(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val commitHash: String,
    val sizeBytes: Long,
    val minRamGb: Int,
    val description: String = "",
    val defaultConfig: ModelConfig = ModelConfig(),
    val taskTypes: List<String> = emptyList(),
    val capabilities: List<String> = emptyList(),
    val bestForTaskTypes: List<String> = emptyList(),
    val llmSupportImage: Boolean = false,
    val llmSupportAudio: Boolean = false,
    val updatableModelFiles: List<UpdatableModelFile> = emptyList(),
    val updateInfo: String = "",
    val customUrl: String? = null,
) {
    val downloadUrl: String
        get() = customUrl
            ?: "https://huggingface.co/$modelId/resolve/$commitHash/$modelFile"

    val hasThinking: Boolean get() = "llm_thinking" in capabilities
    val hasSpeculativeDecoding: Boolean get() = "speculative_decoding" in capabilities
    val isMultimodal: Boolean get() = llmSupportImage || llmSupportAudio
    val supportsVision: Boolean get() = llmSupportImage && "llm_ask_image" in taskTypes
    val isBestForAny: Boolean get() = bestForTaskTypes.isNotEmpty()
    val sizeGb: Float get() = sizeBytes / 1_073_741_824f
}

// ─── Deprecated progress holder kept for backward compat ──────────────────────

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean,
    val error: String? = null,
)

// ─── Manager ──────────────────────────────────────────────────────────────────

private const val TAG = "LiteRtModelManager"
private const val MODELS_DIR = "models"

@Singleton
class LiteRtModelManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val allowlistRepository: ModelAllowlistRepository,
    @ApplicationContext private val context: Context,
) {

    /** All models from the effective allowlist (bundled + user overrides). */
    suspend fun getAllowlist(): List<ModelEntry> =
        allowlistRepository.getEffectiveAllowlist().map { it.toModelEntry() }

    /** Models compatible with device RAM. */
    suspend fun getAvailableModels(): List<ModelEntry> {
        val ramGb = getTotalRamGb()
        return getAllowlist().filter { ramGb >= it.minRamGb }
    }

    /** Find a model by exact name (case-insensitive). */
    suspend fun getModelByName(name: String): ModelEntry? =
        getAllowlist().firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** User's preferred model if compatible, else best available by RAM. */
    suspend fun getUserSelectedModel(): ModelEntry {
        val available = getAvailableModels()
        if (available.isEmpty()) return getAllowlist().firstOrNull()
            ?: error("No models in allowlist")
        val preferred = preferencesManager.selectedLocalModel.first()
        return available.firstOrNull { it.name == preferred } ?: available.first()
    }

    /** Best model for device based on RAM requirement. */
    suspend fun selectModelForDevice(): ModelEntry? =
        getAvailableModels().maxByOrNull { it.minRamGb }

    /** Whether the model's current version file exists on disk. */
    fun isModelDownloaded(model: ModelEntry): Boolean {
        val f = getModelFile(model)
        return f.exists() && f.length() > 100_000L
    }

    /**
     * True when an older version from [ModelEntry.updatableModelFiles] is on disk
     * but the latest file is not — signals "Update available".
     */
    fun isModelUpdatable(model: ModelEntry): Boolean {
        if (isModelDownloaded(model)) return false
        val dir = modelsDir()
        return model.updatableModelFiles.any { old ->
            val f = File(dir, old.fileName)
            f.exists() && f.length() > 100_000L
        }
    }

    /** Check if currently on WiFi. */
    fun isOnWifi(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /** Model file path on disk. */
    fun getModelFile(model: ModelEntry): File =
        File(modelsDir(), model.modelFile)

    /** True if the currently selected best model is downloaded. */
    suspend fun isModelDownloaded(): Boolean {
        val model = selectModelForDevice() ?: return false
        return isModelDownloaded(model)
    }

    /** Download a model directly (used by tests / legacy code — prefer DownloadRepository). */
    suspend fun downloadModel(
        model: ModelEntry,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val token = preferencesManager.getHfAccessTokenSync()
        val outputFile = getModelFile(model)
        val result = ModelDownloader.downloadFile(
            urlString = model.downloadUrl,
            outputFile = outputFile,
            totalBytesHint = model.sizeBytes,
            accessToken = token.ifEmpty { null },
            onProgress = { read, total ->
                onProgress(DownloadProgress(read, total, false))
            },
        )
        if (result.success) {
            preferencesManager.setLocalModelDownloaded(true)
            preferencesManager.setLocalModelDownloadProgress(1f)
            onProgress(DownloadProgress(result.totalBytes, result.totalBytes, true))
            Result.success(outputFile)
        } else {
            preferencesManager.setLocalModelDownloadProgress(0f)
            onProgress(DownloadProgress(0, 0, false, result.error))
            Result.failure(Exception(result.error ?: "Download failed"))
        }
    }

    /** Delete a model file and any partial downloads / old updatable files. */
    suspend fun deleteModel(model: ModelEntry) {
        getModelFile(model).delete()
        // Clean up old versions
        model.updatableModelFiles.forEach { old ->
            File(modelsDir(), old.fileName).delete()
        }
        // Clean up .tmp files
        modelsDir().listFiles { f -> f.name.endsWith(".tmp") }?.forEach { it.delete() }

        val anyDownloaded = getAvailableModels().any { isModelDownloaded(it) }
        if (!anyDownloaded) {
            preferencesManager.setLocalModelDownloaded(false)
            preferencesManager.setLocalModelDownloadProgress(0f)
        }
        Log.d(TAG, "Deleted ${model.name}")
    }

    // ── Private ──────────────────────────────────────────────────────────

    private fun modelsDir(): File = File(context.filesDir, MODELS_DIR).also { it.mkdirs() }

    private fun getTotalRamGb(): Long {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return info.totalMem / (1024L * 1024L * 1024L)
    }
}
