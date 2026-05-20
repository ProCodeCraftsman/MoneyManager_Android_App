package com.moneymanager.app.ui.aidraft

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.domain.ai.AiConversationEntry
import com.moneymanager.domain.repository.AiConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AiHistoryUiState(
    val conversations: List<AiConversationEntry> = emptyList(),
    val selectedConversation: AiConversationEntry? = null,
    val showDetail: Boolean = false,
)

@HiltViewModel
class AiHistoryViewModel @Inject constructor(
    private val repository: AiConversationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiHistoryUiState())
    val uiState: StateFlow<AiHistoryUiState> = _uiState.asStateFlow()

    val conversations = repository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectConversation(entry: AiConversationEntry) {
        _uiState.value = _uiState.value.copy(selectedConversation = entry, showDetail = true)
    }

    fun dismissDetail() {
        _uiState.value = _uiState.value.copy(selectedConversation = null, showDetail = false)
    }

    fun deleteConversation(id: Long) {
        viewModelScope.launch { repository.deleteById(id) }
    }

    fun deleteAll() {
        viewModelScope.launch { repository.deleteAll() }
    }
}
