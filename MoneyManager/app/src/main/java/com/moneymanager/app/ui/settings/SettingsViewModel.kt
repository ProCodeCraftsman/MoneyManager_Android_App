package com.moneymanager.app.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.theme.AppTheme
import com.moneymanager.data.backup.BackupPreferences
import com.moneymanager.data.backup.BackupPassphraseStore
import com.moneymanager.data.backup.BackupScheduler
import com.moneymanager.data.backup.DriveAuthManager
import com.moneymanager.data.backup.DriveAuthState
import com.moneymanager.data.backup.DriveBackupManager
import com.moneymanager.data.backup.EncryptionHelper
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
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.debug.AppResetManager
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
    private val transactionRepository: TransactionRepository,
    private val appResetManager: AppResetManager,
    private val driveAuthManager: DriveAuthManager,
    private val driveBackupManager: DriveBackupManager,
    private val encryptionHelper: EncryptionHelper,
    private val passphraseStore: BackupPassphraseStore,
    private val backupPreferences: BackupPreferences,
    private val backupScheduler: BackupScheduler,
) : ViewModel() {

    private val importResult = MutableStateFlow<ImportResult?>(null)
    private val exportResult = MutableStateFlow<ExportResult?>(null)

    private val driveBackupOpStatus = MutableStateFlow<DriveOpStatus>(DriveOpStatus.Idle)
    private val driveRestoreOpStatus = MutableStateFlow<DriveOpStatus>(DriveOpStatus.Idle)
    private val foundDriveBackupFile = MutableStateFlow<com.moneymanager.data.backup.DriveFile?>(null)

    private val driveBackupUiState: StateFlow<DriveBackupUiState> = combine(
        driveAuthManager.authState,
        backupPreferences.autoBackupEnabled,
        backupPreferences.backupWeekly,
        backupPreferences.lastBackupTime,
        driveBackupOpStatus,
        driveRestoreOpStatus,
    ) { values ->
        val authState = values[0] as DriveAuthState
        val autoBackup = values[1] as Boolean
        val weekly = values[2] as Boolean
        val lastTime = values[3] as Long?
        val backupOp = values[4] as DriveOpStatus
        val restoreOp = values[5] as DriveOpStatus

        DriveBackupUiState(
            isSignedIn = authState is DriveAuthState.SignedIn,
            email = (authState as? DriveAuthState.SignedIn)?.email,
            displayName = (authState as? DriveAuthState.SignedIn)?.displayName,
            photoUrl = (authState as? DriveAuthState.SignedIn)?.photoUrl,
            isLoading = authState is DriveAuthState.Loading,
            error = (authState as? DriveAuthState.Error)?.message,
            pendingAuthIntent = (authState as? DriveAuthState.NeedsAuthorization)?.pendingIntent,
            autoBackupEnabled = autoBackup,
            backupWeekly = weekly,
            lastBackupTime = lastTime,
            backupOpStatus = backupOp,
            restoreOpStatus = restoreOp,
            foundBackupFile = foundDriveBackupFile.value,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DriveBackupUiState())

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
        preferencesManager.imageAttachmentsEnabled,
        authManager.authState,
        syncManager.getSyncState(),
        importResult,
        exportResult,
        driveBackupUiState,
    ) { values ->
        val selectedTheme = values[0] as AppTheme
        val darkMode = values[1] as Boolean
        val currency = values[2] as String
        val pinEnabled = values[3] as Boolean
        val pinHash = values[4] as String?
        val biometricEnabled = values[5] as Boolean
        val autoLockMinutes = values[6] as Int
        val lastSyncTime = values[7] as Long?
        val attachmentsEnabled = values[8] as Boolean
        val authState = values[9] as AuthState
        val syncState = values[10] as com.moneymanager.data.sync.SyncState
        val impResult = values[11] as ImportResult?
        val expResult = values[12] as ExportResult?
        val driveBackup = values[13] as DriveBackupUiState

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
            imageAttachmentsEnabled = attachmentsEnabled,
            driveBackup = driveBackup,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    // ---- Firebase settings ----

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(enabled)
            preferencesManager.setUserHasSetTheme()
        }
    }

    fun setSelectedTheme(theme: AppTheme) {
        viewModelScope.launch { preferencesManager.setSelectedTheme(theme) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { preferencesManager.setCurrency(currency) }
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
        viewModelScope.launch { preferencesManager.setBiometricEnabled(enabled) }
    }

    fun setAutoLockMinutes(minutes: Int) {
        viewModelScope.launch { preferencesManager.setAutoLockMinutes(minutes) }
    }

    fun setImageAttachmentsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.setImageAttachmentsEnabled(enabled) }
    }

    fun deleteAllAttachments() {
        viewModelScope.launch(Dispatchers.IO) {
            val transactions = transactionRepository.getTransactionsWithAttachments()
            transactions.forEach { FileHelper.deleteReceipt(it.receiptPath) }
            transactionRepository.clearAllReceiptPaths()
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

    fun signOut() { authManager.signOut() }

    fun triggerSync() {
        viewModelScope.launch { syncManager.sync() }
    }

    fun exportToCsv(type: ExportType, uri: Uri) {
        viewModelScope.launch {
            exportResult.value = exportRepository.exportToCsv(uri, type)
        }
    }

    fun importFromCsv(type: ExportType, uri: Uri) {
        viewModelScope.launch {
            importResult.value = exportRepository.importFromCsv(uri, type)
        }
    }

    fun exportToJson(uri: Uri) {
        viewModelScope.launch {
            exportResult.value = exportRepository.exportToJson(uri)
        }
    }

    fun importFromJson(uri: Uri) {
        viewModelScope.launch {
            importResult.value = exportRepository.importFromJson(uri)
        }
    }

    fun clearResults() {
        importResult.value = null
        exportResult.value = null
    }

    fun hardResetApp(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            appResetManager.hardReset()
            launch(Dispatchers.Main) { onComplete() }
        }
    }

    // ---- Google Drive Backup ----

    fun signInWithDrive(activity: Activity) {
        viewModelScope.launch { driveAuthManager.signIn(activity) }
    }

    fun handleDriveAuthorizationData(data: Intent?) {
        viewModelScope.launch { driveAuthManager.handleAuthorizationData(data) }
    }

    /** Called by the UI after it has launched the NeedsAuthorization pending intent. */
    fun onDriveAuthorizationConsumed() {
        // state will update via authState flow once the authorization result arrives
    }

    fun driveSignOut() {
        driveAuthManager.signOut()
        backupScheduler.cancelBackup()
    }

    fun clearDriveError() {
        driveAuthManager.clearError()
        driveBackupOpStatus.value = DriveOpStatus.Idle
        driveRestoreOpStatus.value = DriveOpStatus.Idle
    }

    fun clearDriveOp() {
        driveBackupOpStatus.value = DriveOpStatus.Idle
        driveRestoreOpStatus.value = DriveOpStatus.Idle
    }

    fun backupToDrive(passphrase: String) {
        val accessToken = driveAuthManager.accessToken ?: return
        viewModelScope.launch {
            driveBackupOpStatus.value = DriveOpStatus.InProgress
            try {
                val jsonBytes = exportRepository.exportToJsonBytes()
                val encrypted = encryptionHelper.encrypt(jsonBytes, passphrase)
                driveBackupManager.uploadBackup(encrypted, accessToken).fold(
                    onSuccess = { fileId ->
                        driveBackupManager.deleteOldBackups(fileId, accessToken)
                        backupPreferences.setLastBackupTime(System.currentTimeMillis())
                        passphraseStore.savePassphrase(passphrase)
                        driveBackupOpStatus.value = DriveOpStatus.Success("Backup uploaded successfully")
                    },
                    onFailure = { e ->
                        driveBackupOpStatus.value = DriveOpStatus.Error(e.message ?: "Upload failed")
                    }
                )
            } catch (e: Exception) {
                driveBackupOpStatus.value = DriveOpStatus.Error(e.message ?: "Backup failed")
            }
        }
    }

    fun checkForDriveBackup() {
        val accessToken = driveAuthManager.accessToken ?: return
        viewModelScope.launch {
            driveRestoreOpStatus.value = DriveOpStatus.InProgress
            foundDriveBackupFile.value = null
            driveBackupManager.findBackup(accessToken).fold(
                onSuccess = { file ->
                    foundDriveBackupFile.value = file
                    driveRestoreOpStatus.value = if (file == null)
                        DriveOpStatus.NoBackupFound
                    else
                        DriveOpStatus.Idle
                },
                onFailure = { e ->
                    driveRestoreOpStatus.value = DriveOpStatus.Error(e.message ?: "Failed to check for backup")
                }
            )
        }
    }

    fun restoreFromDrive(passphrase: String) {
        val accessToken = driveAuthManager.accessToken ?: return
        val fileId = foundDriveBackupFile.value?.id ?: return
        viewModelScope.launch {
            driveRestoreOpStatus.value = DriveOpStatus.InProgress
            try {
                driveBackupManager.downloadBackup(fileId, accessToken).fold(
                    onSuccess = { encryptedData ->
                        val jsonBytes = encryptionHelper.decrypt(encryptedData, passphrase)
                        val result = exportRepository.importFromJsonBytes(jsonBytes)
                        foundDriveBackupFile.value = null
                        driveRestoreOpStatus.value = DriveOpStatus.Success(
                            "Restored: ${result.accountsImported} accounts, " +
                                "${result.transactionsImported} transactions, " +
                                "${result.categoriesImported} categories"
                        )
                    },
                    onFailure = { e ->
                        driveRestoreOpStatus.value = DriveOpStatus.Error(e.message ?: "Download failed")
                    }
                )
            } catch (e: javax.crypto.BadPaddingException) {
                driveRestoreOpStatus.value = DriveOpStatus.Error("Wrong passphrase — decryption failed")
            } catch (e: Exception) {
                driveRestoreOpStatus.value = DriveOpStatus.Error(e.message ?: "Restore failed")
            }
        }
    }

    fun clearFoundDriveBackup() {
        foundDriveBackupFile.value = null
        driveRestoreOpStatus.value = DriveOpStatus.Idle
    }

    fun setDriveAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            backupPreferences.setAutoBackupEnabled(enabled)
            if (enabled) {
                backupScheduler.scheduleBackup(
                    isWeekly = backupPreferences.backupWeekly.stateIn(
                        viewModelScope, SharingStarted.Eagerly, true
                    ).value
                )
            } else {
                backupScheduler.cancelBackup()
            }
        }
    }

    fun setDriveBackupFrequency(isWeekly: Boolean) {
        viewModelScope.launch {
            backupPreferences.setBackupWeekly(isWeekly)
            if (uiState.value.driveBackup.autoBackupEnabled) {
                backupScheduler.scheduleBackup(isWeekly)
            }
        }
    }
}
