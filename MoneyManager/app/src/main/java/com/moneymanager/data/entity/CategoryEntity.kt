package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "📁",
    val type: String, // expense, income, savings
    val parentId: Long? = null,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)