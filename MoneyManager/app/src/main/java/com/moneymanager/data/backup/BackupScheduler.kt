package com.moneymanager.data.backup

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues and cancels the [DriveBackupWorker] periodic task. */
@Singleton
class BackupScheduler @Inject constructor(@ApplicationContext private val context: Context) {

    private val workManager = WorkManager.getInstance(context)

    fun scheduleBackup(isWeekly: Boolean = true) {
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

    fun cancelBackup() {
        workManager.cancelUniqueWork(DriveBackupWorker.WORK_NAME)
    }
}
