package com.moneymanager.domain.ai

import kotlinx.coroutines.flow.Flow

interface ModelDownloadManager {
    fun download(): Flow<DownloadProgress>
    suspend fun isModelDownloaded(): Boolean
    suspend fun getModelPath(): String
    suspend fun cancelDownload()
    suspend fun deleteModel()
}
