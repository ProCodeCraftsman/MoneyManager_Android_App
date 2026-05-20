package com.moneymanager.app.ui.aidraft

import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.domain.ai.TransactionDraft

data class AiDraftUiState(
    val isGenerating: Boolean = false,
    val generatingStep: String? = null,
    val error: String? = null,
    val isAiAvailable: Boolean = false,
    val aiBackendTier: AiBackend = AiBackend.NONE,
    val isLocalModelDownloaded: Boolean = false,
    val localModelDownloadProgress: Float = 0f,
    val isDownloadingLocalModel: Boolean = false,
)

sealed class NavigationEvent {
    data class NavigateToDraft(val draft: TransactionDraft) : NavigationEvent()
    data class NavigateToCreated(val transactionId: Long, val message: String = "") : NavigationEvent()
    object NavigateBack : NavigationEvent()
    data class ShowError(val message: String) : NavigationEvent()
}
