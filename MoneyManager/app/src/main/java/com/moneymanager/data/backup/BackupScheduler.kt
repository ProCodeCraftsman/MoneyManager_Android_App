package com.moneymanager.data.backup

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues and cancels the periodic backup tasks. */
@Singleton
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupPreferences: BackupPreferences
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Ensures backups are correctly scheduled based on current preferences.
     * Called on app start to migrate/refresh work requests.
     */
    suspend fun initialize() {
        val localEnabled = backupPreferences.localBackupEnabled.first()
        if (localEnabled) {
            scheduleLocalBackup()
        } else {
            cancelLocalBackup()
        }

        val driveEnabled = backupPreferences.autoBackupEnabled.first()
        if (driveEnabled) {
            scheduleDriveBackup(backupPreferences.backupWeekly.first())
        } else {
            cancelDriveBackup()
        }
    }

    /**
     * Schedules a daily local backup with no network constraints.
     */
    fun scheduleLocalBackup() {
        val request = PeriodicWorkRequestBuilder<LocalBackupWorker>(1L, TimeUnit.DAYS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            LocalBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Use KEEP if already exists to avoid resetting timer
            request
        )
    }

    /**
     * Schedules a periodic Google Drive backup with network constraints.
     */
    fun scheduleDriveBackup(isWeekly: Boolean) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(
            if (isWeekly) 7L else 1L, TimeUnit.DAYS
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DriveBackupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelLocalBackup() {
        workManager.cancelUniqueWork(LocalBackupWorker.WORK_NAME)
    }

    fun cancelDriveBackup() {
        workManager.cancelUniqueWork(DriveBackupWorker.WORK_NAME)
    }

    /**
     * Deprecated: Use [scheduleLocalBackup] and [scheduleDriveBackup] independently.
     */
    @Deprecated("Use specific schedule methods", ReplaceWith("scheduleLocalBackup(); scheduleDriveBackup(isWeekly)"))
    fun scheduleBackup(isWeekly: Boolean = true) {
        scheduleLocalBackup()
        scheduleDriveBackup(isWeekly)
    }

    /**
     * Deprecated: Use [cancelLocalBackup] and [cancelDriveBackup] independently.
     */
    @Deprecated("Use specific cancel methods", ReplaceWith("cancelLocalBackup(); cancelDriveBackup()"))
    fun cancelBackup() {
        cancelLocalBackup()
        cancelDriveBackup()
    }
}
