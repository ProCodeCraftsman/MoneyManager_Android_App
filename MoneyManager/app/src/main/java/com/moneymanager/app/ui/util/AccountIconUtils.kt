package com.moneymanager.app.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

fun accountTypeIcon(type: String): ImageVector = when (type) {
    "bank" -> Icons.Default.AccountBalance
    "cash" -> Icons.Default.AccountBalanceWallet
    "credit" -> Icons.Default.CreditCard
    "savings" -> Icons.AutoMirrored.Filled.TrendingUp
    "peer" -> Icons.Default.People
    else -> Icons.Default.AccountBalance
}
