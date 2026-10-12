package com.moneymanager.domain.capture

import kotlin.math.abs

/** What the scanner knows about a new draft when it looks for the same spend already recorded. */
data class DedupDraft(
    val typeId: String?,
    val amount: Double,
    val date: Long,
    val accountId: Long?,
    val merchantHint: String?,
)

/**
 * A saved transaction (id = transaction id) or a pending draft (id = ledger id) that could be the same spend.
 * [merchantText] is what the merchant check reads: description and note for saved rows, the merchant hint for drafts.
 */
data class CaptureCandidate(
    val id: Long,
    val typeId: String?,
    val amount: Double,
    val date: Long,
    val accountId: Long?,
    val merchantText: String,
)

/** The ledger row that already holds a reference number. [transactionId] is set when that row was saved. */
data class LedgerMatch(val ledgerId: Long, val transactionId: Long?)

sealed interface DedupVerdict {
    data object New : DedupVerdict

    /** Same spend as a saved transaction: the message is recorded as saved and not shown again. */
    data class SavedMatch(val transactionId: Long) : DedupVerdict

    /** Same spend as a pending draft: the message is recorded as merged into that draft. */
    data class PendingMatch(val ledgerId: Long) : DedupVerdict

    /** Same amount close in time, but not provably the same spend: shown in the inbox as merge or keep both. */
    data class Uncertain(val transactionId: Long) : DedupVerdict
}

/**
 * Semantic duplicate check for automatic capture (F07 E5). Order matters:
 * 1. Reference number: the strongest key, one ledger row per reference.
 * 2. Same amount, same type, within 48 h, same account (or unknown account), and a merchant match, against
 *    saved transactions first and then pending drafts.
 * 3. Same amount and window without a merchant match is only uncertain: it is shown, never hidden.
 *
 * Saved candidates the ledger already claims are excluded by the store, so each saved spend can absorb at most one
 * message. That keeps repeat payments (two teas in one day) from hiding each other.
 */
object CaptureDedup {
    const val WINDOW_MS = 48L * 60 * 60 * 1000
    private const val AMOUNT_EPSILON = 0.005
    private const val MIN_MERCHANT_LENGTH = 4

    fun classify(
        draft: DedupDraft,
        ref: LedgerMatch?,
        savedNear: List<CaptureCandidate>,
        pendingNear: List<CaptureCandidate>,
    ): DedupVerdict {
        if (ref != null) {
            return ref.transactionId?.let { DedupVerdict.SavedMatch(it) } ?: DedupVerdict.PendingMatch(ref.ledgerId)
        }
        val sameSpend = { c: CaptureCandidate ->
            c.typeId == draft.typeId &&
                abs(c.amount - draft.amount) < AMOUNT_EPSILON &&
                abs(c.date - draft.date) <= WINDOW_MS
        }
        val sameAccount = { c: CaptureCandidate -> draft.accountId == null || c.accountId == draft.accountId }
        val savedCandidates = savedNear.filter(sameSpend)
        val pendingCandidates = pendingNear.filter(sameSpend)

        savedCandidates.firstOrNull { sameAccount(it) && merchantMatches(draft.merchantHint, it.merchantText) }
            ?.let { return DedupVerdict.SavedMatch(it.id) }
        pendingCandidates.firstOrNull { sameAccount(it) && merchantMatches(draft.merchantHint, it.merchantText) }
            ?.let { return DedupVerdict.PendingMatch(it.id) }
        savedCandidates.firstOrNull()?.let { return DedupVerdict.Uncertain(it.id) }
        return DedupVerdict.New
    }

    /** Short merchant strings ("tea") match too much, so anything under four letters never counts as a match. */
    fun merchantMatches(hint: String?, text: String): Boolean {
        val h = key(hint)
        val t = key(text)
        return h.length >= MIN_MERCHANT_LENGTH && t.contains(h)
    }

    private fun key(text: String?): String = text.orEmpty().lowercase().filter { it.isLetterOrDigit() }
}
