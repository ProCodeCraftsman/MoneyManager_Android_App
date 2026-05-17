package com.moneymanager.app.ui.aimodels

import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.ai.ModelEntry

data class AiModelsUiState(
    val backendTier: AiBackend = AiBackend.NONE,
    val aiStatus: String = "PENDING",
    val aiDownloadProgress: Float = 0f,
    val allModels: List<ModelEntry> = emptyList(),
    val selectedLocalModel: ModelEntry? = null,
    val isLocalModelDownloaded: Boolean = false,
    val localModelDownloadProgress: Float = 0f,
    val downloadingModelName: String? = null,
    val wifiOnlyDownload: Boolean = true,
    val hfAccessToken: String = "",
    val isHfTokenValid: Boolean = false,
    val needsHfLogin: Boolean = false,
    val modelDownloadStates: Map<String, Boolean> = emptyMap(),
    val modelProgress: Map<String, ModelDownloadProgress> = emptyMap(),
)
