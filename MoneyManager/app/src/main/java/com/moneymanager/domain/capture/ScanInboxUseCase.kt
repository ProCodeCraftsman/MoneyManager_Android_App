package com.moneymanager.domain.capture

import com.moneymanager.domain.ai.SmsDraftPipeline
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/**
 * Runs one inbox scan with the model-free SMS stages. Called on app open (AI-058) and by the reminder worker
 * (AI-059). Scans share one lock: two overlapping scans would read the same cursor and race on the cursor save.
 */
class ScanInboxUseCase @Inject constructor(
    private val inbox: SmsInbox,
    private val store: CaptureStore,
    private val pipeline: SmsDraftPipeline,
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): ScanReport = scanLock.withLock {
        InboxScanner(inbox, store) { message, accounts ->
            pipeline.fromRules(message.body, message.sender, null, accounts)
        }.scan(now)
    }

    private companion object {
        val scanLock = Mutex()
    }
}
