package com.moneymanager.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.moneymanager.data.repository.ExportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that encrypts and uploads a backup to Google Drive.
 *
 * Prerequisites before it will succeed:
 *  1. User has signed in to Drive at least once this session, OR silent token refresh succeeds.
 *  2. The encryption passphrase is a random key stored alongside the backup in Drive's
 *     appDataFolder (see [DriveBackupKeyManager]), not derived from the account itself.
 *
 * The worker retries with exponential back-off on transient failures (network, token expiry).
 */
@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val driveAuthManager: DriveAuthManager,
    private val driveBackupManager: DriveBackupManager,
    private val driveBackupKeyManager: DriveBackupKeyManager,
    private val encryptionHelper: EncryptionHelper,
    private val exportRepository: ExportRepository,
    private val backupPreferences: BackupPreferences,
    private val notificationManager: BackupNotificationManager,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        var driveSuccess = false
        var driveError: String? = null
        var errorCode: Int? = null

        val driveEnabled = backupPreferences.autoBackupEnabled.first()

        // Drive Backup
        if (driveEnabled) {
            val accessToken = driveAuthManager.getSilentAccessToken()
            if (accessToken != null) {
                try {
                    val passphrase = driveBackupKeyManager.getOrCreatePassphrase(accessToken).getOrThrow()
                    val jsonBytes = exportRepository.exportToJsonBytes()
                    val encrypted = encryptionHelper.encrypt(jsonBytes, passphrase)

                    driveBackupManager.uploadBackup(encrypted, accessToken).fold(
                        onSuccess = { fileId ->
                            driveBackupManager.deleteOldBackups(fileId, accessToken)
                            backupPreferences.setLastBackupTime(System.currentTimeMillis())
                            driveSuccess = true
                        },
                        onFailure = { e ->
                            driveError = e.message
                            if (e is DriveApiException) {
                                errorCode = e.code
                            }
                        }
                    )
                } catch (e: Exception) {
                    driveError = e.message
                }
            } else {
                driveError = "No Drive access token"
                errorCode = 403
            }
        }

        return when {
            !driveEnabled -> Result.success()
            driveSuccess -> Result.success()
            driveError != null && runAttemptCount < MAX_RETRIES -> Result.retry()
            else -> {
                notificationManager.showBackupFailedNotification(errorCode)
                Result.failure(workDataOf(KEY_ERROR to (driveError ?: "Backup failed")))
            }
        }
    }

    companion object {
        const val WORK_NAME = "drive_periodic_backup"
        const val KEY_ERROR = "error"
        private const val MAX_RETRIES = 3
    }
}
