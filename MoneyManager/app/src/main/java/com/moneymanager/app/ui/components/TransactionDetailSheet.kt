package com.moneymanager.app.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.theme.LocalCategoryColors
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.app.ui.util.accountTypeIcon
import com.moneymanager.data.entity.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailSheet(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    peers: List<PeerContact> = emptyList(),
    goals: List<GoalEntity> = emptyList(),
    currency: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = remember(transaction.categoryId, categories) {
        categories.find { it.id == transaction.categoryId }
    }
    val subCategory = remember(transaction.subCategoryId, categories) {
        transaction.subCategoryId?.let { id -> categories.find { it.id == id } }
    }
    val parentCategory = remember(category, categories) {
        category?.parentId?.let { pid -> categories.find { it.id == pid } }
    }
    val account = remember(transaction.accountId, accounts) {
        accounts.find { it.id == transaction.accountId }
    }
    val toAccount = remember(transaction.toAccountId, accounts) {
        transaction.toAccountId?.let { id -> accounts.find { it.id == id } }
    }
    val peer = remember(transaction.peerContactId, peers) {
        transaction.peerContactId?.let { id -> peers.find { it.id == id } }
    }
    val goal = remember(transaction.goalId, goals) {
        transaction.goalId?.let { id -> goals.find { it.id == id } }
    }
    val transactionTags = remember(transaction.tagIds, tags) {
        if (transaction.tagIds.isNotBlank()) {
            transaction.tagIds.split(",").mapNotNull { idStr ->
                val id = idStr.trim().toLongOrNull()
                id?.let { tags.find { t -> t.id == it } }
            }
        } else emptyList()
    }

    val categoryColors = LocalCategoryColors.current
    val expenseColor = categoryColors.expense

    val title = when {
        transaction.description.isNotBlank() -> transaction.description
        category != null -> category.name
        else -> transaction.type.replaceFirstChar { it.uppercase() }
    }

    val typeIcon = when {
        transaction.isSplitParent -> "\uD83D\uDD00"
        transaction.isTransfer || transaction.type == "transfer" -> "\uE29C"
        category != null -> category.emoji
        else -> "\uD83D\uDCB8"
    }

    val amountText = if (transaction.type == "expense") {
        "-${CurrencyUtils.getCurrencyFormat(currency).format(transaction.amount)}"
    } else {
        CurrencyUtils.getCurrencyFormat(currency).format(transaction.amount)
    }

    val amountColor = when (transaction.type) {
        "income" -> MaterialTheme.colorScheme.primary
        "expense", "lend" -> expenseColor
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            icon = {
                Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            },
            title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently delete this transaction and adjust your account balance. This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete", color = Color.White) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // ── Header: Icon + Title ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(amountColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(typeIcon, fontSize = 22.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (category != null) category.name else transaction.type.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Hero Amount ──
            Text(
                text = amountText,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = amountColor,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(Modifier.height(20.dp))

            // ── Info Rows ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Date/Time
                InfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "${dateFormat.format(Date(transaction.date))} at ${timeFormat.format(Date(transaction.date))}"
                )

                // Transfer: From Account → To Account
                if (transaction.isTransfer || transaction.type == "transfer") {
                    val fromName = account?.name ?: "Account"
                    val toName = toAccount?.name ?: "Account"
                    IconTransferRow(
                        fromLabel = fromName,
                        toLabel = toName
                    )
                }

                // Lend/Borrow: Peer
                if (peer != null && (transaction.type == "lend" || transaction.type == "borrow")) {
                    InfoRow(
                        icon = Icons.Default.People,
                        label = peer.effectiveDisplayName
                    )
                }

                // Expected Return Date (lend/borrow)
                if (transaction.expectedReturnDate != null && (transaction.type == "lend" || transaction.type == "borrow")) {
                    InfoRow(
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        label = "Expected return: ${dateFormat.format(Date(transaction.expectedReturnDate))}"
                    )
                }

                // Category / Subcategory
                if (subCategory != null) {
                    InfoRow(
                        icon = Icons.Default.Category,
                        label = "${parentCategory?.name ?: category?.name ?: ""} \u203A ${subCategory.name}"
                    )
                } else if (category != null) {
                    InfoRow(
                        icon = Icons.Default.Category,
                        label = category.name
                    )
                }

                // Account
                if (account != null) {
                    IconInfoRow(icon = accountTypeIcon(account.type), label = account.name)
                }

                // Savings: Goal
                if (goal != null && transaction.type == "savings") {
                    InfoRow(
                        icon = Icons.Default.Flag,
                        label = goal.name
                    )
                }

                // Savings: Investment Platform
                if (transaction.investmentPlatform != null && transaction.type == "savings") {
                    InfoRow(
                        icon = Icons.AutoMirrored.Filled.TrendingUp,
                        label = transaction.investmentPlatform
                    )
                }

                // Note
                if (transaction.note.isNotBlank() && transaction.note != transaction.description) {
                    InfoRow(
                        icon = Icons.AutoMirrored.Filled.Notes,
                        label = transaction.note
                    )
                }

                // Tags
                if (transactionTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalOffer,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.width(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            transactionTags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                                ) {
                                    Text(
                                        text = tag.name,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Receipt/Attachment
                if (transaction.receiptPath != null) {
                    AttachmentPreviewSection(receiptPath = transaction.receiptPath)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Divider ──
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Spacer(Modifier.height(16.dp))

            // ── Action Buttons ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IconInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun IconTransferRow(fromLabel: String, toLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AccountBalance,
            null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = fromLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "\u2192",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = toLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AttachmentPreviewSection(receiptPath: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (receiptPath.startsWith("data:image")) {
                val bitmap = remember(receiptPath) {
                    runCatching {
                        val b64 = receiptPath.substringAfter("base64,")
                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Receipt",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Description,
                        null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = if (receiptPath.startsWith("data:image")) "Receipt Image" else "Attachment",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tap to view full size",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
