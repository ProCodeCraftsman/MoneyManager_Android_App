package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BorrowLendUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val peers: List<PeerContact> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedPeerId: Long? = null,
    val peerName: String = "",
    val amount: Double = 0.0,
    val note: String = "",
    val date: Long = System.currentTimeMillis(),
    val expectedReturnDate: Long? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val currencyCode: String = "INR"
)

@HiltViewModel
class BorrowLendViewModel @Inject constructor(
    application: Application,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(BorrowLendUiState())
    val uiState: StateFlow<BorrowLendUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                accountRepository.getAllAccounts(),
                peerContactRepository.getAllPeers(),
                preferencesManager.currency
            ) { accounts, peers, currency ->
                Triple(accounts, peers, currency)
            }.collect { (accounts, peers, currency) ->
                _uiState.update { it.copy(
                    accounts = accounts,
                    peers = peers,
                    currencyCode = currency
                ) }
            }
        }
    }

    fun selectAccount(accountId: Long) {
        _uiState.update { it.copy(selectedAccountId = accountId) }
    }

    fun selectPeer(peer: PeerContact) {
        _uiState.update { it.copy(selectedPeerId = peer.id, peerName = peer.displayName) }
    }

    fun setPeerName(name: String) {
        _uiState.update { it.copy(peerName = name, selectedPeerId = null) }
    }

    fun setAmount(amount: Double) {
        _uiState.update { it.copy(amount = amount) }
    }

    fun setNote(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun setDate(date: Long) {
        _uiState.update { it.copy(date = date) }
    }

    fun setExpectedReturnDate(date: Long?) {
        _uiState.update { it.copy(expectedReturnDate = date) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun saveTransaction(type: TransactionType, onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.selectedAccountId == null) {
            _uiState.update { it.copy(error = "Please select an account") }
            return
        }
        if (state.selectedPeerId == null) {
            _uiState.update { it.copy(error = "Please select a person") }
            return
        }
        if (state.amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val transaction = TransactionEntity(
                    accountId = state.selectedAccountId,
                    type = if (type == TransactionType.LEND) "lend" else "receive",
                    amount = state.amount,
                    peerContactId = state.selectedPeerId,
                    date = state.date,
                    note = state.note,
                    expectedReturnDate = state.expectedReturnDate,
                    createdAt = System.currentTimeMillis()
                )

                transactionRepository.insertTransaction(transaction)
                adjustBalance(transaction)
                updatePeerBalance(transaction)
                
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to save transaction", isSaving = false) }
            }
        }
    }

    private suspend fun adjustBalance(transaction: TransactionEntity) {
        val account = accountRepository.getAccountById(transaction.accountId) ?: return
        val newBalance = if (transaction.type == "lend") {
            account.balance - transaction.amount
        } else {
            account.balance + transaction.amount
        }
        accountRepository.updateAccount(account.copy(balance = newBalance))
    }

    private suspend fun updatePeerBalance(transaction: TransactionEntity) {
        val peerId = transaction.peerContactId ?: return
        val peer = peerContactRepository.getPeerByIdSync(peerId) ?: return
        
        val updatedPeer = if (transaction.type == "lend") {
            peer.copy(totalGiven = peer.totalGiven + transaction.amount)
        } else {
            peer.copy(totalReceived = peer.totalReceived + transaction.amount)
        }
        peerContactRepository.updatePeer(updatedPeer)
    }
}
