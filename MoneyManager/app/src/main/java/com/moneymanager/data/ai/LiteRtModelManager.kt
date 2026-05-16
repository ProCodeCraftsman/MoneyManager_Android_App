package com.moneymanager.data.ai

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Supported local LiteRT-LM models. Selection is based on device RAM.
 * File info sourced from gallery/model_allowlists/1_0_14.json.
 */
enum class LocalModel(
    val filename: String,
    val modelId: String,
    val commitHash: String,
    val sizeBytes: Long,
    val minRamGb: Int,
) {
    // ~584 MB — requires ≥6 GB RAM
    GEMMA3_1B(
        filename = "gemma3-1b-it-int4.litertlm",
        modelId = "litert-community/Gemma3-1B-IT",
        commitHash = "42d538a932e8d5b12e6b3b455f5572560bd60b2c",
        sizeBytes = 584_417_280L,
        minRamGb = 6,
    ),
    // ~2.6 GB — requires ≥8 GB RAM; better quality, supports image/audio
    GEMMA4_E2B(
        filename = "gemma-4-E2B-it.litertlm",
        modelId = "litert-community/gemma-4-E2B-it-litert-lm",
        commitHash = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
        sizeBytes = 2_588_147_712L,
        minRamGb = 8,
    );

    val downloadUrl: String
        get() = "https://huggingface.co/$modelId/resolve/$commitHash/$filename"
}

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val isComplete: Boolean,
    val error: String? = null,
)

@Singleton
class LiteRtModelManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "LiteRtModelManager"
        private const val MODELS_DIR = "models"
    }

    /** Select the best model the device can run based on available RAM. Returns null if <6 GB. */
    fun selectModelForDevice(): LocalModel? {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val totalGb = memInfo.totalMem / (1024L * 1024L * 1024L)
        return when {
            totalGb >= LocalModel.GEMMA4_E2B.minRamGb -> LocalModel.GEMMA4_E2B
            totalGb >= LocalModel.GEMMA3_1B.minRamGb -> LocalModel.GEMMA3_1B
            else -> null
        }
    }

    // Store in app-private filesDir — never external storage (HYBRID-07)
    private fun modelsDir(): File = File(context.filesDir, MODELS_DIR)

    fun getModelFile(model: LocalModel = selectModelForDevice() ?: LocalModel.GEMMA3_1B): File =
        File(modelsDir(), model.filename)

    fun isModelDownloaded(): Boolean {
        val model = selectModelForDevice() ?: return false
        val file = getModelFile(model)
        return file.exists() && file.length() > 100_000L
    }

    suspend fun downloadModel(
        model: LocalModel,
        onProgress: (DownloadProgress) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val dir = modelsDir()
            if (!dir.exists()) dir.mkdirs()

            val outputFile = getModelFile(model)
            if (outputFile.exists() && outputFile.length() > 100_000L) {
                Log.d(TAG, "Model already at ${outputFile.absolutePath}")
                onProgress(DownloadProgress(model.sizeBytes, model.sizeBytes, true))
                preferencesManager.setLocalModelDownloaded(true)
                preferencesManager.setLocalModelDownloadProgress(1f)
                return@withContext Result.success(outputFile)
            }

            val tmpFile = File(dir, "${model.filename}.tmp")
            Log.d(TAG, "Downloading ${model.filename} from ${model.downloadUrl}")

            val url = URL(model.downloadUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("User-Agent", "MoneyManager/1.0")
            connection.connect()

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                val msg = "HTTP ${connection.responseCode}: ${connection.responseMessage}"
                Log.e(TAG, msg)
                return@withContext Result.failure(Exception(msg))
            }

            val totalBytes = connection.contentLengthLong.let {
                if (it > 0) it else model.sizeBytes
            }
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tmpFile)
            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastReportMs = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val now = System.currentTimeMillis()
                if (now - lastReportMs > 250) {
                    lastReportMs = now
                    val progress = if (totalBytes > 0) totalRead.toFloat() / totalBytes else 0f
                    preferencesManager.setLocalModelDownloadProgress(progress)
                    onProgress(DownloadProgress(totalRead, totalBytes, false))
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (totalBytes > 0 && totalRead < totalBytes * 0.99) {
                tmpFile.delete()
                val msg = "Incomplete download: $totalRead / $totalBytes bytes"
                Log.e(TAG, msg)
                return@withContext Result.failure(Exception(msg))
            }

            tmpFile.renameTo(outputFile)
            Log.d(TAG, "Download complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")

            preferencesManager.setLocalModelDownloaded(true)
            preferencesManager.setLocalModelDownloadProgress(1f)
            onProgress(DownloadProgress(totalRead, totalBytes, true))
            Result.success(outputFile)

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            preferencesManager.setLocalModelDownloadProgress(0f)
            onProgress(DownloadProgress(0, 0, false, e.message))
            Result.failure(e)
        }
    }

    fun deleteModel(model: LocalModel) {
        getModelFile(model).delete()
        modelsDir().listFiles { f -> f.name.endsWith(".tmp") }?.forEach { it.delete() }
        preferencesManager.setLocalModelDownloaded(false)
        preferencesManager.setLocalModelDownloadProgress(0f)
        Log.d(TAG, "Deleted ${model.filename}")
    }
}
