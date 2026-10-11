package com.moneymanager.domain.capture

import com.moneymanager.data.entity.CapturedMessageEntity
import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.SmsRuleOutcome
import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.serialization.json.Json

/** Counts for one scan, for the caller to show or log. Contains no message content. */
data class ScanReport(
    val newDrafts: Int,
    val alreadyCaptured: Int,
    val unmatched: Int,
    val notTransactions: Int,
    val capped: Boolean,
)

/**
 * Reads the inbox window, runs the model-free SMS stages on each message and writes pending ledger rows for
 * verified rule matches (F07 E4). Unmatched messages are left for the interactive flow; no model runs here.
 *
 * Messages in the cursor window are read oldest first. If the cap cuts the window short, the cursor stops at the
 * last message read, so the next scan picks up where this one ended and nothing older is skipped.
 */
class InboxScanner(
    private val inbox: SmsInbox,
    private val store: CaptureStore,
    private val classify: suspend (SmsInboxMessage, List<AccountEntry>) -> SmsRuleOutcome,
) {

    suspend fun scan(now: Long): ScanReport {
        val cursor = store.cursor()
        val plan = CaptureScanPlanner.plan(cursor.lastScanned, cursor.startDate, now)
        val messages = inbox.read(plan.lowerBound, plan.anchorMode, CaptureScanPlanner.MAX_MESSAGES)
        val accounts = if (messages.isEmpty()) emptyList() else store.activeAccounts()

        var newDrafts = 0
        var alreadyCaptured = 0
        var unmatched = 0
        var notTransactions = 0
        var knownRun = 0
        var newestProcessed: Long? = null

        for (message in messages) {
            newestProcessed = maxOf(newestProcessed ?: message.date, message.date)
            val hash = CaptureHash.message(message.sender, message.body, message.date)
            if (store.isCaptured(hash)) {
                alreadyCaptured++
                knownRun++
            } else {
                when (val outcome = classify(message, accounts)) {
                    is SmsRuleOutcome.NotATransaction -> notTransactions++
                    is SmsRuleOutcome.Draft -> {
                        val draft = outcome.draft.copy(sourceType = SOURCE_TYPE)
                        val row = CapturedMessageEntity(
                            messageHash = hash,
                            refHash = outcome.reference?.let { CaptureHash.reference(it) },
                            amount = draft.amount,
                            messageDate = message.date,
                            sourceKey = draft.accountLink,
                            draftJson = JSON.encodeToString(TransactionDraft.serializer(), draft),
                        )
                        if (store.insertPending(row)) {
                            newDrafts++
                            knownRun = if (store.hasSavedTransactionNear(draft.amount ?: 0.0, message.date)) knownRun + 1 else 0
                        } else {
                            alreadyCaptured++
                            knownRun++
                        }
                    }
                    is SmsRuleOutcome.Unmatched -> {
                        unmatched++
                        val parsed = outcome.parsed
                        val amount = parsed.amount
                        if (parsed.isActionableTransaction && amount != null) {
                            val known = store.hasSavedTransactionNear(amount, parsed.dateMillis ?: message.date)
                            knownRun = if (known) knownRun + 1 else 0
                        }
                    }
                }
            }
            // Anchor mode walks back from the newest message; a short run of known spends means it has caught up.
            if (plan.anchorMode && knownRun >= CaptureScanPlanner.ANCHOR_RUN) break
        }

        newestProcessed?.let { store.saveCursor(maxOf(it, cursor.lastScanned ?: it)) }
        return ScanReport(
            newDrafts = newDrafts,
            alreadyCaptured = alreadyCaptured,
            unmatched = unmatched,
            notTransactions = notTransactions,
            capped = messages.size >= CaptureScanPlanner.MAX_MESSAGES,
        )
    }

    private companion object {
        const val SOURCE_TYPE = "SMS"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
