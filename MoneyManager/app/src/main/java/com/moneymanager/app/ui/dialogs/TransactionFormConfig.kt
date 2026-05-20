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
        FormTypeConfig("expense", "Expense", "Expense", Icons.Default.AccountBalanceWallet,
            setOf(TransactionFeature.CATEGORY, TransactionFeature.SPLIT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("income", "Income", "Income", Icons.AutoMirrored.Filled.TrendingUp,
            setOf(TransactionFeature.CATEGORY, TransactionFeature.SPLIT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("savings", "Savings", "Savings", Icons.Default.Savings,
            setOf(TransactionFeature.CATEGORY, TransactionFeature.SPLIT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT, TransactionFeature.GOAL, TransactionFeature.PLATFORM),
            categoryFilterType = "expense"),
        FormTypeConfig("transfer", "Transfer", "Transfer", Icons.Default.SwapHoriz,
            setOf(TransactionFeature.TO_ACCOUNT, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("lend", "Lending", "Lending", Icons.Default.Handshake,
            setOf(TransactionFeature.PEER, TransactionFeature.RETURN_DATE, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
        FormTypeConfig("borrow", "Borrow", "Borrowing", Icons.Default.SouthWest,
            setOf(TransactionFeature.PEER, TransactionFeature.RETURN_DATE, TransactionFeature.TAGS, TransactionFeature.NOTE, TransactionFeature.RECEIPT)),
    )

    fun getType(id: String): FormTypeConfig = allTypes.firstOrNull { it.id == id } ?: allTypes.first()
    fun hasFeature(typeId: String, feature: TransactionFeature): Boolean = getType(typeId).features.contains(feature)
    fun resolveCategoryType(typeId: String): String = getType(typeId).categoryFilterType ?: typeId
}
