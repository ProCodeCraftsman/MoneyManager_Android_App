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
import androidx.compose.ui.text.style.TextOverflow
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
    activeAccountId: Long? = null,
    showCategory: Boolean = true,
    overrideToAccountId: Long? = null,
    onClick: (TransactionEntity) -> Unit,
) {
    Column {
        TransactionRow(
            transaction = transaction,
            accounts = accounts,
            categories = categories,
            peers = peers,
            currencyFormat = currencyFormat,
            activeAccountId = activeAccountId,
            showCategory = showCategory,
            overrideToAccountId = overrideToAccountId,
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
    activeAccountId: Long? = null,
    showCategory: Boolean = true,
    overrideToAccountId: Long? = null,
    onClick: () -> Unit
) {
    val account = remember(transaction.accountId, accounts) { accounts.find { it.id == transaction.accountId } }
    val toAccount = remember(transaction.toAccountId, overrideToAccountId, accounts) {
        val id = overrideToAccountId ?: transaction.toAccountId
        id?.let { accounts.find { it.id == id } }
    }
    val category = remember(transaction.categoryId, categories) { categories.find { it.id == transaction.categoryId } }
    val peer = remember(transaction.peerContactId, peers) {
        transaction.peerContactId?.let { id -> peers.find { it.id == id } }
    }

    val isTransfer = transaction.isTransfer || transaction.type == "transfer"
    val isIncoming = isTransfer && activeAccountId != null && 
                     (transaction.toAccountId == activeAccountId || transaction.note.contains("from", ignoreCase = true))

    val typeIcon = when {
        isTransfer -> ICON_TRANSFER
        transaction.type == "lend" -> ICON_LEND
        transaction.type == "borrow" -> ICON_BORROW
        transaction.type == "savings" -> ICON_SAVINGS
        category != null && showCategory -> category.emoji
        else -> ICON_DEFAULT
    }
    val typeIconType = when {
        isTransfer || transaction.type == "lend" || 
        transaction.type == "borrow" || transaction.type == "savings" -> "emoji"
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
            val title: String
            val subtitle: String?
            
            if (isTransfer) {
                val fromName = account?.name ?: "Account"
                val toName = toAccount?.name ?: "Account"
                title = "Transfer"
                val isOrphanedIncoming = toAccount == null && transaction.note.contains("from", ignoreCase = true)
                subtitle = if (isOrphanedIncoming) "$toName → $fromName" else "$fromName → $toName"
            } else {
                val cat = if (showCategory) category else null
                val isSimpleType = transaction.type in setOf("expense", "income", "savings")
                if (isSimpleType) {
                    title = cat?.name ?: transaction.type.replaceFirstChar { it.uppercase() }
                    subtitle = account?.name
                } else {
                    val hasDescription = transaction.description.isNotBlank()
                    title = when {
                        hasDescription -> transaction.description
                        cat != null -> cat.name
                        else -> transaction.type.replaceFirstChar { it.uppercase() }
                    }
                    subtitle = when {
                        hasDescription && cat != null -> cat.name
                        !hasDescription && peer != null -> peer.effectiveDisplayName
                        !hasDescription && account != null -> account.name
                        else -> null
                    }
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        val amountPrefix = when {
            isTransfer -> when {
                activeAccountId == null -> ""
                isIncoming -> "+"
                else -> "-"
            }
            transaction.type == "expense" -> "-"
            else -> ""
        }
        val amountColor = when {
            isTransfer -> when {
                activeAccountId == null -> MaterialTheme.colorScheme.secondary
                isIncoming -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.error
            }
            transaction.type == "income" -> MaterialTheme.colorScheme.primary
            transaction.type == "expense" || transaction.type == "lend" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurface
        }
        Text(
            text = "$amountPrefix${currencyFormat.format(transaction.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor
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
