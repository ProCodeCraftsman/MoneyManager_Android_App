package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.*
import com.moneymanager.domain.repository.*
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val filterType: String = "",
    val filterAccountId: Long? = null,
    val filterCategoryId: Long? = null,
    val filterTagId: Long? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val allTags: List<TagEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val allAccounts: List<AccountEntity> = emptyList(),
    val allGoals: List<GoalEntity> = emptyList(),
    val currency: String = "USD",
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _filters = MutableStateFlow(FilterState("", null, null, null, null, null))

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "USD")

    val allTags: StateFlow<List<TagEntity>> = categoryRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<GoalEntity>> = goalRepository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _searchQuery,
        _filters,
        allTags,
        allCategories,
        allAccounts,
        allGoals,
        preferencesManager.currency
    ) { array ->
        val txs = array[0] as List<TransactionEntity>
        val q = array[1] as String
        val f = array[2] as FilterState
        val t = array[3] as List<TagEntity>
        val c = array[4] as List<CategoryEntity>
        val a = array[5] as List<AccountEntity>
        val g = array[6] as List<GoalEntity>
        val curr = array[7] as String

        val filtered = txs
            .filter { !it.isSplitChild } // hide split children from main list
            .filter { tx ->
                val noteMatches = q.isEmpty() ||
                    tx.note.contains(q, ignoreCase = true) ||
                    tx.amount.toString().contains(q)
                val typeMatches = f.type.isEmpty() || tx.type == f.type
                val accountMatches = f.accountId == null || tx.accountId == f.accountId
                val categoryMatches = f.categoryId == null || tx.categoryId == f.categoryId
                val tagMatches = f.tagId == null ||
                    (tx.tagIds.isNotEmpty() && tx.tagIds.split(",").contains(f.tagId.toString()))
                val startDateMatches = f.startDate == null || tx.date >= f.startDate
                val endDateMatches = f.endDate == null || tx.date <= f.endDate
                noteMatches && typeMatches && accountMatches && categoryMatches && tagMatches && startDateMatches && endDateMatches
            }
            .sortedByDescending { it.date }
        TransactionsUiState(
            transactions = filtered,
            isLoading = false,
            searchQuery = q,
            filterType = f.type,
            filterAccountId = f.accountId,
            filterCategoryId = f.categoryId,
            filterTagId = f.tagId,
            filterStartDate = f.startDate,
            filterEndDate = f.endDate,
            allTags = t,
            allCategories = c,
            allAccounts = a,
            allGoals = g,
            currency = curr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState(),
    )

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setTypeFilter(type: String) { _filters.value = _filters.value.copy(type = type) }
    fun setAccountFilter(id: Long?) { _filters.value = _filters.value.copy(accountId = id) }
    fun setCategoryFilter(id: Long?) { _filters.value = _filters.value.copy(categoryId = id) }
    fun setTagFilter(id: Long?) { _filters.value = _filters.value.copy(tagId = id) }
    fun setDateRangeFilter(start: Long?, end: Long?) { _filters.value = _filters.value.copy(startDate = start, endDate = end) }
    fun clearAllFilters() { _filters.value = FilterState("", null, null, null, null, null) }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
            adjustBalance(transaction, reverse = false)
        }
    }

    /** Save a split: inserts parent + children, updates balance once for the total. */
    fun addSplitTransaction(parent: TransactionEntity, children: List<TransactionEntity>) {
        viewModelScope.launch {
            val parentId = transactionRepository.insertTransaction(parent.copy(isSplitParent = true))
            children.forEach { child ->
                transactionRepository.insertTransaction(child.copy(isSplitChild = true, parentTransactionId = parentId))
            }
            adjustBalance(parent, reverse = false)
        }
    }

    fun getSplitChildren(parentId: Long): Flow<List<TransactionEntity>> {
        return transactionRepository.getAllTransactions().map { txs ->
            txs.filter { it.isSplitChild && it.parentTransactionId == parentId }
        }
    }

    fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            val tx = TransactionEntity(
                accountId = fromAccountId,
                toAccountId = toAccountId,
                type = "transfer",
                isTransfer = true,
                amount = amount,
                note = note,
                date = date
            )
            transactionRepository.insertTransaction(tx)
            accountRepository.updateAccountBalance(fromAccountId, -amount)
            accountRepository.updateAccountBalance(toAccountId, amount)
        }
    }

    fun updateTransaction(old: TransactionEntity, new: TransactionEntity, children: List<TransactionEntity>? = null) {
        viewModelScope.launch {
            adjustBalance(old, reverse = true)
            transactionRepository.updateTransaction(new)
            
            if (new.isSplitParent && children != null) {
                // Simplified: delete old children and insert new ones
                transactionRepository.getAllTransactions().first()
                    .filter { it.isSplitChild && it.parentTransactionId == old.id }
                    .forEach { transactionRepository.deleteTransaction(it) }
                
                children.forEach { child ->
                    transactionRepository.insertTransaction(child.copy(isSplitChild = true, parentTransactionId = new.id))
                }
            }

            adjustBalance(new, reverse = false)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            // Delete split children if this is a parent
            if (transaction.isSplitParent) {
                transactionRepository.getAllTransactions().first()
                    .filter { it.parentTransactionId == transaction.id }
                    .forEach { transactionRepository.deleteTransaction(it) }
            }
            adjustBalance(transaction, reverse = true)
            transactionRepository.deleteTransaction(transaction)
        }
    }

    fun duplicateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            val copy = transaction.copy(
                id = 0,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            val newId = transactionRepository.insertTransaction(copy)
            
            if (transaction.isSplitParent) {
                // Also duplicate children
                val children = transactionRepository.getAllTransactions().first()
                    .filter { it.isSplitChild && it.parentTransactionId == transaction.id }
                
                children.forEach { child ->
                    transactionRepository.insertTransaction(
                        child.copy(
                            id = 0,
                            parentTransactionId = newId,
                            date = copy.date,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }

            adjustBalance(copy, reverse = false)
        }
    }

    fun createTag(name: String) {
        viewModelScope.launch {
            categoryRepository.insertTag(TagEntity(name = name, color = "#607D8B"))
        }
    }

    private suspend fun adjustBalance(tx: TransactionEntity, reverse: Boolean) {
        val sign = if (reverse) -1.0 else 1.0
        when (tx.type) {
            "income" -> accountRepository.updateAccountBalance(tx.accountId, sign * tx.amount)
            "expense", "savings" -> accountRepository.updateAccountBalance(tx.accountId, -sign * tx.amount)
            "transfer" -> {
                if (!reverse) {
                    accountRepository.updateAccountBalance(tx.accountId, -tx.amount)
                    tx.toAccountId?.let { accountRepository.updateAccountBalance(it, tx.amount) }
                } else {
                    accountRepository.updateAccountBalance(tx.accountId, tx.amount)
                    tx.toAccountId?.let { accountRepository.updateAccountBalance(it, -tx.amount) }
                }
            }
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
