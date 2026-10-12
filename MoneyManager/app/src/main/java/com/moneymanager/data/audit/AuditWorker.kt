package com.moneymanager.data.audit

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException

/**
 * Nightly second look at saved entries (F08 A2). Runs only while the phone is idle and charging. It notifies nobody:
 * findings wait in the SMS inbox under "Check these".
 */
@HiltWorker
class AuditWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val audit: AuditRepository,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = try {
        audit.runAndStore()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        const val WORK_NAME = "audit_nightly"
    }
}
