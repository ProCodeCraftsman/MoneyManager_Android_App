package com.moneymanager.app.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.moneymanager.app.ui.components.ScrollToTopBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.RecurringEntity
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    viewModel: RecurringViewModel,
    navController: NavController? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember(uiState.currencyCode) { 
        CurrencyUtils.getCurrencyFormat(uiState.currencyCode)
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<RecurringEntity?>(null) }
    val lazyListState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController?.navigate("recurring_form")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->
        ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.recurringList.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recurring transactions",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recurringList) { recurring ->
                        val category = uiState.categories.find { it.id == recurring.categoryId }
                        val account = uiState.accounts.find { it.id == recurring.accountId }
                        
                        RecurringRow(
                            recurring = recurring,
                            category = category,
                            account = account,
                            currencyFormat = currencyFormat,
                            dateFormat = dateFormat,
                            onToggleActive = {
                                coroutineScope.launch {
                                    viewModel.toggleActive(recurring)
                                }
                            },
                            onClick = {
                                navController?.navigate("recurring_form?recurringId=${recurring.id}")
                            },
                            onDelete = { showDeleteDialog = recurring }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        } // ScrollToTopBox
    }

    showDeleteDialog?.let { recurring ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Recurring") },
            text = { Text("Are you sure you want to delete this recurring transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteRecurring(recurring)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecurringRow(
    recurring: RecurringEntity,
    category: com.moneymanager.data.entity.CategoryEntity?,
    account: com.moneymanager.data.entity.AccountEntity?,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onToggleActive: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
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
                    .background(
                        when (recurring.type) {
                            "income" -> MaterialTheme.colorScheme.primaryContainer
                            "savings" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (category != null) {
                    com.moneymanager.app.ui.components.CategoryIcon(
                        emoji = category.emoji,
                        iconType = category.iconType,
                        colorIndex = category.colorIndex,
                        fontSize = 20.sp
                    )
                } else {
                    Icon(
                        imageVector = when (recurring.type) {
                            "income" -> Icons.AutoMirrored.Filled.TrendingUp
                            "savings" -> Icons.Default.Savings
                            else -> Icons.Default.AccountBalanceWallet
                        },
                        contentDescription = null,
                        tint = when (recurring.type) {
                            "income" -> MaterialTheme.colorScheme.onPrimaryContainer
                            "savings" -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recurring.note.ifEmpty { category?.name ?: "Recurring" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(recurring.frequency.replaceFirstChar { it.uppercase() })
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
                    text = currencyFormat.format(recurring.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (recurring.type == "income") MaterialTheme.colorScheme.primary
                            else if (recurring.type == "savings") MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Next: ${dateFormat.format(Date(recurring.nextDate))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Switch(
                checked = recurring.isActive,
                onCheckedChange = { onToggleActive() },
                modifier = Modifier.scale(0.7f)
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        HorizontalDivider(
            modifier = Modifier.padding(start = 54.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
