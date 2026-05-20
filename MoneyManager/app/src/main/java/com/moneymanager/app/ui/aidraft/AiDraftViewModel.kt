package com.moneymanager.app.ui.aidraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.ai.DeterministicExtractor
import com.moneymanager.data.ai.LiteRtModelManager
import com.moneymanager.data.ai.agent.JsSkillEngine
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
    private val generateDraftFromTextUseCase: GenerateDraftFromTextUseCase,
    private val generateDraftFromImageUseCase: GenerateDraftFromImageUseCase,
    private val promptContextBuilder: PromptContextBuilder,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val peerContactRepository: PeerContactRepository,
    private val transactionRepository: TransactionRepository,
    private val modelManager: LiteRtModelManager,
    private val merchantMemory: MerchantCategoryMemoryRepository,
    private val jsSkillEngine: JsSkillEngine,
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
            _uiState.update { it.copy(isGenerating = true, error = null, generatingStep = "Reading…") }
            try {
                // Phase 1 — deterministic, zero-latency: extract amount/type/date/merchant
                // before touching the DB or AI. Handles multi-number bills correctly.
                val pre = DeterministicExtractor.extract(rawText, sourceType)

                val foundLabel = buildString {
                    pre.amount?.let { append(formatAmount(it)) }
                    pre.typeId?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.replaceFirstChar { c -> c.uppercase() })
                    }
                    pre.merchantHint?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it.take(20))
                    }
                }
                val contextStep = if (foundLabel.isNotEmpty()) "Found $foundLabel — loading context…" else "Loading your data…"
                _uiState.update { it.copy(generatingStep = contextStep) }

                // Phase 2 — merchant cache check: known merchant → skip AI entirely
                val cachedEntry = pre.merchantHint?.let { hint ->
                    withContext(Dispatchers.IO) { merchantMemory.lookup(hint) }
                }
                if (cachedEntry != null) {
                    val fastDraft = com.moneymanager.domain.ai.TransactionDraft(
                        amount = pre.amount,
                        typeId = pre.typeId ?: cachedEntry.typeId,
                        date = pre.epochMs,
                        categoryId = cachedEntry.categoryId,
                        categoryName = cachedEntry.categoryName,
                        merchantHint = pre.merchantHint,
                        sourceType = sourceType,
                        sourceSender = sourceSender,
                        receiptPath = attachmentPath,
                    )
                    _navigationEvent.emit(NavigationEvent.NavigateToDraft(fastDraft))
                    _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = null) }
                    return@launch
                }

                // Phase 4 — load prompt context (cached after first call)
                val promptContext = loadPromptContext()

                // Phase 5 — single focused AI call (smaller prompt = faster inference)
                val thinkingLabel = buildString {
                    if (foundLabel.isNotEmpty()) { append("Found $foundLabel — ") }
                    append(when (_uiState.value.aiBackendTier) {
                        AiBackend.LOCAL_MODEL -> "local AI classifying…"
                        AiBackend.AICORE -> "AI classifying…"
                        else -> "classifying…"
                    })
                }
                _uiState.update { it.copy(generatingStep = thinkingLabel) }

                val result = withContext(Dispatchers.IO) {
                    generateDraftFromTextUseCase(rawText, promptContext, sourceType, sourceSender, pre)
                }

                result.fold(
                    onSuccess = { draft ->
                        val finalDraft = draft.copy(
                            receiptPath = attachmentPath ?: draft.receiptPath,
                            merchantHint = pre.merchantHint ?: draft.merchantHint,
                        )
                        // Learn: write merchant→category association for future cache hits
                        if (pre.merchantHint != null && finalDraft.categoryId != null) {
                            viewModelScope.launch(Dispatchers.IO) {
                                merchantMemory.record(
                                    merchantHint = pre.merchantHint,
                                    categoryId = finalDraft.categoryId,
                                    categoryName = finalDraft.categoryName ?: "",
                                    typeId = finalDraft.typeId,
                                )
                            }
                        }
                        _navigationEvent.emit(NavigationEvent.NavigateToDraft(finalDraft))
                        _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = null) }
                    },
                    onFailure = { throwable ->
                        val error = when {
                            throwable is com.moneymanager.domain.ai.AiUnavailableException &&
                                _uiState.value.aiBackendTier == AiBackend.LOCAL_MODEL &&
                                !_uiState.value.isLocalModelDownloaded ->
                                com.moneymanager.domain.ai.AiError.LocalModelNotDownloaded
                            throwable is com.moneymanager.domain.ai.AiUnavailableException ->
                                com.moneymanager.domain.ai.AiError.Generic("Could not reach AI. Please try again.")
                            else ->
                                com.moneymanager.domain.ai.AiError.InsufficientInformation
                        }
                        _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                        _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = error.userMessage()) }
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val error = com.moneymanager.domain.ai.AiError.AiCallTimedOut
                _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = error.userMessage()) }
            } catch (e: Exception) {
                val error = com.moneymanager.domain.ai.AiError.Generic("Could not generate draft. Please try again.")
                _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = error.userMessage()) }
            }
        }
    }

    fun quickAddFromVoice(rawText: String, saveAsNote: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generatingStep = "Reading…") }
            try {
                val pre = DeterministicExtractor.extract(rawText, "VOICE")
                val label = buildString {
                    pre.amount?.let { append(formatAmount(it)) }
                    if (pre.typeId != null) {
                        if (isNotEmpty()) append(" · ")
                        append(pre.typeId.replaceFirstChar { c -> c.uppercase() })
                    }
                }
                _uiState.update { it.copy(generatingStep = "Creating $label…") }

                val typeId = pre.typeId ?: "expense"
                val epochMs = pre.epochMs ?: System.currentTimeMillis()
                val description = pre.merchantHint?.take(40) ?: rawText.take(40)

                val inputJson = buildString {
                    append("""{"amount":${pre.amount ?: 0},"type":"$typeId","description":"${jsonEscape(description)}","date":$epochMs,"accountName":null,"categoryName":null,"note":"${jsonEscape(if (saveAsNote) rawText else "")}","peerName":null}""")
                }

                val result = withContext(Dispatchers.IO) {
                    jsSkillEngine.execute("create-transaction", inputJson)
                }

                val parsed = runCatching {
                    kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                        .decodeFromString<com.moneymanager.data.http.CreateTransactionResponse>(result)
                }.getOrNull()

                if (parsed?.status == "created") {
                    _navigationEvent.emit(NavigationEvent.NavigateToCreated(parsed.id, label))
                } else {
                    val errorMsg = parsed?.error ?: "Failed to create transaction"
                    _navigationEvent.emit(NavigationEvent.ShowError(errorMsg))
                }
            } catch (e: Exception) {
                _navigationEvent.emit(NavigationEvent.ShowError("Could not create: ${e.message?.take(80) ?: "Unknown error"}"))
            } finally {
                _uiState.update { it.copy(isGenerating = false, generatingStep = null) }
            }
        }
    }

    private fun jsonEscape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")

    fun generateDraftFromImage(imageBytes: ByteArray, sourceType: String = "RECEIPT", attachmentPath: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, generatingStep = "Analyzing receipt image…") }
            try {
                val promptContext = loadPromptContext()
                _uiState.update { it.copy(generatingStep = "AI vision reading receipt…") }

                val result = withContext(Dispatchers.IO) {
                    generateDraftFromImageUseCase(imageBytes, promptContext, sourceType)
                }

                result.fold(
                    onSuccess = { draft ->
                        val finalDraft = draft.copy(receiptPath = attachmentPath ?: draft.receiptPath)
                        if (draft.merchantHint != null && draft.categoryId != null &&
                            draft.confidence["merchant"] != "low") {
                            viewModelScope.launch(Dispatchers.IO) {
                                merchantMemory.record(
                                    merchantHint = draft.merchantHint,
                                    categoryId = draft.categoryId,
                                    categoryName = draft.categoryName ?: "",
                                    typeId = draft.typeId,
                                )
                            }
                        }
                        _navigationEvent.emit(NavigationEvent.NavigateToDraft(finalDraft))
                        _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = null) }
                    },
                    onFailure = { throwable ->
                        val error = when {
                            throwable is com.moneymanager.domain.ai.AiUnavailableException ->
                                com.moneymanager.domain.ai.AiError.Generic("Could not analyze image. Please try again.")
                            throwable is UnsupportedOperationException ->
                                com.moneymanager.domain.ai.AiError.Generic("This model does not support image analysis.")
                            else ->
                                com.moneymanager.domain.ai.AiError.InsufficientInformation
                        }
                        _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                        _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = error.userMessage()) }
                    }
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                val error = com.moneymanager.domain.ai.AiError.AiCallTimedOut
                _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = error.userMessage()) }
            } catch (e: Exception) {
                val error = com.moneymanager.domain.ai.AiError.Generic("Could not analyze image. Please try again.")
                _navigationEvent.emit(NavigationEvent.ShowError(error.userMessage()))
                _uiState.update { it.copy(isGenerating = false, generatingStep = null, error = error.userMessage()) }
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
            val accountEntries = accounts.map { AccountEntry(id = it.id, name = it.name) }
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

    private fun formatAmount(amount: Double): String =
        if (amount == kotlin.math.floor(amount)) "₹${amount.toLong()}"
        else "₹${"%.2f".format(amount)}"
}
