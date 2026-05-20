package com.moneymanager.app.ui.aimodels

data class ModelDownloadProgress(
    val receivedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val bytesPerSecond: Long = 0L,
    val remainingMs: Long = 0L,
) {
    val progress: Float
        get() = if (totalBytes > 0L) (receivedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}
