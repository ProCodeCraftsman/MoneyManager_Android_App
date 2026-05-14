package com.moneymanager.app.ui.dialogs

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*

enum class TransactionFeature {
    CATEGORY,
    SPLIT,
    TAGS,
    NOTE,
    RECEIPT,
    PEER,
    RETURN_DATE,
    TO_ACCOUNT,
    GOAL,
    PLATFORM,
}

data class FormTypeConfig(
    val id: String,
    val label: String,
    val displayName: String,
    val icon: ImageVector,
    val features: Set<TransactionFeature>,
    val categoryFilterType: String? = null,
)

object TransactionFormConfig {
    val allTypes = listOf(
        FormTypeConfig("expense", "Exp", "Expense", Icons.Default.MoneyOff,
            setOf(TransactionFeature.CATEGORY, TransactionFeature.SPLIT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("income", "Inc", "Income", Icons.Default.Money,
            setOf(TransactionFeature.CATEGORY, TransactionFeature.SPLIT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("savings", "Sav", "Savings", Icons.AutoMirrored.Filled.TrendingUp,
            setOf(TransactionFeature.CATEGORY, TransactionFeature.SPLIT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT, TransactionFeature.GOAL, TransactionFeature.PLATFORM),
            categoryFilterType = "expense"),
        FormTypeConfig("transfer", "Tra", "Transfer", Icons.Default.SwapHoriz,
            setOf(TransactionFeature.TO_ACCOUNT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("lend", "Len", "Lending", Icons.Default.Handshake,
            setOf(TransactionFeature.PEER, TransactionFeature.RETURN_DATE, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("borrow", "Bor", "Borrowing", Icons.AutoMirrored.Filled.CallReceived,
            setOf(TransactionFeature.PEER, TransactionFeature.RETURN_DATE, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
    )

    fun getType(id: String): FormTypeConfig = allTypes.first { it.id == id }
    fun hasFeature(typeId: String, feature: TransactionFeature): Boolean = getType(typeId).features.contains(feature)
    fun resolveCategoryType(typeId: String): String = getType(typeId).categoryFilterType ?: typeId
}
