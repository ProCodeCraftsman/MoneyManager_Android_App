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
    /** Names of models currently RUNNING or ENQUEUED in WorkManager. */
    val downloadingModelNames: Set<String> = emptySet(),
    val wifiOnlyDownload: Boolean = true,
    val hfAccessToken: String = "",
    val isHfTokenValid: Boolean = false,
    /** true=downloaded, false=not downloaded keyed by model.name */
    val modelDownloadStates: Map<String, Boolean> = emptyMap(),
    val modelProgress: Map<String, ModelDownloadProgress> = emptyMap(),
    /** Models where an older version exists on disk — show Update button. */
    val updatableModelNames: Set<String> = emptySet(),
)
