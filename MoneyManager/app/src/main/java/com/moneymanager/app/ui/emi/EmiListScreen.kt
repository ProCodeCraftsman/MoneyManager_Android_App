package com.moneymanager.app.ui.emi

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.components.ScrollToTopBox
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.EmiEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiListScreen(
    viewModel: EmiViewModel,
    onNavigateBack: () -> Unit,
    onEmiClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember(uiState.currencyCode) {
        CurrencyUtils.getCurrencyFormat(uiState.currencyCode)
    }
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EMIs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (uiState.emis.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No EMIs yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.emis, key = { it.id }) { emi ->
                        val category = uiState.categories.find { it.id == emi.categoryId }
                        val account = uiState.accounts.find { it.id == emi.accountId }

                        EmiRow(
                            emi = emi,
                            category = category,
                            account = account,
                            currencyFormat = currencyFormat,
                            onClick = { onEmiClick(emi.id) }
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun EmiRow(
    emi: EmiEntity,
    category: CategoryEntity?,
    account: AccountEntity?,
    currencyFormat: java.text.NumberFormat,
    onClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.Center
            ) {
                if (category != null) {
                    CategoryIcon(
                        emoji = category.emoji,
                        iconType = category.iconType,
                        colorIndex = category.colorIndex,
                        fontSize = 20.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = emi.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("${emi.tenureMonths} months")
                        if (account != null) {
                            append(" • ")
                            append(account.name)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currencyFormat.format(emi.monthlyAmount) + "/mo",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
                StatusChip(status = emi.status)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 54.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
fun StatusChip(status: String) {
    val (label, color) = when (status) {
        "COMPLETED" -> "Completed" to MaterialTheme.colorScheme.primary
        "FORECLOSED" -> "Foreclosed" to MaterialTheme.colorScheme.tertiary
        else -> "Active" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color
    )
}
