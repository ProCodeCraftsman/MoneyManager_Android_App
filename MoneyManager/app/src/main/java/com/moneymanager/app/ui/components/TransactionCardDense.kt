package com.moneymanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.R
import com.moneymanager.app.ui.constants.COLOR_EXPENSE
import com.moneymanager.app.ui.constants.COLOR_SAVINGS
import com.moneymanager.app.ui.constants.COLOR_TRANSFER
import com.moneymanager.app.ui.constants.ICON_BORROW
import com.moneymanager.app.ui.constants.ICON_DEFAULT
import com.moneymanager.app.ui.constants.ICON_LEND
import com.moneymanager.app.ui.constants.ICON_SAVINGS
import com.moneymanager.app.ui.constants.ICON_SPLIT
import com.moneymanager.app.ui.constants.ICON_TRANSFER
import com.moneymanager.app.ui.constants.SEPARATOR_DOT
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionCardDense(
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
    val subcategory = remember(transaction.subCategoryId, categories) {
        transaction.subCategoryId?.let { id -> categories.find { it.id == id } }
    }
    val toAccount = remember(transaction.toAccountId, accounts) {
        transaction.toAccountId?.let { id -> accounts.find { it.id == id } }
    }
    val peer = remember(transaction.peerContactId, peers) {
        transaction.peerContactId?.let { id -> peers.find { it.id == id } }
    }

    val typeColor = when (transaction.type) {
        "income" -> MaterialTheme.colorScheme.secondary
        "expense", "lend" -> COLOR_EXPENSE
        "savings" -> COLOR_SAVINGS
        "transfer" -> COLOR_TRANSFER
        else -> MaterialTheme.colorScheme.onSurface
    }

    val typeIcon = when {
        transaction.isSplitParent -> ICON_SPLIT
        transaction.isTransfer || transaction.type == "transfer" -> ICON_TRANSFER
        transaction.type == "lend" -> ICON_LEND
        transaction.type == "borrow" -> ICON_BORROW
        else -> category?.emoji ?: ICON_DEFAULT
    }

    val typeIconType = when {
        transaction.isSplitParent || transaction.isTransfer || transaction.type == "transfer" || transaction.type == "lend" || transaction.type == "borrow" -> "emoji"
        else -> category?.iconType ?: "emoji"
    }

    val amountText = if (transaction.type == "expense") {
        "-${currencyFormat.format(transaction.amount)}"
    } else {
        currencyFormat.format(transaction.amount)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(typeColor.copy(alpha = 0.1f)),
            Alignment.Center
        ) {
            CategoryIcon(emoji = typeIcon, iconType = typeIconType, colorIndex = category?.colorIndex, fontSize = 18.sp)
        }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (transaction.type == "transfer")
                            stringResource(R.string.transfer)
                        else if (showCategory)
                            (category?.name ?: transaction.type.replaceFirstChar { it.uppercase() })
                        else
                            transaction.type.replaceFirstChar { it.uppercase() },

                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subcategory != null) {
                        Text(
                            text = " - ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = subcategory.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (transaction.type == "transfer" && toAccount != null) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = account?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = COLOR_TRANSFER
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = toAccount.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = COLOR_TRANSFER
                        )
                    } else if (transaction.type == "lend" && peer != null) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = COLOR_EXPENSE
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = account?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = COLOR_EXPENSE
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = peer.effectiveDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = COLOR_EXPENSE
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = account?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (transaction.note.isNotBlank() || transaction.description.isNotBlank()) {
                        val desc = transaction.note.ifBlank { transaction.description }
                        Text(SEPARATOR_DOT, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
}