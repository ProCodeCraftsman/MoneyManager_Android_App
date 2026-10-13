package com.moneymanager.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.capture.CaptureBannerPolicy
import com.moneymanager.domain.capture.CaptureConnectPolicy
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.capture.ScanInboxUseCase
import com.moneymanager.domain.capture.SmsInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CaptureBannerState(
    val pending: Int = 0,
    val visible: Boolean = false,
)

/**
 * Scans the inbox on app open once permission is granted (AI-058, AI-079) and drives the home banner that counts the
 * drafts waiting for review. Builds without an inbox (play) never scan, never show the banner and never show the card.
 */
@HiltViewModel
class CaptureBannerViewModel @Inject constructor(
    private val inbox: SmsInbox,
    private val scanInbox: ScanInboxUseCase,
    private val store: CaptureStore,
    private val preferences: PreferencesManager,
) : ViewModel() {

    val state: StateFlow<CaptureBannerState> = combine(
        store.observePendingCount(),
        preferences.captureBannerDismissedDay,
        preferences.captureBannerHidden,
    ) { pending, dismissedDay, hidden ->
        val visible = inbox.isAvailable &&
            CaptureBannerPolicy.isVisible(pending, dismissedDay, hidden, LocalDate.now().toEpochDay())
        CaptureBannerState(pending = pending, visible = visible)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureBannerState())

    private val permitted = MutableStateFlow(inbox.hasPermission())

    /** The first-run connect card (AI-079). Shown until the inbox permission is granted. */
    val showConnectCard: StateFlow<Boolean> = permitted
        .map { CaptureConnectPolicy.showConnectCard(inbox.isAvailable, it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CaptureConnectPolicy.showConnectCard(inbox.isAvailable, permitted.value))

    init {
        if (CaptureConnectPolicy.scanOnOpen(inbox.isAvailable, permitted.value)) scanQuietly()
    }

    /** Called when the connect card's permission prompt returns. A grant scans straight away. */
    fun onSmsPermissionResult(granted: Boolean) {
        if (granted) connect()
    }

    /** Called when the home screen comes back, so a grant or revoke made in system settings is picked up. */
    fun refreshPermission() {
        val now = inbox.hasPermission()
        if (now && !permitted.value) connect() else permitted.value = now
    }

    private fun connect() {
        permitted.value = true
        scanQuietly()
    }

    private fun scanQuietly() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                scanInbox()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The banner still shows whatever is already waiting.
            }
        }
    }

    /** The ✕ button: hidden until local midnight. */
    fun dismissForToday() {
        viewModelScope.launch { preferences.setCaptureBannerDismissedDay(LocalDate.now().toEpochDay()) }
    }

    /** "Don't show this" in the banner menu. Reversible only from the settings screen. */
    fun dontShowAgain() {
        viewModelScope.launch { preferences.setCaptureBannerHidden(true) }
    }
}
