package com.moneymanager.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.moneymanager.data.repository.ExportRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic background worker that encrypts and uploads a backup to Google Drive.
 *
 * Prerequisites before it will succeed:
 *  1. User has signed in to Drive at least once this session, OR silent token refresh succeeds.
 *  2. A backup passphrase was saved via [BackupPassphraseStore] (happens on first manual backup).
 *
 * The worker retries with exponential back-off on transient failures (network, token expiry).
 */
@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val driveAuthManager: DriveAuthManager,
    private val driveBackupManager: DriveBackupManager,
    private val encryptionHelper: EncryptionHelper,
    private val exportRepository: ExportRepository,
    private val backupPreferences: BackupPreferences,
    private val passphraseStore: BackupPassphraseStore,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val passphrase = passphraseStore.getPassphrase()
            ?: return Result.failure(workDataOf(KEY_ERROR to "No backup passphrase configured. Run a manual backup first."))

        val accessToken = driveAuthManager.getSilentAccessToken()
            ?: return Result.retry()

        return try {
            val jsonBytes = exportRepository.exportToJsonBytes()
            val encrypted = encryptionHelper.encrypt(jsonBytes, passphrase)

            driveBackupManager.uploadBackup(encrypted, accessToken).fold(
                onSuccess = { fileId ->
                    driveBackupManager.deleteOldBackups(fileId, accessToken)
                    backupPreferences.setLastBackupTime(System.currentTimeMillis())
                    Result.success()
                },
                onFailure = { e ->
                    if (runAttemptCount < MAX_RETRIES) Result.retry()
                    else Result.failure(workDataOf(KEY_ERROR to e.message))
                }
            )
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to e.message))
        }
    }

    companion object {
        const val WORK_NAME = "drive_periodic_backup"
        const val KEY_ERROR = "error"
        private const val MAX_RETRIES = 3
    }
}
