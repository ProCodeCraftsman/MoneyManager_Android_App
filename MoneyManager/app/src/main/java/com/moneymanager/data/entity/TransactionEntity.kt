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
        )
    ],
    indices = [Index("accountId"), Index("categoryId"), Index("date"), Index("note"), Index("tagIds")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: String, // income, expense, savings, transfer
    val amount: Double,
    val categoryId: Long? = null,
    val tagIds: String = "", // comma-separated tag IDs
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val receiptPath: String? = null,
    val isRecurring: Boolean = false,
    val recurringId: Long? = null,
    val splitData: String? = null, // JSON for split transactions
    val createdAt: Long = System.currentTimeMillis()
)