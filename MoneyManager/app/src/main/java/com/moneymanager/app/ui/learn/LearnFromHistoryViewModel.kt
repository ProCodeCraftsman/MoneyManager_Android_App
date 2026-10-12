package com.moneymanager.app.ui.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.domain.ai.DraftLearning
import com.moneymanager.domain.learn.HistoryLabels
import com.moneymanager.domain.learn.LearnFromHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LearnPhase { Idle, Scanning, Ready, Saved }

data class LearnFromHistoryUiState(
    val phase: LearnPhase = LearnPhase.Idle,
    val labels: HistoryLabels? = null,
    val merchantPicks: Set<String> = emptySet(),
    val sourcePicks: Set<String> = emptySet(),
    val savedCount: Int = 0,
    val permissionDenied: Boolean = false,
)

/**
 * Drives the "Learned from your history" confirm screen (F08 L2). The scan itself writes nothing; only the
 * labels the user keeps are written to merchant memory and account links, and only on [confirm].
 */
@HiltViewModel
class LearnFromHistoryViewModel @Inject constructor(
    private val learnFromHistory: LearnFromHistoryUseCase,
    private val draftLearning: DraftLearning,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnFromHistoryUiState())
    val uiState: StateFlow<LearnFromHistoryUiState> = _uiState.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        if (granted) scan() else _uiState.update { it.copy(permissionDenied = true) }
    }

    fun scan() {
        _uiState.update { it.copy(phase = LearnPhase.Scanning, permissionDenied = false) }
        viewModelScope.launch {
            val labels = withContext(Dispatchers.IO) { learnFromHistory() }
            // Only labels that agree on every kept pair start ticked; anything less is the user's call.
            _uiState.value = LearnFromHistoryUiState(
                phase = LearnPhase.Ready,
                labels = labels,
                merchantPicks = labels.merchants.filter { preTick(it.count, it.unanimous) }.map { it.merchantKey }.toSet(),
                sourcePicks = labels.sources.filter { preTick(it.count, it.unanimous) }.map { it.sourceKey }.toSet(),
            )
        }
    }

    fun toggleMerchant(merchantKey: String) {
        _uiState.update { it.copy(merchantPicks = it.merchantPicks.toggle(merchantKey)) }
    }

    fun toggleSource(sourceKey: String) {
        _uiState.update { it.copy(sourcePicks = it.sourcePicks.toggle(sourceKey)) }
    }

    fun confirm() {
        val state = _uiState.value
        val labels = state.labels ?: return
        val merchants = labels.merchants.filter { it.merchantKey in state.merchantPicks }
        val sources = labels.sources.filter { it.sourceKey in state.sourcePicks }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                merchants.forEach { draftLearning.learnMerchant(it.merchantHint, it.categoryId, it.categoryName, it.typeId) }
                sources.forEach { draftLearning.learnSourceAccount(it.sourceKey, it.accountId) }
            }
            _uiState.update { it.copy(phase = LearnPhase.Saved, savedCount = merchants.size + sources.size) }
        }
    }

    private fun preTick(count: Int, unanimous: Boolean): Boolean = unanimous && count >= PRETICK_MIN_COUNT

    private fun Set<String>.toggle(key: String): Set<String> = if (key in this) this - key else this + key

    private companion object {
        const val PRETICK_MIN_COUNT = 3
    }
}
