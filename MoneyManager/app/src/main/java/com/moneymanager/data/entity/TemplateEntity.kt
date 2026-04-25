package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "templates",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("categoryId")]
)
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // income, expense, savings
    val amount: Double = 0.0,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)