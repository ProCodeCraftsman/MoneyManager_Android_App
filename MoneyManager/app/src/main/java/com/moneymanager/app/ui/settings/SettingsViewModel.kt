package com.moneymanager.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.theme.AppTheme
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.repository.ExportRepository
import com.moneymanager.data.repository.ExportType
import com.moneymanager.data.repository.ExportResult
import com.moneymanager.data.repository.ImportResult
import com.moneymanager.data.security.SecurityManager
import com.moneymanager.data.sync.AuthManager
import com.moneymanager.data.sync.AuthState
import com.moneymanager.data.sync.FirebaseSyncManager
import com.moneymanager.data.sync.SyncStatus
import android.net.Uri
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    @OptIn(kotlinx.coroutines.FlowPreview::class)
    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesManager.selectedTheme,
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
        val selectedTheme = values[0] as AppTheme
        val darkMode = values[1] as Boolean
        val currency = values[2] as String
        val pinEnabled = values[3] as Boolean
        val pinHash = values[4] as String?
        val biometricEnabled = values[5] as Boolean
        val autoLockMinutes = values[6] as Int
        val lastSyncTime = values[7] as Long?
        val authState = values[8] as AuthState
        val syncState = values[9] as com.moneymanager.data.sync.SyncState
        val impResult = values[10] as ImportResult?
        val expResult = values[11] as ExportResult?

        val isSignedIn = authState is AuthState.SignedIn
        val user = (authState as? AuthState.SignedIn)?.user
        SettingsUiState(
            selectedTheme = selectedTheme,
            darkMode = darkMode,
            currency = currency,
            pinEnabled = pinEnabled,
            pinHash = pinHash,
            biometricEnabled = biometricEnabled,
            autoLockMinutes = autoLockMinutes,
            isSignedIn = isSignedIn,
            userEmail = user?.email,
            userName = user?.displayName,
            userPhone = user?.phoneNumber,
            syncStatus = syncState.status,
            lastSyncTime = lastSyncTime,
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
            preferencesManager.setUserHasSetTheme()
        }
    }

    fun setSelectedTheme(theme: AppTheme) {
        viewModelScope.launch {
            preferencesManager.setSelectedTheme(theme)
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
                preferencesManager.setPinEnabled(true)
            } else {
                preferencesManager.setPinEnabled(false)
                preferencesManager.setPinHash(null)
                preferencesManager.setPinSalt(null)
                preferencesManager.setBiometricEnabled(false)
            }
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

    fun createAndEnablePin(pin: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val (hash, salt) = securityManager.hashPin(pin)
            preferencesManager.setPinHash(hash)
            preferencesManager.setPinSalt(salt)
            preferencesManager.setPinEnabled(true)
            preferencesManager.resetWrongAttempts()
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
