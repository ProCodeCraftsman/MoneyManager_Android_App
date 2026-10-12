package com.moneymanager.app.ui.addtransaction

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.dao.AccountUsage
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.repository.DraftEditStatsRepository
import com.moneymanager.domain.ai.DraftLearning
import com.moneymanager.domain.account.observeAccountUsage
import com.moneymanager.domain.capture.CaptureReview
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.GoalRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager,
    private val draftLearning: DraftLearning,
    private val emiDao: com.moneymanager.data.dao.EmiDao,
    private val captureReview: CaptureReview,
    private val draftEditStats: DraftEditStatsRepository,
) : AndroidViewModel(application) {

    val uiState: StateFlow<AddTransactionUiState> = combine(
        accountRepository.getActiveAccounts(),
        categoryRepository.getAllCategories(),
        categoryRepository.getAllTags(),
        goalRepository.getAllGoals(),
        peerContactRepository.getAllPeers(),
        preferencesManager.currency,
        transactionRepository.getAllTransactions(),
        preferencesManager.imageAttachmentsEnabled,
        preferencesManager.defaultAccounts,
        transactionRepository.observeAccountUsage()
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
        val attachmentsEnabled = array[7] as Boolean
        @Suppress("UNCHECKED_CAST")
        val defaultAccounts = array[8] as Map<String, Long>
        @Suppress("UNCHECKED_CAST")
        val accountUsage = array[9] as List<AccountUsage>

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
            imageAttachmentsEnabled = attachmentsEnabled,
            defaultAccounts = defaultAccounts,
            accountUsage = accountUsage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AddTransactionUiState(),
    )

    /** [captureId] is the inbox row this save came from; saving closes that row so it leaves the inbox (AI-056). */
    fun addTransaction(transaction: TransactionEntity, captureId: Long? = null) {
        viewModelScope.launch {
            if (transaction.type == "transfer") {
                val toId = transaction.toAccountId ?: return@launch
                val id = transactionRepository.insertTransaction(transaction)
                captureId?.let { captureReview.markSaved(it, id) }
                accountRepository.updateAccountBalance(transaction.accountId, -transaction.amount)
                accountRepository.updateAccountBalance(toId, transaction.amount)
            } else {
                val id = transactionRepository.insertTransaction(transaction)
                captureId?.let { captureReview.markSaved(it, id) }
                adjustBalance(transaction, reverse = false)
                updatePeerBalance(transaction, reverse = false)
            }
        }
    }

    /** Edit tracking (AI-014): which fields the user changed on a captured draft before saving. Names only. */
    fun recordDraftSave(sourceType: String, changedFields: List<String>) {
        viewModelScope.launch(Dispatchers.IO) { draftEditStats.record(sourceType, changedFields) }
    }

    fun addEmiExpense(
        baseTx: TransactionEntity,
        tenureMonths: Int,
        annualInterestRate: Double,
        isNoCost: Boolean,
        processingFee: Double
    ) {
        viewModelScope.launch {
            val emi = com.moneymanager.domain.transaction.EmiTransactionFactory.buildEmi(
                baseTx, tenureMonths, annualInterestRate, isNoCost, processingFee
            )
            val emiId = emiDao.insertEmi(emi)

            com.moneymanager.domain.transaction.EmiTransactionFactory.buildFeeTransaction(baseTx, processingFee, emiId)?.let { feeTx ->
                transactionRepository.insertTransaction(feeTx)
                accountRepository.updateAccountBalance(baseTx.accountId, -processingFee)
            }

            com.moneymanager.domain.transaction.EmiTransactionFactory.buildInstallments(baseTx, emi, emiId).forEach { installmentTx ->
                transactionRepository.insertTransaction(installmentTx)
                if (installmentTx.emiInstallmentNumber == 1) {
                    accountRepository.updateAccountBalance(baseTx.accountId, -installmentTx.amount)
                }
            }
        }
    }

    fun addSplitTransaction(parent: TransactionEntity, children: List<TransactionEntity>, captureId: Long? = null) {
        viewModelScope.launch {
            val parentId = transactionRepository.insertTransaction(parent.copy(isSplitParent = true))
            captureId?.let { captureReview.markSaved(it, parentId) }
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
            "income", "borrow" -> accountRepository.updateAccountBalance(tx.accountId, sign * tx.amount)
            "expense", "lend" -> accountRepository.updateAccountBalance(tx.accountId, -sign * tx.amount)
            "savings" -> {
                accountRepository.updateAccountBalance(tx.accountId, -sign * tx.amount)
                tx.toAccountId?.let { accountRepository.updateAccountBalance(it, sign * tx.amount) }
            }
        }
    }

    // Called from AddTransactionScreen after saving an AI-drafted transaction.
    // Records the user's actual category choice so future same-merchant drafts skip AI.
    fun recordMerchantCategory(merchantHint: String, categoryId: Long, typeId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryName = uiState.value.categories
                .firstOrNull { it.id == categoryId }?.name ?: ""
            draftLearning.learnMerchant(merchantHint, categoryId, categoryName, typeId)
        }
    }

    // Called after saving a draft that came from an SMS: the account the user kept becomes the link for that
    // SMS source, so the next message from it needs no account edit. A card repayment's SMS describes the card,
    // which is the destination of the transfer.
    fun recordSmsAccountLink(linkKey: String, tx: TransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) { draftLearning.learnFromSave(linkKey, tx) }
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
