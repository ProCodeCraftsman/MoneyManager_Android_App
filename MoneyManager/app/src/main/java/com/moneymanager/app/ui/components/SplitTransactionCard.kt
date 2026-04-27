package com.moneymanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.constants.COLOR_EXPENSE
import com.moneymanager.app.ui.constants.ICON_SPLIT
import com.moneymanager.app.ui.constants.TILE_CORNER_RADIUS
import com.moneymanager.app.ui.constants.TILE_ELEVATION
import com.moneymanager.app.ui.constants.TILE_ICON_CORNER_RADIUS
import com.moneymanager.app.ui.constants.TILE_ICON_SIZE
import com.moneymanager.app.ui.constants.TILE_PADDING_HORIZONTAL
import com.moneymanager.app.ui.constants.TILE_PADDING_INNER
import com.moneymanager.app.ui.constants.TILE_SPACING_VERTICAL
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat

@Composable
fun SplitTransactionCard(
    parentTransaction: TransactionEntity,
    splitChildren: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    val isCurrentlyExpanded = isExpanded

    Column {
        Surface(
            onClick = onToggleExpand,
            shape = RoundedCornerShape(TILE_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = TILE_ELEVATION,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TILE_PADDING_HORIZONTAL)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TILE_PADDING_INNER),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(TILE_ICON_SIZE)
                        .clip(RoundedCornerShape(TILE_ICON_CORNER_RADIUS))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    Alignment.Center
                ) {
                    Text(ICON_SPLIT, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Split Transaction",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${splitChildren.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(parentTransaction.amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (parentTransaction.type) {
                            "income", "receive" -> MaterialTheme.colorScheme.secondary
                            "expense", "lend" -> COLOR_EXPENSE
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Icon(
                        imageVector = if (isCurrentlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCurrentlyExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (isCurrentlyExpanded) {
            Column(
                modifier = Modifier.padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(TILE_SPACING_VERTICAL)
            ) {
                splitChildren.forEach { child ->
                    TransactionCardDense(
                        transaction = child,
                        accounts = accounts,
                        categories = categories,
                        peers = peers,
                        currencyFormat = currencyFormat,
                        onClick = { onEdit(child) }
                    )
                }
            }
        }

        Spacer(Modifier.height(TILE_SPACING_VERTICAL))
    }
}