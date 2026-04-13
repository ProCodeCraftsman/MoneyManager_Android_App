package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.CategoryRepository
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
    // Filter state
    val filterAccountId: Long? = null,
    val filterCategoryId: Long? = null,
    val filterTagId: Long? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    // All available options for filtering
    val allTags: List<TagEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    
    // Use a data class to hold all filter state - simpler than combine
    private val _filters = MutableStateFlow(FilterState("", null, null, null, null, null))

    // Expose all tags and categories for filter UI
    val allTags: StateFlow<List<TagEntity>> = categoryRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _searchQuery,
        _filters,
        allTags,
        allCategories
    ) { transactions, query, filters, tags, categories ->
        val filtered = transactions.filter { tx ->
            val noteMatches = query.isEmpty() || 
                tx.note.contains(query, ignoreCase = true) ||
                tx.amount.toString().contains(query)
            val typeMatches = filters.type.isEmpty() || tx.type == filters.type
            val accountMatches = filters.accountId == null || tx.accountId == filters.accountId
            val categoryMatches = filters.categoryId == null || tx.categoryId == filters.categoryId
            val tagMatches = filters.tagId == null || 
                (tx.tagIds.isNotEmpty() && tx.tagIds.split(",").contains(filters.tagId.toString()))
            val startDateMatches = filters.startDate == null || tx.date >= filters.startDate
            val endDateMatches = filters.endDate == null || tx.date <= filters.endDate
            noteMatches && typeMatches && accountMatches && categoryMatches && tagMatches && startDateMatches && endDateMatches
        }
        TransactionsUiState(
            transactions = filtered,
            isLoading = false,
            searchQuery = query,
            filterType = filters.type,
            filterAccountId = filters.accountId,
            filterCategoryId = filters.categoryId,
            filterTagId = filters.tagId,
            filterStartDate = filters.startDate,
            filterEndDate = filters.endDate,
            allTags = tags,
            allCategories = categories,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(),
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setTypeFilter(type: String) {
        _filters.value = _filters.value.copy(type = type)
    }

    fun setAccountFilter(accountId: Long?) {
        _filters.value = _filters.value.copy(accountId = accountId)
    }

    fun setCategoryFilter(categoryId: Long?) {
        _filters.value = _filters.value.copy(categoryId = categoryId)
    }

    fun setTagFilter(tagId: Long?) {
        _filters.value = _filters.value.copy(tagId = tagId)
    }

    fun setDateRangeFilter(startDate: Long?, endDate: Long?) {
        _filters.value = _filters.value.copy(startDate = startDate, endDate = endDate)
    }

    fun clearAllFilters() {
        _filters.value = FilterState("", null, null, null, null, null)
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
        }
    }
}

private data class FilterState(
    val type: String,
    val accountId: Long?,
    val categoryId: Long?,
    val tagId: Long?,
    val startDate: Long?,
    val endDate: Long?
)