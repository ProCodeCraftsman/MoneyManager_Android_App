package com.moneymanager.domain.ai

import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.repository.SmsAccountLinkRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Learn stage of the draft pipeline. What the user actually saved teaches merchant memory and the SMS account
 * links. The add screen and the history bootstrap both call this, so the rule for what counts as learned lives once.
 */
@Singleton
class DraftLearning @Inject constructor(
    private val merchantMemory: MerchantCategoryMemoryRepository,
    private val accountLinks: SmsAccountLinkRepository,
) {

    suspend fun learnMerchant(merchantHint: String, categoryId: Long, categoryName: String, typeId: String?) {
        merchantMemory.record(merchantHint, categoryId, categoryName, typeId)
    }

    /** Called after a draft from an SMS source is saved: the link points at the account the user kept. */
    suspend fun learnFromSave(linkKey: String, tx: TransactionEntity) {
        val accountId = accountToLearn(tx.type, tx.accountId, tx.toAccountId) ?: return
        accountLinks.learn(linkKey, accountId)
    }

    suspend fun learnSourceAccount(linkKey: String, accountId: Long) {
        accountLinks.learn(linkKey, accountId)
    }

    companion object {
        /**
         * The account an SMS source should point at. A card repayment's SMS describes the card, which is the
         * destination of the transfer. Null when a transfer has no destination, so nothing is learned.
         */
        fun accountToLearn(type: String, accountId: Long, toAccountId: Long?): Long? =
            if (type == "transfer") toAccountId else accountId
    }
}
