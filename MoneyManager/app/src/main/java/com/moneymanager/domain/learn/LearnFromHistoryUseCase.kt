package com.moneymanager.domain.learn

import com.moneymanager.domain.ai.SmsDraftPipeline
import com.moneymanager.domain.ai.SmsRuleOutcome
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.capture.SmsInbox
import javax.inject.Inject

/**
 * Read-only bootstrap: reads six months of inbox SMS, keeps the rule matches and joins them to saved transactions
 * (F08 L1). Nothing is written here; the confirm screen (L2) decides what to learn.
 */
class LearnFromHistoryUseCase @Inject constructor(
    private val inbox: SmsInbox,
    private val store: CaptureStore,
    private val history: SavedHistory,
    private val pipeline: SmsDraftPipeline,
) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): HistoryLabels {
        val from = now - WINDOW_MS
        val accounts = store.activeAccounts()
        val spends = inbox.read(from, newestFirst = true, limit = MAX_MESSAGES).mapNotNull { message ->
            val draft = (pipeline.fromRules(message.body, message.sender, null, accounts) as? SmsRuleOutcome.Draft)
                ?.draft ?: return@mapNotNull null
            val amount = draft.amount ?: return@mapNotNull null
            val typeId = draft.typeId ?: return@mapNotNull null
            SmsSpend(
                date = draft.date ?: message.date,
                amount = amount,
                typeId = typeId,
                merchantHint = draft.merchantHint,
                sourceKey = draft.accountLink,
            )
        }
        // A day either side, so a spend near the window edge still finds its saved row.
        val saved = history.rowsBetween(from - HistoryJoin.DAY_MS, now + HistoryJoin.DAY_MS)
        return HistoryJoin.join(spends, saved)
    }

    private companion object {
        const val WINDOW_MS = 180L * 24 * 60 * 60 * 1000
        const val MAX_MESSAGES = 5000
    }
}
