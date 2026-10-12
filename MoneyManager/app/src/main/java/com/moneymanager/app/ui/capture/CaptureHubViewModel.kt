package com.moneymanager.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.repository.AiAvailabilityRepository
import com.moneymanager.domain.capture.CaptureStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CaptureHubState(
    val pendingCount: Int = 0,
    val aiAvailable: Boolean = false,
    val imageAttachments: Boolean = false,
)

/** What the "Add from…" hub needs to decide which entries to offer. */
@HiltViewModel
class CaptureHubViewModel @Inject constructor(
    store: CaptureStore,
    aiAvailability: AiAvailabilityRepository,
    preferences: PreferencesManager,
) : ViewModel() {
    val state: StateFlow<CaptureHubState> = combine(
        store.observePendingCount(),
        aiAvailability.isAiAvailable,
        preferences.imageAttachmentsEnabled,
    ) { pending, ai, images ->
        CaptureHubState(pendingCount = pending, aiAvailable = ai, imageAttachments = images)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CaptureHubState())
}
