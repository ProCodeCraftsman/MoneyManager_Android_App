package com.moneymanager.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.capture.CaptureBannerPolicy
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.capture.ScanInboxUseCase
import com.moneymanager.domain.capture.SmsInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class CaptureBannerState(
    val pending: Int = 0,
    val visible: Boolean = false,
)

/**
 * Scans the inbox on app open (AI-058) and drives the home banner that counts the drafts waiting for review.
 * Builds without an inbox (play) never scan and never show the banner.
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

    init {
        if (inbox.isAvailable) {
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
