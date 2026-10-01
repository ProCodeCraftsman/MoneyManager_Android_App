package com.moneymanager.domain.account

import com.moneymanager.data.dao.AccountUsage
import com.moneymanager.data.entity.AccountEntity

/** One configurable default-account field. [key] is the persisted preference suffix; do not rename. */
enum class DefaultAccountSlot(val key: String, val txType: String, val isDestination: Boolean) {
    EXPENSE("expense", "expense", false),
    INCOME("income", "income", false),
    TRANSFER_FROM("transfer_from", "transfer", false),
    TRANSFER_TO("transfer_to", "transfer", true),
    SAVINGS_FROM("savings_from", "savings", false),
    SAVINGS_TO("savings_to", "savings", true),
    LEND("lend", "lend", false),
    BORROW("borrow", "borrow", false);

    companion object {
        fun fromKey(key: String): DefaultAccountSlot? = entries.firstOrNull { it.key == key }
    }
}

object DefaultAccountResolver {

    /** Accounts that may be chosen (or defaulted) for [slot]. Archived accounts are never eligible. */
    fun eligibleAccounts(slot: DefaultAccountSlot, accounts: List<AccountEntity>): List<AccountEntity> =
        accounts.filter { !it.isArchived && isEligible(slot, it) }

    private fun isEligible(slot: DefaultAccountSlot, account: AccountEntity): Boolean = when (slot) {
        // Peer accounts are only meaningful for lend/borrow.
        DefaultAccountSlot.LEND, DefaultAccountSlot.BORROW -> true
        // Investment platforms are not spending sources, matching the add-transaction dialog.
        DefaultAccountSlot.EXPENSE, DefaultAccountSlot.SAVINGS_FROM ->
            account.type != "peer" && account.type != "savings"
        DefaultAccountSlot.SAVINGS_TO -> account.type == "savings"
        DefaultAccountSlot.INCOME, DefaultAccountSlot.TRANSFER_FROM, DefaultAccountSlot.TRANSFER_TO ->
            account.type != "peer"
    }

    /**
     * Picks the account id to preselect for [slot]: the configured default if still eligible, else the most used
     * eligible account for that transaction type, else the first eligible one. [excludeAccountId] is skipped at
     * every step (used to keep transfer/savings from and to apart). Returns null if nothing qualifies.
     */
    fun resolve(
        slot: DefaultAccountSlot,
        accounts: List<AccountEntity>,
        configured: Map<String, Long>,
        usage: List<AccountUsage>,
        excludeAccountId: Long? = null
    ): Long? {
        val eligible = eligibleAccounts(slot, accounts).filter { it.id != excludeAccountId }
        if (eligible.isEmpty()) return null

        configured[slot.key]?.let { id -> if (eligible.any { it.id == id }) return id }

        val counts = HashMap<Long, Int>()
        usage.filter { it.type == slot.txType }.forEach { row ->
            val target = if (slot.isDestination) row.toAccountId else row.accountId
            if (target != null) counts[target] = (counts[target] ?: 0) + row.count
        }
        // maxByOrNull keeps the first of equal maxima, so ties fall back to list order (alphabetical from the DAO)
        val mostUsed = eligible.filter { (counts[it.id] ?: 0) > 0 }.maxByOrNull { counts.getValue(it.id) }
        return (mostUsed ?: eligible.first()).id
    }
}
