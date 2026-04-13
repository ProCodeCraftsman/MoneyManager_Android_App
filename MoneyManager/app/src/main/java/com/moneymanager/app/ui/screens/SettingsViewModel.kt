package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.sync.AuthManager
import com.moneymanager.data.sync.AuthState
import com.moneymanager.data.sync.FirebaseSyncManager
import com.moneymanager.data.sync.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val currency: String = "USD",
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 5,
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager,
    private val syncManager: FirebaseSyncManager,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.darkMode,
        preferencesManager.currency,
        preferencesManager.pinEnabled,
        preferencesManager.biometricEnabled,
        preferencesManager.autoLockMinutes,
        preferencesManager.lastSyncTime,
        authManager.authState,
        syncManager.getSyncState(),
    ) { values ->
        val darkMode = values[0] as Boolean
        val currency = values[1] as String
        val pinEnabled = values[2] as Boolean
        val biometricEnabled = values[3] as Boolean
        val autoLockMinutes = values[4] as Int
        val lastSyncTime = values[5] as Long?
        val authState = values[6] as AuthState
        val syncState = values[7] as com.moneymanager.data.sync.SyncState
        
        val isSignedIn = authState is AuthState.SignedIn
        val user = (authState as? AuthState.SignedIn)?.user
        
        SettingsUiState(
            darkMode = darkMode,
            currency = currency,
            pinEnabled = pinEnabled,
            biometricEnabled = biometricEnabled,
            autoLockMinutes = autoLockMinutes,
            isSignedIn = isSignedIn,
            userEmail = user?.email,
            userName = user?.displayName,
            syncStatus = syncState.status,
            lastSyncTime = lastSyncTime,
            isSyncing = syncState.status is SyncStatus.Syncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
        }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            preferencesManager.setCurrency(currency)
        }
    }

    fun setPinEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setPinEnabled(enabled)
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setBiometricEnabled(enabled)
        }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setAutoLockMinutes(minutes)
        }
    }

    fun signInWithGoogle(idToken: String) {
        authManager.signInWithGoogle(idToken) { result ->
            if (result.isSuccess) {
                viewModelScope.launch { syncManager.sync() }
            }
        }
    }

    fun signOut() {
        authManager.signOut()
    }

    fun triggerSync() {
        viewModelScope.launch {
            syncManager.sync()
        }
    }
}