package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = TransactionEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTransactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("subCategoryId"), Index("date"), Index("note"), Index("tagIds"), Index("parentTransactionId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: String, // income, expense, savings, transfer, lend, borrow
    val amount: Double, // always positive; sign derived from type
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val goalId: Long? = null,
    val peerContactId: Long? = null, // link to peer if type = lend/borrow
    val tagIds: String = "", // comma-separated tag IDs
    val date: Long = System.currentTimeMillis(),
    val description: String = "",
    val note: String = "",
    val receiptPath: String? = null, // base64 data URL
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val splitData: String? = null, // kept for backward compat
    val isSplitParent: Boolean = false,
    val isSplitChild: Boolean = false,
    val parentTransactionId: Long? = null,
    val isTransfer: Boolean = false,
    val toAccountId: Long? = null,
    val investmentPlatform: String? = null,
    val expectedReturnDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        val VALID_TYPES = listOf("income", "expense", "savings", "transfer", "lend", "borrow")
    }
}
