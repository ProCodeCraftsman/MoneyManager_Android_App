package com.moneymanager.domain.learn

/** A saved transaction as the history join sees it. Only ids, amounts, dates and names; no notes or descriptions. */
data class SavedRow(
    val id: Long,
    val typeId: String,
    val amount: Double,
    val date: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val accountId: Long,
    val accountName: String,
    val accountType: String,
)

/** Read access to saved transactions for learning from history (F08 L1). */
interface SavedHistory {
    suspend fun rowsBetween(from: Long, to: Long): List<SavedRow>
}
