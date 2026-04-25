package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.security.SecurityManager
import com.moneymanager.data.sync.AuthManager
import com.moneymanager.data.sync.AuthState
import com.moneymanager.data.sync.FirebaseSyncManager
import com.moneymanager.data.sync.SyncStatus
import com.moneymanager.data.repository.ExportRepository
import com.moneymanager.data.repository.ExportType
import com.moneymanager.data.repository.ExportResult
import com.moneymanager.data.repository.ImportResult
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val darkMode: Boolean = false,
    val currency: String = "INR",
    val pinEnabled: Boolean = false,
    val pinSetupRequired: Boolean = false,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 5,
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long? = null,
    val isSyncing: Boolean = false,
    val storageUsedKb: Long = 0,
    val importResult: ImportResult? = null,
    val exportResult: ExportResult? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val authManager: AuthManager,
    private val syncManager: FirebaseSyncManager,
    private val exportRepository: ExportRepository,
    private val securityManager: SecurityManager,
) : ViewModel() {

    private val importResult = MutableStateFlow<ImportResult?>(null)
    private val exportResult = MutableStateFlow<ExportResult?>(null)
    private val _showPinSetupDialog = MutableStateFlow(false)
    val showPinSetupDialog: StateFlow<Boolean> = _showPinSetupDialog.asStateFlow()

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.darkMode,
        preferencesManager.currency,
        preferencesManager.pinEnabled,
        preferencesManager.pinHash,
        preferencesManager.biometricEnabled,
        preferencesManager.autoLockMinutes,
        preferencesManager.lastSyncTime,
        authManager.authState,
        syncManager.getSyncState(),
        importResult,
        exportResult,
    ) { values ->
val darkMode = values[0] as Boolean
        val currency = values[1] as String
        val pinEnabled = values[2] as Boolean
        val pinHash = values[3] as String?
        val biometricEnabled = values[4] as Boolean
        val autoLockMinutes = values[5] as Int
        val lastSyncTime = values[6] as Long?
        val authState = values[7] as AuthState
        val syncState = values[8] as com.moneymanager.data.sync.SyncState
        val impResult = values[9] as ImportResult?
        val expResult = values[10] as ExportResult?

        val isSignedIn = authState is AuthState.SignedIn
        val user = (authState as? AuthState.SignedIn)?.user
        val pinSetupRequired = pinEnabled && pinHash == null

        SettingsUiState(
            darkMode = darkMode,
            currency = currency,
            pinEnabled = pinEnabled,
            pinSetupRequired = pinSetupRequired,
            biometricEnabled = biometricEnabled,
            autoLockMinutes = autoLockMinutes,
            isSignedIn = isSignedIn,
            userEmail = user?.email,
            userName = user?.displayName,
            syncStatus = syncState.status,
            lastSyncTime = lastSyncTime,
            isSyncing = syncState.status is SyncStatus.Syncing,
            storageUsedKb = exportRepository.getStorageUsedKb(),
            importResult = impResult,
            exportResult = expResult,
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
            if (enabled) {
                val pinHash = preferencesManager.pinHash.first()
                if (pinHash == null) {
                    _showPinSetupDialog.value = true
                } else {
                    preferencesManager.setPinEnabled(true)
                }
            } else {
                preferencesManager.setPinEnabled(false)
                preferencesManager.setPinHash(null)
                preferencesManager.setPinSalt(null)
            }
        }
    }

    fun onPinSetupComplete(pin: String) {
        viewModelScope.launch {
            val (hash, salt) = securityManager.hashPin(pin)
            preferencesManager.setPinHash(hash)
            preferencesManager.setPinSalt(salt)
            preferencesManager.setPinEnabled(true)
            _showPinSetupDialog.value = false
        }
    }

    fun dismissPinSetupDialog() {
        _showPinSetupDialog.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val pinHash = preferencesManager.pinHash.first()
            if (enabled && pinHash == null) {
                _showPinSetupDialog.value = true
            } else {
                preferencesManager.setBiometricEnabled(enabled)
            }
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

    fun exportToCsv(type: ExportType, uri: Uri) {
        viewModelScope.launch {
            val result = exportRepository.exportToCsv(uri, type)
            exportResult.value = result
        }
    }

    fun importFromCsv(type: ExportType, uri: Uri) {
        viewModelScope.launch {
            val result = exportRepository.importFromCsv(uri, type)
            importResult.value = result
        }
    }

    fun exportToJson(uri: Uri) {
        viewModelScope.launch {
            val result = exportRepository.exportToJson(uri)
            exportResult.value = result
        }
    }

    fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            val result = exportRepository.importFromJson(uri)
            importResult.value = result
        }
    }

    fun clearResults() {
        importResult.value = null
        exportResult.value = null
    }
}