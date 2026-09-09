package com.moneymanager.data.worker

import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide advisory locks preventing two concurrently-running chains of the same
 * worker (e.g. the on-launch one-time trigger and the 12h periodic job, which are enqueued
 * under separate WorkManager unique-work names and can therefore run at the same time) from
 * both reading and processing the same due list, which would double-generate transactions or
 * double-post EMI installments.
 */
@Singleton
class GenerationLocks @Inject constructor() {
    val recurringGeneration = Mutex()
    val emiPosting = Mutex()
}
