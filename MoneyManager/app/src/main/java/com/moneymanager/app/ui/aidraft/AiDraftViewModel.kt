package com.moneymanager.app.ui.aidraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.moneymanager.domain.ai.AiAgent
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.ai.LiteRtModelManager
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.ai.ModelDownloadService
import dagger.hilt.android.qualifiers.ApplicationContext
import com.moneymanager.data.repository.AiAvailabilityRepository
import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.CategoryEntry
import com.moneymanager.domain.ai.GenerateDraftFromImageUseCase
import com.moneymanager.domain.ai.GenerateDraftFromTextUseCase
import com.moneymanager.domain.ai.PeerEntry
import com.moneymanager.domain.ai.PromptContextBuilder
import com.moneymanager.domain.ai.TagEntry
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AiDraftViewModel @Inject constructor(
    private val aiAvailabilityRepository: AiAvailabilityRepository,
    private val aiAgent: AiAgent,
    private val promptContextBuilder: PromptContextBuilder,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val peerContactRepository: PeerContactRepository,
    private val transactionRepository: TransactionRepository,
    private val modelManager: LiteRtModelManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiDraftUiState())
    val uiState: StateFlow<AiDraftUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(replay = 0, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    val isAiAvailable = aiAvailabilityRepository.isAiAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val aiState = aiAvailabilityRepository.aiState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000),
            com.moneymanager.data.repository.AiState())

    private var cachedPromptContext: com.moneymanager.domain.ai.PromptContext? = null

    /** True when the selected model has llm_ask_image in taskTypes (vision-capable). */
    val modelSupportsVision: StateFlow<Boolean> = flow {
        emit(modelManager.selectModelForDevice()?.supportsVision == true)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    init {
        viewModelScope.launch {
            aiAvailabilityRepository.localModelDownloadProgress.collect { progress ->
                if (progress >= 1f) {
                    _uiState.update { it.copy(isDownloadingLocalModel = false, isLocalModelDownloaded = true) }
                }
            }
        }
        viewModelScope.launch {
            aiState.collect { state ->
                _uiState.update {
                    it.copy(
                        isAiAvailable = state.isAvailable,
                        aiBackendTier = state.tier,
                        isLocalModelDownloaded = state.isLocalModelDownloaded,
                        localModelDownloadProgress = state.localModelDownloadProgress,
                    )
                }
            }
        }
    }

    fun generateDraft(rawText: String, sourceType: String, sourceSender: String? = null, attachmentPath: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, agentStep = AgentStep.READING) }
            try {
                val promptContext = loadPromptContext()
                
                aiAgent.processText(
                    rawText = rawText,
                    context = promptContext,
                    sourceType = sourceType,
                    sourceSender = sourceSender,
                    attachmentPath = attachmentPath,
                    autoCommit = sourceType == "VOICE" // Example: auto-commit voice if high confidence
                ).collect { status ->
                    when (status) {
                        is AgentStatus.Progress -> {
                            _uiState.update { it.copy(agentStep = status.step, generatingStep = status.detail) }
                        }
                        is AgentStatus.Success -> {
                            if (status.draft.flags.contains("auto_committed")) {
                                val txId = status.draft.flags.last().toLongOrNull() ?: 0L
                                _navigationEvent.emit(NavigationEvent.NavigateToCreated(txId, "Transaction created automatically"))
                            } else {
                                _navigationEvent.emit(NavigationEvent.NavigateToDraft(status.draft))
                            }
                            _uiState.update { it.copy(isGenerating = false, agentStep = null, generatingStep = null) }
                        }
                        is AgentStatus.Failure -> {
                            val error = mapThrowableToAiError(status.throwable)
                            _uiState.update { it.copy(isGenerating = false, agentStep = null, error = error.userMessage()) }
                            _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, agentStep = null, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    fun quickAdd(rawText: String, sourceType: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, agentStep = AgentStep.READING) }
            try {
                val promptContext = loadPromptContext()
                aiAgent.processText(
                    rawText = rawText,
                    context = promptContext,
                    sourceType = sourceType,
                    autoCommit = true
                ).collect { status ->
                    when (status) {
                        is AgentStatus.Progress -> {
                            _uiState.update { it.copy(agentStep = status.step, generatingStep = status.detail) }
                        }
                        is AgentStatus.Success -> {
                            val txId = status.draft.flags.lastOrNull()?.toLongOrNull() ?: 0L
                            _navigationEvent.emit(NavigationEvent.NavigateToCreated(txId, "Transaction created automatically"))
                            _uiState.update { it.copy(isGenerating = false, agentStep = null, generatingStep = null) }
                        }
                        is AgentStatus.Failure -> {
                            val error = mapThrowableToAiError(status.throwable)
                            _uiState.update { it.copy(isGenerating = false, agentStep = null, error = error.userMessage()) }
                            _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, agentStep = null, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    private fun mapThrowableToAiError(throwable: Throwable): com.moneymanager.domain.ai.AiError {
        return when {
            throwable is com.moneymanager.domain.ai.AiUnavailableException &&
                    _uiState.value.aiBackendTier == AiBackend.LOCAL_MODEL &&
                    !_uiState.value.isLocalModelDownloaded ->
                com.moneymanager.domain.ai.AiError.LocalModelNotDownloaded
            throwable is com.moneymanager.domain.ai.AiUnavailableException ->
                com.moneymanager.domain.ai.AiError.Generic("Could not reach AI. Please try again.")
            else ->
                com.moneymanager.domain.ai.AiError.InsufficientInformation
        }
    }


    fun generateDraftFromImage(imageBytes: ByteArray, sourceType: String = "RECEIPT", attachmentPath: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, agentStep = AgentStep.READING) }
            try {
                val promptContext = loadPromptContext()
                
                aiAgent.processImage(
                    imageBytes = imageBytes,
                    context = promptContext,
                    sourceType = sourceType,
                    attachmentPath = attachmentPath,
                    autoCommit = false // Usually don't auto-commit receipts without review
                ).collect { status ->
                    when (status) {
                        is AgentStatus.Progress -> {
                            _uiState.update { it.copy(agentStep = status.step, generatingStep = status.detail) }
                        }
                        is AgentStatus.Success -> {
                            _navigationEvent.emit(NavigationEvent.NavigateToDraft(status.draft))
                            _uiState.update { it.copy(isGenerating = false, agentStep = null, generatingStep = null) }
                        }
                        is AgentStatus.Failure -> {
                            val error = mapThrowableToAiError(status.throwable)
                            _uiState.update { it.copy(isGenerating = false, agentStep = null, error = error.userMessage()) }
                            _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, agentStep = null, error = "Unexpected error: ${e.message}") }
            }
        }
    }

    private suspend fun loadPromptContext(): com.moneymanager.domain.ai.PromptContext {
        return cachedPromptContext ?: withContext(Dispatchers.IO) {
            val categories = categoryRepository.getAllCategories().first()
            val accounts = accountRepository.getAllAccounts().first()
            val peers = peerContactRepository.getAllPeers().first()
            val transactions = transactionRepository.getAllTransactions().first()
            val tags = categoryRepository.getAllTags().first()

            val categoryUsageCounts = transactions
                .filter { it.categoryId != null && !it.isSplitChild }
                .groupBy { it.categoryId!! }
                .mapValues { it.value.size }

            val categoryEntries = categories.map { CategoryEntry(id = it.id, name = it.name, type = it.type, parentId = it.parentId) }
            val accountEntries = accounts.map { AccountEntry(id = it.id, name = it.name, type = it.type) }
            val peerEntries = peers.map { PeerEntry(id = it.id, name = it.displayName) }
            val tagEntries = tags.map { TagEntry(id = it.id, name = it.name) }

            promptContextBuilder.build(
                categories = categoryEntries,
                categoryUsageCounts = categoryUsageCounts,
                accounts = accountEntries,
                peers = peerEntries,
                tags = tagEntries
            ).also { cachedPromptContext = it }
        }
    }

    fun invalidatePromptCache() {
        cachedPromptContext = null
    }

    fun downloadLocalModel() {
        if (_uiState.value.isDownloadingLocalModel) return
        viewModelScope.launch {
            val model = modelManager.getUserSelectedModel()
            _uiState.update { it.copy(isDownloadingLocalModel = true) }
            ModelDownloadService.start(context, model.name)
        }
    }

    fun deleteLocalModel() {
        viewModelScope.launch {
            val selectedModel = modelManager.selectModelForDevice() ?: return@launch
            modelManager.deleteModel(selectedModel)
            _uiState.update { it.copy(isLocalModelDownloaded = false, localModelDownloadProgress = 0f) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearDraft() {
        _uiState.update { AiDraftUiState() }
    }

}
