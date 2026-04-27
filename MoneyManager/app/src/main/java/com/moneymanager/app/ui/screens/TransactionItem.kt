package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.moneymanager.app.ui.components.TransactionCardDense
import com.moneymanager.app.ui.constants.TILE_SPACING_VERTICAL
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    showDeleteConfirm = true
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit(transaction)
                    false
                }
                else -> false
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
            },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(transaction)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        Column {
            TransactionCardDense(
                transaction = transaction,
                accounts = accounts,
                categories = categories,
                peers = peers,
                currencyFormat = currencyFormat,
                onClick = { onEdit(transaction) }
            )
            Spacer(Modifier.height(TILE_SPACING_VERTICAL))
        }
    }
}