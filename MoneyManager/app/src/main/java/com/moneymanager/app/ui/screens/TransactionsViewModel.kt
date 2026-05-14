package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.GoalRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TransactionSort {
    NEWEST, OLDEST, HIGHEST, LOWEST
}

data class TransactionsUiState(
    val searchQuery: String = "",
    val filterType: String = "",
    val filterAccountId: Long? = null,
    val filterCategoryId: Long? = null,
    val filterTagId: Long? = null,
    val filterGoalId: Long? = null,
    val filterPeerId: Long? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val isAllExpanded: Boolean = true,
    val sortBy: TransactionSort = TransactionSort.NEWEST,
    val showSummary: Boolean = true,
    val showCategories: Boolean = true,
    val allTags: List<TagEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val allAccounts: List<AccountEntity> = emptyList(),
    val allGoals: List<GoalEntity> = emptyList(),
    val allPeers: List<PeerContact> = emptyList(),
    val currency: String = "INR",
    val categoryUsageCounts: Map<Long, Int> = emptyMap(),
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager,
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    private val _filters = MutableStateFlow(FilterState("", null, null, null, null, null, null))
    private val _isAllExpanded = MutableStateFlow(true)
    private val _sortBy = MutableStateFlow(TransactionSort.NEWEST)
    private val _showSummary = MutableStateFlow(true)
    private val _showCategories = MutableStateFlow(true)

    val currency: StateFlow<String> = preferencesManager.currency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "INR")

    val allTags: StateFlow<List<TagEntity>> = categoryRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = categoryRepository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAccounts: StateFlow<List<AccountEntity>> = accountRepository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allGoals: StateFlow<List<GoalEntity>> = goalRepository.getAllGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPeers: StateFlow<List<PeerContact>> = peerContactRepository.getAllPeers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Paginated transaction stream — reacts to filter changes via flatMapLatest. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val transactionsPagingData: Flow<PagingData<TransactionEntity>> =
        combine(_filters, _searchQuery) { filters, query ->
            filters to query
        }.flatMapLatest { (filters, query) ->
            transactionRepository.getTransactionsPaged(
                accountId = filters.accountId,
                type = filters.type.takeIf { it.isNotEmpty() && it != "All" },
                categoryId = filters.categoryId,
                goalId = filters.goalId,
                tagId = filters.tagId,
                startDate = filters.startDate,
                endDate = filters.endDate,
                query = query
            )
        }.cachedIn(viewModelScope)

    val categoryUsageCounts: Flow<Map<Long, Int>> = transactionRepository.getAllTransactions()
        .map { txs ->
            txs.filter { it.categoryId != null && !it.isSplitChild }
                .groupBy { it.categoryId!! }
                .mapValues { it.value.size }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val uiState: StateFlow<TransactionsUiState> = combine(
        _searchQuery,
        _filters,
        allTags,
        allCategories,
        allAccounts,
        allGoals,
        allPeers,
        preferencesManager.currency,
        _isAllExpanded,
        _sortBy,
        _showSummary,
        _showCategories,
        categoryUsageCounts
    ) { array ->
        val q = array[0] as String
        val f = array[1] as FilterState
        @Suppress("UNCHECKED_CAST")
        val t = array[2] as List<TagEntity>
        @Suppress("UNCHECKED_CAST")
        val c = array[3] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val a = array[4] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val g = array[5] as List<GoalEntity>
        @Suppress("UNCHECKED_CAST")
        val p = array[6] as List<PeerContact>
        val curr = array[7] as String
        val expanded = array[8] as Boolean
        val sort = array[9] as TransactionSort
        val summary = array[10] as Boolean
        val cats = array[11] as Boolean
        @Suppress("UNCHECKED_CAST")
        val counts = array[12] as Map<Long, Int>

        TransactionsUiState(
            searchQuery = q,
            filterType = f.type,
            filterAccountId = f.accountId,
            filterCategoryId = f.categoryId,
            filterTagId = f.tagId,
            filterGoalId = f.goalId,
            filterPeerId = f.peerId,
            filterStartDate = f.startDate,
            filterEndDate = f.endDate,
            isAllExpanded = expanded,
            sortBy = sort,
            showSummary = summary,
            showCategories = cats,
            allTags = t,
            allCategories = c,
            allAccounts = a,
            allGoals = g,
            allPeers = p,
            currency = curr,
            categoryUsageCounts = counts
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
    fun setGoalFilter(id: Long?) { _filters.value = _filters.value.copy(goalId = id) }
    fun setPeerFilter(id: Long?) { _filters.value = _filters.value.copy(peerId = id) }
    fun setDateRangeFilter(start: Long?, end: Long?) { _filters.value = _filters.value.copy(startDate = start, endDate = end) }
    fun toggleAllExpanded() { _isAllExpanded.value = !_isAllExpanded.value }
    fun setSortBy(sort: TransactionSort) { _sortBy.value = sort }
    fun setShowSummary(show: Boolean) { _showSummary.value = show }
    fun setShowCategories(show: Boolean) { _showCategories.value = show }
    fun clearAllFilters() { 
        _filters.value = FilterState("", null, null, null, null, null, null, null)
        _sortBy.value = TransactionSort.NEWEST
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.type == "transfer") {
                val toId = transaction.toAccountId ?: return@launch
                val outNote = transaction.note.ifEmpty { "Transfer to Account" }
                val inNote = transaction.note.ifEmpty { "Transfer from Account" }
                transactionRepository.insertTransaction(transaction.copy(note = outNote))
                transactionRepository.insertTransaction(transaction.copy(
                    id = 0, accountId = toId, toAccountId = transaction.accountId, note = inNote
                ))
                accountRepository.updateAccountBalance(transaction.accountId, -transaction.amount)
                accountRepository.updateAccountBalance(toId, transaction.amount)
            } else {
                transactionRepository.insertTransaction(transaction)
                adjustBalance(transaction, reverse = false)
                updatePeerBalance(transaction, reverse = false)
            }
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
            updatePeerBalance(parent, reverse = false)
        }
    }


    fun getSplitChildren(parentId: Long): Flow<List<TransactionEntity>> {
        return transactionRepository.getSplitChildren(parentId)
    }

    fun addTransfer(fromAccountId: Long, toAccountId: Long, amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            // Create OUT transaction for source account
            val sourceTx = TransactionEntity(
                accountId = fromAccountId,
                toAccountId = toAccountId,
                type = "transfer",
                isTransfer = true,
                amount = amount,
                note = note.ifEmpty { "Transfer to Account" },
                date = date
            )
            transactionRepository.insertTransaction(sourceTx)

            // Create IN transaction for destination account
            val destTx = TransactionEntity(
                accountId = toAccountId,
                toAccountId = fromAccountId,
                type = "transfer",
                isTransfer = true,
                amount = amount,
                note = note.ifEmpty { "Transfer from Account" },
                date = date
            )
            transactionRepository.insertTransaction(destTx)

            accountRepository.updateAccountBalance(fromAccountId, -amount)
            accountRepository.updateAccountBalance(toAccountId, amount)
        }
    }

    fun updateTransaction(old: TransactionEntity, new: TransactionEntity, children: List<TransactionEntity>? = null) {
        viewModelScope.launch {
            if (old.type == "transfer") {
                if (old.receiptPath != null && old.receiptPath != new.receiptPath) {
                    FileHelper.deleteReceipt(old.receiptPath)
                }
                // Reverse both legs' balance
                val isOutgoing = old.note.contains("Transfer to", ignoreCase = true)
                if (isOutgoing) {
                    accountRepository.updateAccountBalance(old.accountId, old.amount)
                    accountRepository.updateAccountBalance(old.toAccountId!!, -old.amount)
                } else {
                    accountRepository.updateAccountBalance(old.toAccountId!!, old.amount)
                    accountRepository.updateAccountBalance(old.accountId, -old.amount)
                }
                // Delete sibling leg
                val siblings = transactionRepository.getTransferSiblings(old.accountId, old.toAccountId, old.amount, old.id)
                siblings.forEach { transactionRepository.deleteTransaction(it) }
                transactionRepository.deleteTransaction(old)
                // Create new transfer as double-entry
                val toId = new.toAccountId ?: return@launch
                val outNote = new.note.ifEmpty { "Transfer to Account" }
                val inNote = new.note.ifEmpty { "Transfer from Account" }
                transactionRepository.insertTransaction(new.copy(note = outNote))
                transactionRepository.insertTransaction(new.copy(
                    id = 0, accountId = toId, toAccountId = new.accountId, note = inNote
                ))
                accountRepository.updateAccountBalance(new.accountId, -new.amount)
                accountRepository.updateAccountBalance(toId, new.amount)
            } else {
                adjustBalance(old, reverse = true)
                updatePeerBalance(old, reverse = true)

                // Delete old receipt file if it changed
                if (old.receiptPath != null && old.receiptPath != new.receiptPath) {
                    FileHelper.deleteReceipt(old.receiptPath)
                }

                val updatedParent = if (new.id == 0L) old.copy(
                    accountId = new.accountId,
                    toAccountId = new.toAccountId,
                    type = new.type,
                    amount = new.amount,
                    categoryId = new.categoryId,
                    subCategoryId = new.subCategoryId,
                    goalId = new.goalId,
                    peerContactId = new.peerContactId,
                    tagIds = new.tagIds,
                    date = new.date,
                    note = new.note,
                    receiptPath = new.receiptPath,
                    isSplitParent = new.isSplitParent,
                    isTransfer = new.isTransfer,
                    investmentPlatform = new.investmentPlatform,
                    expectedReturnDate = new.expectedReturnDate
                ) else new

                transactionRepository.updateTransaction(updatedParent)

                if (updatedParent.isSplitParent && children != null) {
                    transactionRepository.deleteSplitChildren(old.id)
                    children.forEach { child ->
                        transactionRepository.insertTransaction(child.copy(isSplitChild = true, parentTransactionId = updatedParent.id))
                    }
                }

                adjustBalance(updatedParent, reverse = false)
                updatePeerBalance(updatedParent, reverse = false)
            }
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.isSplitParent) {
                val children = transactionRepository.getSplitChildren(transaction.id).first()
                children.forEach { FileHelper.deleteReceiptsForTransaction(it) }
                transactionRepository.deleteSplitChildren(transaction.id)
            }
            FileHelper.deleteReceiptsForTransaction(transaction)

            if (transaction.type == "transfer") {
                val isOutgoing = transaction.note.contains("Transfer to", ignoreCase = true)
                if (isOutgoing) {
                    accountRepository.updateAccountBalance(transaction.accountId, transaction.amount)
                    transaction.toAccountId?.let { accountRepository.updateAccountBalance(it, -transaction.amount) }
                } else {
                    transaction.toAccountId?.let { accountRepository.updateAccountBalance(it, transaction.amount) }
                    accountRepository.updateAccountBalance(transaction.accountId, -transaction.amount)
                }
                val siblings = transactionRepository.getTransferSiblings(
                    transaction.accountId, transaction.toAccountId ?: return@launch, transaction.amount, transaction.id
                )
                siblings.forEach { transactionRepository.deleteTransaction(it) }
            } else {
                adjustBalance(transaction, reverse = true)
                updatePeerBalance(transaction, reverse = true)
            }

            transactionRepository.deleteTransaction(transaction)
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
            "income", "receive", "borrow" -> accountRepository.updateAccountBalance(tx.accountId, sign * tx.amount)
            "expense", "savings", "lend" -> accountRepository.updateAccountBalance(tx.accountId, -sign * tx.amount)
        }
    }

    private suspend fun updatePeerBalance(tx: TransactionEntity, reverse: Boolean) {
        val peerId = tx.peerContactId ?: return
        val peer = peerContactRepository.getPeerByIdSync(peerId) ?: return
        val sign = if (reverse) -1.0 else 1.0

        val updatedPeer = when (tx.type) {
            "lend" -> peer.copy(
                totalGiven = peer.totalGiven + (sign * tx.amount),
                updatedAt = System.currentTimeMillis()
            )
            "receive" -> peer.copy(
                totalReceived = peer.totalReceived + (sign * tx.amount),
                updatedAt = System.currentTimeMillis()
            )
            "borrow" -> peer.copy(
                totalReceived = peer.totalReceived + (sign * tx.amount),
                updatedAt = System.currentTimeMillis()
            )
            else -> peer
        }

        if (updatedPeer !== peer) {
            peerContactRepository.updatePeer(updatedPeer)
        }
    }
}

private data class FilterState(
    val type: String = "All",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val tagId: Long? = null,
    val goalId: Long? = null,
    val peerId: Long? = null,
    val startDate: Long? = null,
    val endDate: Long? = null
)
