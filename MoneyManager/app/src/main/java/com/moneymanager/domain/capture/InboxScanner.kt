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
    val duplicates: Int = 0,
)

/**
 * Reads the inbox window, runs the model-free SMS stages on each message and writes ledger rows for verified rule
 * matches (F07 E4). A draft that is the same spend as one already recorded is stored as saved or merged and is not
 * shown; an uncertain one is shown with a "possible duplicate" flag (F07 E5). Unmatched messages are left for the
 * interactive flow; no model runs here.
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
        var duplicates = 0
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
                        val refHash = outcome.reference?.let { CaptureHash.reference(it) }
                        val verdict = verdictFor(draft, refHash, message.date)
                        val row = rowFor(hash, refHash, draft, message.date, verdict)
                        if (store.insertRow(row)) {
                            when (verdict) {
                                is DedupVerdict.New, is DedupVerdict.Uncertain -> newDrafts++
                                is DedupVerdict.SavedMatch, is DedupVerdict.PendingMatch -> duplicates++
                            }
                            val amount = draft.amount ?: 0.0
                            val known = verdict !is DedupVerdict.New || store.hasSavedTransactionNear(amount, message.date)
                            knownRun = if (known) knownRun + 1 else 0
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
            duplicates = duplicates,
        )
    }

    private suspend fun verdictFor(draft: TransactionDraft, refHash: String?, receivedAt: Long): DedupVerdict {
        val amount = draft.amount
        val date = draft.date ?: receivedAt
        val dedupDraft = DedupDraft(draft.typeId, amount ?: 0.0, date, draft.accountId, draft.merchantHint)
        return CaptureDedup.classify(
            draft = dedupDraft,
            ref = refHash?.let { store.ledgerRowWithRef(it) },
            savedNear = amount?.let { store.savedCandidatesNear(it, date) }.orEmpty(),
            pendingNear = amount?.let { store.pendingCandidatesNear(it, date) }.orEmpty(),
        )
    }

    private fun rowFor(
        hash: String,
        refHash: String?,
        draft: TransactionDraft,
        messageDate: Long,
        verdict: DedupVerdict,
    ): CapturedMessageEntity {
        val base = CapturedMessageEntity(
            messageHash = hash,
            refHash = refHash,
            amount = draft.amount,
            messageDate = messageDate,
            sourceKey = draft.accountLink,
        )
        return when (verdict) {
            is DedupVerdict.New -> base.copy(draftJson = encode(draft))
            is DedupVerdict.Uncertain -> base.copy(
                draftJson = encode(draft.copy(flags = draft.flags + "${CaptureFlags.MAYBE_DUPLICATE}${verdict.transactionId}")),
            )
            is DedupVerdict.SavedMatch -> base.copy(
                status = CapturedMessageEntity.STATUS_SAVED,
                transactionId = verdict.transactionId,
            )
            is DedupVerdict.PendingMatch -> base.copy(
                status = CapturedMessageEntity.STATUS_MERGED,
                mergedIntoId = verdict.ledgerId,
            )
        }
    }

    private fun encode(draft: TransactionDraft): String = JSON.encodeToString(TransactionDraft.serializer(), draft)

    private companion object {
        const val SOURCE_TYPE = "SMS"
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
