package com.moneymanager.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.repository.DraftEditStatsRepository
import com.moneymanager.domain.capture.DraftEditStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DraftEditStatsViewModel @Inject constructor(
    private val repository: DraftEditStatsRepository,
) : ViewModel() {

    private val _stats = MutableStateFlow(DraftEditStats())
    val stats: StateFlow<DraftEditStats> = _stats.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { _stats.update { repository.load() } }
    }
}
