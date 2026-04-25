package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.util.FileHelper
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
    val filterGoalId: Long? = null,
    val filterPeerId: Long? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val allTags: List<TagEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val allAccounts: List<AccountEntity> = emptyList(),
    val allGoals: List<GoalEntity> = emptyList(),
    val allPeers: List<PeerContact> = emptyList(),
    val currency: String = "INR",
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

    val uiState: StateFlow<TransactionsUiState> = combine(
        transactionRepository.getAllTransactions(),
        _searchQuery,
        _filters,
        allTags,
        allCategories,
        allAccounts,
        allGoals,
        allPeers,
        preferencesManager.currency
    ) { array ->
        val txs = array[0] as List<TransactionEntity>
        val q = array[1] as String
        val f = array[2] as FilterState
        val t = array[3] as List<TagEntity>
        val c = array[4] as List<CategoryEntity>
        val a = array[5] as List<AccountEntity>
        val g = array[6] as List<GoalEntity>
        val p = array[7] as List<PeerContact>
        val curr = array[8] as String

        val filtered = txs
            .filter { !it.isSplitChild } // hide split children from main list
            .filter { tx ->
                val noteMatches = q.isEmpty() ||
                    tx.note.contains(q, ignoreCase = true) ||
                    tx.amount.toString().contains(q)
                val typeMatches = f.type.isEmpty() || tx.type == f.type
                val accountMatches = f.accountId == null || tx.accountId == f.accountId || tx.toAccountId == f.accountId
                val categoryMatches = f.categoryId == null || tx.categoryId == f.categoryId
                val tagMatches = f.tagId == null ||
                    (tx.tagIds.isNotEmpty() && tx.tagIds.split(",").contains(f.tagId.toString()))
                val goalMatches = f.goalId == null || tx.goalId == f.goalId
                val peerMatches = f.peerId == null || tx.peerContactId == f.peerId
                val startDateMatches = f.startDate == null || tx.date >= f.startDate
                val endDateMatches = f.endDate == null || tx.date <= f.endDate
                noteMatches && typeMatches && accountMatches && categoryMatches && tagMatches && goalMatches && peerMatches && startDateMatches && endDateMatches
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
            filterGoalId = f.goalId,
            filterPeerId = f.peerId,
            filterStartDate = f.startDate,
            filterEndDate = f.endDate,
            allTags = t,
            allCategories = c,
            allAccounts = a,
            allGoals = g,
            allPeers = p,
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
    fun setGoalFilter(id: Long?) { _filters.value = _filters.value.copy(goalId = id) }
    fun setPeerFilter(id: Long?) { _filters.value = _filters.value.copy(peerId = id) }
    fun setDateRangeFilter(start: Long?, end: Long?) { _filters.value = _filters.value.copy(startDate = start, endDate = end) }
    fun clearAllFilters() { _filters.value = FilterState("", null, null, null, null, null, null, null) }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.insertTransaction(transaction)
            adjustBalance(transaction, reverse = false)
            updatePeerBalance(transaction, reverse = false)
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

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            if (transaction.isSplitParent) {
                val children = transactionRepository.getSplitChildren(transaction.id).first()
                children.forEach { FileHelper.deleteReceiptsForTransaction(it) }
                transactionRepository.deleteSplitChildren(transaction.id)
            }
            FileHelper.deleteReceiptsForTransaction(transaction)
            adjustBalance(transaction, reverse = true)
            updatePeerBalance(transaction, reverse = true)
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
                transactionRepository.getSplitChildren(transaction.id).first().forEach { child ->
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
            updatePeerBalance(copy, reverse = false)
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
            "income", "receive" -> accountRepository.updateAccountBalance(tx.accountId, sign * tx.amount)
            "expense", "savings", "lend" -> accountRepository.updateAccountBalance(tx.accountId, -sign * tx.amount)
            "transfer" -> {
                if (tx.isTransfer) {
                    val isOutgoing = tx.note.contains("Transfer to", ignoreCase = true)
                    val factor = if (isOutgoing) -1.0 else 1.0
                    accountRepository.updateAccountBalance(tx.accountId, sign * factor * tx.amount)
                }
            }
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
