package com.moneymanager.app.ui.settings

import com.moneymanager.app.ui.theme.AppTheme
import com.moneymanager.data.backup.DriveFile
import com.moneymanager.data.repository.ExportResult
import com.moneymanager.data.repository.ImportResult
import com.moneymanager.data.sync.SyncStatus

data class SettingsUiState(
    val selectedTheme: AppTheme = AppTheme.CALM_GREEN,
    val darkMode: Boolean = false,
    val currency: String = "INR",
    val pinEnabled: Boolean = false,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
    val autoLockMinutes: Int = 0,
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val userName: String? = null,
    val userPhone: String? = null,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long? = null,
    val importResult: ImportResult? = null,
    val exportResult: ExportResult? = null,
    val imageAttachmentsEnabled: Boolean = true,
    val driveBackup: DriveBackupUiState = DriveBackupUiState(),
)

data class DriveBackupUiState(
    val isSignedIn: Boolean = false,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val pendingAuthIntent: android.app.PendingIntent? = null,
    val autoBackupEnabled: Boolean = false,
    val backupWeekly: Boolean = true,
    val lastBackupTime: Long? = null,
    val backupOpStatus: DriveOpStatus = DriveOpStatus.Idle,
    val restoreOpStatus: DriveOpStatus = DriveOpStatus.Idle,
    val foundBackupFile: DriveFile? = null,
)

sealed class DriveOpStatus {
    data object Idle : DriveOpStatus()
    data object InProgress : DriveOpStatus()
    data class Success(val message: String) : DriveOpStatus()
    data class Error(val message: String) : DriveOpStatus()
    data object NoBackupFound : DriveOpStatus()
}
