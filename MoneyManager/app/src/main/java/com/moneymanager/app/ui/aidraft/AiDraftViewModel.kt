package com.moneymanager.app.ui.aidraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.AiAvailabilityRepository
import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.CategoryEntry
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AiDraftViewModel @Inject constructor(
    private val aiAvailabilityRepository: AiAvailabilityRepository,
    private val generateDraftFromTextUseCase: GenerateDraftFromTextUseCase,
    private val promptContextBuilder: PromptContextBuilder,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val peerContactRepository: PeerContactRepository,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiDraftUiState())
    val uiState: StateFlow<AiDraftUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>(replay = 0, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    val isAiAvailable: StateFlow<Boolean> = aiAvailabilityRepository.isAiAvailable
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            isAiAvailable.collect { available ->
                _uiState.update { it.copy(isAiAvailable = available) }
            }
        }
    }

    fun generateDraft(rawText: String, sourceType: String, sourceSender: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }
            try {
                val promptContext = withContext(Dispatchers.IO) {
                    val categories = categoryRepository.getAllCategories().first()
                    val accounts = accountRepository.getAllAccounts().first()
                    val peers = peerContactRepository.getAllPeers().first()
                    val transactions = transactionRepository.getAllTransactions().first()
                    val tags = categoryRepository.getAllTags().first()

                    val categoryUsageCounts = transactions
                        .filter { it.categoryId != null && !it.isSplitChild }
                        .groupBy { it.categoryId!! }
                        .mapValues { it.value.size }

                    val categoryEntries = categories.map { CategoryEntry(id = it.id, name = it.name, type = it.type) }
                    val accountEntries = accounts.map { AccountEntry(id = it.id, name = it.name) }
                    val peerEntries = peers.map { PeerEntry(id = it.id, name = it.displayName) }
                    val tagEntries = tags.map { TagEntry(id = it.id, name = it.name) }

                    promptContextBuilder.build(
                        categories = categoryEntries,
                        categoryUsageCounts = categoryUsageCounts,
                        accounts = accountEntries,
                        peers = peerEntries,
                        tags = tagEntries
                    )
                }

                val result = withContext(Dispatchers.IO) {
                    generateDraftFromTextUseCase(rawText, promptContext)
                }

                result.fold(
                    onSuccess = { draft ->
                        _navigationEvent.emit(NavigationEvent.NavigateToDraft(draft))
                        _uiState.update { it.copy(isGenerating = false, error = null) }
                    },
                    onFailure = {
                        _navigationEvent.emit(NavigationEvent.ShowError("Could not generate draft. Please enter details manually."))
                        _uiState.update { it.copy(isGenerating = false, error = "Could not generate draft. Please enter details manually.") }
                    }
                )
            } catch (e: Exception) {
                _navigationEvent.emit(NavigationEvent.ShowError("Could not generate draft. Please enter details manually."))
                _uiState.update { it.copy(isGenerating = false, error = "Could not generate draft. Please enter details manually.") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearDraft() {
        _uiState.update { AiDraftUiState() }
    }
}
