package com.moneymanager.domain.capture

import com.moneymanager.domain.ai.SmsDraftPipeline
import javax.inject.Inject

/**
 * Runs one inbox scan with the model-free SMS stages. No UI calls it yet; the app-open scan and the
 * "Scan now" tile arrive with AI-056 and AI-058.
 */
class ScanInboxUseCase @Inject constructor(
    private val inbox: SmsInbox,
    private val store: CaptureStore,
    private val pipeline: SmsDraftPipeline,
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): ScanReport =
        InboxScanner(inbox, store) { message, accounts ->
            pipeline.fromRules(message.body, message.sender, null, accounts)
        }.scan(now)
}
