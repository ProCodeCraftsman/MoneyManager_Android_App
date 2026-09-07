package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emis",
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
data class EmiEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val accountId: Long,
    val categoryId: Long? = null,
    val totalAmount: Double,
    val tenureMonths: Int,
    val monthlyAmount: Double,
    val annualInterestRate: Double = 0.0,
    val isNoCost: Boolean = false,
    val processingFee: Double = 0.0,
    val startDate: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE", // ACTIVE, COMPLETED, FORECLOSED
    val createdAt: Long = System.currentTimeMillis()
)
