package com.moneymanager.app.ui.aidraft

import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.domain.ai.TransactionDraft

data class AiDraftUiState(
    val isGenerating: Boolean = false,
    val generatingStep: String? = null,
    val agentStep: AgentStep? = null,
    val error: String? = null,
    val isAiAvailable: Boolean = false,
    val aiBackendTier: AiBackend = AiBackend.NONE,
    val isLocalModelDownloaded: Boolean = false,
    val localModelDownloadProgress: Float = 0f,
    val isDownloadingLocalModel: Boolean = false,
)

enum class AgentStep(val label: String) {
    READING("Reading input…"),
    EXTRACTING("Extracting details…"),
    SEARCHING_CACHE("Checking history…"),
    LOADING_CONTEXT("Loading accounts & categories…"),
    CLASSIFYING("AI is classifying…"),
    VALIDATING("Validating draft…"),
    RETRIEVING_MISSING("Resolving entities…"),
    FIXING("Fixing AI output…"),
    FINALIZING("Finalizing…"),
    AUTO_COMMITTING("Auto-committing high confidence draft…")
}

sealed class AgentStatus {
    data class Progress(val step: AgentStep, val detail: String? = null) : AgentStatus()
    data class Success(val draft: TransactionDraft) : AgentStatus()
    data class Failure(val throwable: Throwable) : AgentStatus()
}

sealed class NavigationEvent {
    data class NavigateToDraft(val draft: TransactionDraft) : NavigationEvent()
    data class NavigateToCreated(val transactionId: Long, val message: String = "") : NavigationEvent()
    object NavigateBack : NavigationEvent()
    data class ShowError(val message: String) : NavigationEvent()
}
