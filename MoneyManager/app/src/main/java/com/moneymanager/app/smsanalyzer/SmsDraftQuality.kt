package com.moneymanager.app.smsanalyzer

import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.sms.ParsedSms
import com.moneymanager.domain.sms.SmsAccountLinks
import com.moneymanager.domain.sms.SmsDraftMapper

/** What a stored merchant-category memory entry says; only the kind of transaction matters here. */
data class MemoryHit(val typeId: String?)

/** The user's real accounts and merchant memory, so a scan can measure how much editing drafts need. */
class DraftContext(
    val accounts: List<AccountEntry>,
    val links: SmsAccountLinks = SmsAccountLinks.EMPTY,
    val memoryFor: (merchant: String, typeId: String) -> MemoryHit?,
)

/** How one SMS would turn out as a draft. Holds no values, only which fields resolved. */
data class DraftOutcome(
    val accountHinted: Boolean,
    val accountMatched: Boolean,
    val categoryNeeded: Boolean,
    val categoryFilled: Boolean,
    val isTransfer: Boolean,
    val ready: Boolean,
)

/** Per-template counters of [DraftOutcome]. */
data class DraftQuality(
    val drafts: Int = 0,
    val ready: Int = 0,
    val accountHinted: Int = 0,
    val accountMatched: Int = 0,
    val categoryNeeded: Int = 0,
    val categoryFilled: Int = 0,
    val transfers: Int = 0,
) {
    val accountHintedButUnmatched: Int get() = accountHinted - minOf(accountHinted, accountMatched)
    val notReady: Int get() = drafts - ready

    operator fun plus(o: DraftQuality) = DraftQuality(
        drafts + o.drafts, ready + o.ready, accountHinted + o.accountHinted, accountMatched + o.accountMatched,
        categoryNeeded + o.categoryNeeded, categoryFilled + o.categoryFilled, transfers + o.transfers,
    )

    fun add(o: DraftOutcome) = this + DraftQuality(
        drafts = 1,
        ready = if (o.ready) 1 else 0,
        accountHinted = if (o.accountHinted) 1 else 0,
        accountMatched = if (o.accountMatched) 1 else 0,
        categoryNeeded = if (o.categoryNeeded) 1 else 0,
        categoryFilled = if (o.categoryFilled) 1 else 0,
        transfers = if (o.isTransfer) 1 else 0,
    )

    companion object {
        /**
         * Runs the same mapper and merchant-memory rule as the app's SMS flow. "Ready" means the draft needs
         * no edit to post: an account matched and, where the type carries a category, a category was filled.
         * Card repayments are never ready because the source account is left to the user.
         */
        fun evaluate(parsed: ParsedSms, sender: String?, ctx: DraftContext): DraftOutcome? {
            val draft = SmsDraftMapper.toDraft(parsed, ctx.accounts, sender, links = ctx.links) ?: return null
            val isTransfer = draft.typeId == "transfer"
            val accountMatched = draft.accountId != null || draft.toAccountId != null
            val categoryNeeded = SmsDraftMapper.needsCategory(draft.typeId)
            val categoryFilled = categoryNeeded && draft.merchantHint?.let { hint ->
                ctx.memoryFor(hint, draft.typeId.orEmpty())?.let { SmsDraftMapper.memoryApplies(draft.typeId, it.typeId) }
            } == true
            return DraftOutcome(
                accountHinted = parsed.accountHint != null,
                accountMatched = accountMatched,
                categoryNeeded = categoryNeeded,
                categoryFilled = categoryFilled,
                isTransfer = isTransfer,
                ready = !isTransfer && accountMatched && (!categoryNeeded || categoryFilled),
            )
        }
    }
}
