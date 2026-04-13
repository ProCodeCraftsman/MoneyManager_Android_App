package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterType: String = "",
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("")

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _searchQuery,
        _filterType,
    ) { transactions, query, type ->
        val filtered = transactions.filter { tx ->
            val noteMatches = (query.isEmpty() || tx.note.contains(query, ignoreCase = true))
            val typeMatches = (type.isEmpty() || tx.type == type)
            noteMatches && typeMatches
        }
        TransactionsUiState(
            transactions = filtered,
            isLoading = false,
            searchQuery = query,
            filterType = type,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(),
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
        }
    }
}