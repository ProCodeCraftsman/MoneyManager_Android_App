package com.moneymanager.data.worker

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules a true periodic [RecurringGenerationWorker] run so recurring transactions post even
 * if the app isn't opened. `MainActivity` still enqueues a one-time run on launch (with its own
 * catch-up loop for missed occurrences) for immediate freshness when the user opens the app;
 * this periodic job is the fallback that keeps things current in between launches.
 */
@Singleton
class RecurringScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<RecurringGenerationWorker>(12, TimeUnit.HOURS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    companion object {
        const val WORK_NAME = "recurring_periodic_generation"
    }
}
