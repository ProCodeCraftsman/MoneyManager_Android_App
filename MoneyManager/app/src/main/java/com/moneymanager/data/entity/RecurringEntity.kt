package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring",
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
        )
    ],
    indices = [Index("accountId"), Index("categoryId")]
)
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: String, // income, expense, savings, transfer, lend, borrow
    val amount: Double,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val goalId: Long? = null,
    val peerContactId: Long? = null,
    val tagIds: String = "",
    val description: String = "",
    val note: String = "",
    val frequency: String, // daily, weekly, biweekly, monthly, yearly
    val startDate: Long = System.currentTimeMillis(),
    val nextDate: Long,
    val endDate: Long? = null,
    val isActive: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderDays: Int = 0,
    val toAccountId: Long? = null,
    val investmentPlatform: String? = null,
    val receiptPath: String? = null,
    val expectedReturnDate: Long? = null,
    /** JSON-encoded list of split rows (see [com.moneymanager.domain.transaction.SplitTransactionFactory]); null when not a split template. */
    val splitData: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)