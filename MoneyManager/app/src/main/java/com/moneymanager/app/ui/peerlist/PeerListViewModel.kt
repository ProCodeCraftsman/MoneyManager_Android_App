package com.moneymanager.app.ui.peerlist

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.util.ContactPickerHelper
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.PeerContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class PeerEvent {
    data class Success(val message: String) : PeerEvent()
    data class Error(val message: String) : PeerEvent()
}

@HiltViewModel
class PeerListViewModel @Inject constructor(
    application: Application,
    private val peerContactRepository: PeerContactRepository,
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

    fun onContactPicked(contactUri: Uri?, contentResolver: ContentResolver) {
        if (contactUri == null) return
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ContactPickerHelper.queryContactData(contentResolver, contactUri)
            }
            if (result == null) {
                _events.emit(PeerEvent.Error("Could not read contact"))
                return@launch
            }
            val (name, lookupKey, phone) = result
            if (lookupKey.isBlank()) {
                _events.emit(PeerEvent.Error("Invalid contact"))
                return@launch
            }
            try {
                val existing = peerContactRepository.getPeerByLookupKey(lookupKey)
                if (existing != null) {
                    _events.emit(PeerEvent.Success("Contact already exists"))
                } else {
                    peerContactRepository.insertPeer(
                        PeerContact(
                            displayName = name,
                            lookupKey = lookupKey,
                            phoneNumber = phone
                        )
                    )
                    _events.emit(PeerEvent.Success("Contact added as peer"))
                }
            } catch (e: Exception) {
                _events.emit(PeerEvent.Error("Failed to add contact: ${e.message}"))
            }
        }
    }

    fun savePeer(peer: PeerContact) {
        viewModelScope.launch {
            try {
                peerContactRepository.updatePeer(peer)
                _events.emit(PeerEvent.Success("Peer updated"))
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
