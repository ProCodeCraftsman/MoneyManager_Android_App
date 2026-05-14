package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.GoalRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val peers: List<PeerContact> = emptyList(),
    val currency: String = "INR",
    val categoryUsageCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager,
) : AndroidViewModel(application) {

    val uiState: StateFlow<AddTransactionUiState> = combine(
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories(),
        categoryRepository.getAllTags(),
        goalRepository.getAllGoals(),
        peerContactRepository.getAllPeers(),
        preferencesManager.currency,
        transactionRepository.getAllTransactions()
    ) { array ->
        @Suppress("UNCHECKED_CAST")
        val accounts = array[0] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = array[1] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val tags = array[2] as List<TagEntity>
        @Suppress("UNCHECKED_CAST")
        val goals = array[3] as List<GoalEntity>
        @Suppress("UNCHECKED_CAST")
        val peers = array[4] as List<PeerContact>
        val currency = array[5] as String
        @Suppress("UNCHECKED_CAST")
        val transactions = array[6] as List<TransactionEntity>

        AddTransactionUiState(
            accounts = accounts,
            categories = categories,
            tags = tags,
            goals = goals,
            peers = peers,
            currency = currency,
            categoryUsageCounts = transactions
                .filter { it.categoryId != null && !it.isSplitChild }
                .groupBy { it.categoryId!! }
                .mapValues { it.value.size },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddTransactionUiState(),
    )

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
