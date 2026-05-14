package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

data class PeerListUiState(
    val peers: List<PeerContact> = emptyList(),
    val totalLent: Double = 0.0,
    val totalReceived: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
)

sealed class PeerEvent {
    data class Success(val message: String) : PeerEvent()
    data class Error(val message: String) : PeerEvent()
}

@HiltViewModel
class PeerListViewModel @Inject constructor(
    application: Application,
    private val peerContactRepository: PeerContactRepository,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager,
) : AndroidViewModel(application) {

    val uiState: StateFlow<PeerListUiState> = combine(
        peerContactRepository.getAllPeers(),
        peerContactRepository.getTotalLent(),
        peerContactRepository.getTotalReceived(),
        preferencesManager.currency
    ) { peers, totalLent, totalReceived, currency ->
        PeerListUiState(
            peers = peers,
            totalLent = totalLent ?: 0.0,
            totalReceived = totalReceived ?: 0.0,
            outstandingBalance = (totalLent ?: 0.0) - (totalReceived ?: 0.0),
            currencyCode = currency,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PeerListUiState()
    )

    private val _events = MutableSharedFlow<PeerEvent>()
    val events = _events.asSharedFlow()

    fun savePeer(peer: PeerContact) {
        viewModelScope.launch {
            try {
                if (peer.id == 0L) {
                    peerContactRepository.insertPeer(peer)
                    _events.emit(PeerEvent.Success("Peer added"))
                } else {
                    peerContactRepository.updatePeer(peer)
                    _events.emit(PeerEvent.Success("Peer updated"))
                }
            } catch (e: Exception) {
                _events.emit(PeerEvent.Error("Failed to save: ${e.message}"))
            }
        }
    }

    fun deletePeer(peer: PeerContact) {
        viewModelScope.launch {
            try {
                peerContactRepository.deletePeer(peer)
                _events.emit(PeerEvent.Success("Peer deleted"))
            } catch (e: Exception) {
                _events.emit(PeerEvent.Error("Failed to delete: ${e.message}"))
            }
        }
    }
}