package com.moneymanager.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Periodic background worker that performs a local JSON backup.
 * Runs daily with no network constraints.
 */
@HiltWorker
class LocalBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val localBackupManager: LocalBackupManager,
    private val backupPreferences: BackupPreferences,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val localEnabled = backupPreferences.localBackupEnabled.first()
        if (!localEnabled) return Result.success()

        return try {
            localBackupManager.performLocalBackup().fold(
                onSuccess = {
                    backupPreferences.setLastLocalBackupTime(System.currentTimeMillis())
                    Result.success()
                },
                onFailure = {
                    if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME = "local_periodic_backup"
        private const val MAX_RETRIES = 3
    }
}
