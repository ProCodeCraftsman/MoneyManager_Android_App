package com.moneymanager.data.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class DownloadResult(
    val success: Boolean,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val error: String? = null,
)

object ModelDownloader {

    private const val TAG = "ModelDownloader"
    private const val BUFFER_SIZE = 32 * 1024
    private const val PROGRESS_INTERVAL_MS = 250L

    suspend fun downloadFile(
        urlString: String,
        outputFile: File,
        totalBytesHint: Long,
        onProgress: suspend (Long, Long) -> Unit,
        isCancelled: () -> Boolean = { false },
        accessToken: String? = null,
    ): DownloadResult = withContext(Dispatchers.IO) {
        val tmpFile = File(outputFile.parentFile, "${outputFile.name}.tmp")
        var totalRead = 0L
        var lastReportMs = 0L

        try {
            if (!outputFile.parentFile?.exists()!!) outputFile.parentFile!!.mkdirs()
            if (outputFile.exists() && outputFile.length() > 100_000L) {
                Log.d(TAG, "File already exists at ${outputFile.absolutePath}")
                val len = outputFile.length()
                return@withContext DownloadResult(true, len, len)
            }

            val resumeFrom = if (tmpFile.exists()) tmpFile.length() else 0L
            if (resumeFrom > 0L) {
                Log.d(TAG, "Resuming download from $resumeFrom bytes")
                totalRead = resumeFrom
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("User-Agent", "MoneyManager/1.0")
            if (accessToken != null && accessToken.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (resumeFrom > 0L) {
                connection.setRequestProperty("Range", "bytes=$resumeFrom-")
            }
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK &&
                responseCode != HttpURLConnection.HTTP_PARTIAL) {
                val msg = "HTTP $responseCode: ${connection.responseMessage}"
                Log.e(TAG, msg)
                return@withContext DownloadResult(false, totalRead, 0, msg)
            }

            val totalBytes = connection.contentLengthLong.let {
                if (it > 0) it + resumeFrom else totalBytesHint
            }

            val inputStream = connection.inputStream
            val outputStream = if (resumeFrom > 0L) {
                FileOutputStream(tmpFile, true)
            } else {
                FileOutputStream(tmpFile)
            }
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (isCancelled()) {
                    outputStream.close()
                    inputStream.close()
                    return@withContext DownloadResult(false, totalRead, totalBytes, "Cancelled")
                }
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val now = System.currentTimeMillis()
                if (now - lastReportMs > PROGRESS_INTERVAL_MS) {
                    lastReportMs = now
                    onProgress(totalRead, totalBytes)
                }
            }
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            if (totalBytes > 0 && totalRead < totalBytes * 0.99) {
                Log.e(TAG, "Incomplete download: $totalRead / $totalBytes bytes")
                return@withContext DownloadResult(false, totalRead, totalBytes, "Incomplete download")
            }

            tmpFile.renameTo(outputFile)
            Log.d(TAG, "Download complete: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
            DownloadResult(true, totalRead, totalBytes)

        } catch (e: Exception) {
            Log.e(TAG, "Download failed", e)
            DownloadResult(false, totalRead, 0, e.message ?: "Unknown error")
        }
    }
}
