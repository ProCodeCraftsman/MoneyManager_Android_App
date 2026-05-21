package com.moneymanager.app.ui.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.constants.ICON_BORROW
import com.moneymanager.app.ui.constants.ICON_DEFAULT
import com.moneymanager.app.ui.constants.ICON_LEND
import com.moneymanager.app.ui.constants.ICON_SAVINGS
import com.moneymanager.app.ui.constants.ICON_TRANSFER
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    showCategory: Boolean = true,
    onClick: (TransactionEntity) -> Unit,
) {
    Column {
        TransactionRow(
            transaction = transaction,
            accounts = accounts,
            categories = categories,
            peers = peers,
            currencyFormat = currencyFormat,
            showCategory = showCategory,
            onClick = { onClick(transaction) }
        )
        HorizontalDivider(
            modifier = Modifier.padding(start = 50.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun TransactionRow(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    showCategory: Boolean = true,
    onClick: () -> Unit
) {
    val account = remember(transaction.accountId, accounts) { accounts.find { it.id == transaction.accountId } }
    val category = remember(transaction.categoryId, categories) { categories.find { it.id == transaction.categoryId } }
    val peer = remember(transaction.peerContactId, peers) {
        transaction.peerContactId?.let { id -> peers.find { it.id == id } }
    }

    val typeIcon = when {
        transaction.isTransfer || transaction.type == "transfer" -> ICON_TRANSFER
        transaction.type == "lend" -> ICON_LEND
        transaction.type == "borrow" -> ICON_BORROW
        category != null && showCategory -> category.emoji
        else -> ICON_DEFAULT
    }
    val typeIconType = when {
        transaction.isTransfer || transaction.type == "transfer" || transaction.type == "lend" || transaction.type == "borrow" -> "emoji"
        category != null && showCategory -> category.iconType
        else -> "emoji"
    }

    val iconBg = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(
                emoji = typeIcon,
                iconType = typeIconType,
                colorIndex = category?.colorIndex,
                fontSize = 18.sp,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            val hasDescription = transaction.description.isNotBlank()
            val cat = if (showCategory) category else null
            val title = when {
                hasDescription -> transaction.description
                cat != null -> cat.name
                else -> transaction.type.replaceFirstChar { it.uppercase() }
            }
            val subtitle = when {
                hasDescription && cat != null -> cat.name
                !hasDescription && peer != null -> peer.effectiveDisplayName
                !hasDescription && account != null -> account.name
                else -> null
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        val amountPrefix = if (transaction.type == "expense") "-" else ""
        Text(
            text = "$amountPrefix${currencyFormat.format(transaction.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = when (transaction.type) {
                "income" -> MaterialTheme.colorScheme.primary
                "expense", "lend" -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}
