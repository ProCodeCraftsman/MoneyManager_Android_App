package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val emoji: String = "📁",
    val iconType: String = "emoji", // "emoji", "material", "image"
    val color: String = "#90A4AE", // Hex color string
    val type: String, // expense, income, savings
    val parentId: Long? = null,
    val isCustom: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)