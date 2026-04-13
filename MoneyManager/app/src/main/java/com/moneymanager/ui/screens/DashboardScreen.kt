package com.moneymanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.BudgetWidget
import com.moneymanager.app.ui.components.CategoryDrilldownPanel
import com.moneymanager.app.ui.components.ExpensePieChart
import com.moneymanager.app.ui.components.RemindersWidget
import com.moneymanager.app.ui.components.TimeFilterBar
import com.moneymanager.app.ui.components.TransferDialog
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            showTransferDialog = true
                            showFabMenu = false
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    TimeFilterBar(
                        selectedFilter = uiState.selectedFilter,
                        customStartDate = uiState.customStartDate,
                        customEndDate = uiState.customEndDate,
                        onFilterSelected = { viewModel.setTimeFilter(it) },
                        onCustomDateRangeSelected = { start, end -> viewModel.setCustomDateRange(start, end) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Net Worth",
                            value = currencyFormat.format(uiState.netWorth),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Income",
                            value = currencyFormat.format(uiState.totalIncome),
                            valueColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Expenses",
                            value = currencyFormat.format(uiState.totalExpense),
                            valueColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Net",
                            value = currencyFormat.format(uiState.totalIncome - uiState.totalExpense),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (uiState.expenseBreakdown.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Expense Breakdown",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ExpensePieChart(
                                    entries = uiState.expenseBreakdown,
                                    currencyFormat = currencyFormat,
                                    onCategoryClick = { entry -> viewModel.selectCategory(entry) }
                                )
                            }
                        }
                    }
                }

                if (uiState.budgetsWithProgress.isNotEmpty()) {
                    item {
                        BudgetWidget(
                            budgetsWithProgress = uiState.budgetsWithProgress,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                if (uiState.upcomingRecurring.isNotEmpty()) {
                    item {
                        RemindersWidget(
                            upcomingRecurring = uiState.upcomingRecurring,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentTransactions) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            dateFormat = dateFormat,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, toId, amount, note ->
                viewModel.transferMoney(fromId, toId, amount, note)
                showTransferDialog = false
            }
        )
    }

    val selectedCategory = uiState.selectedCategory
    if (selectedCategory != null) {
        CategoryDrilldownPanel(
            categoryName = selectedCategory.label,
            categoryColor = selectedCategory.color,
            transactions = uiState.categoryTransactions,
            dateFormat = dateFormat,
            currencyFormat = currencyFormat,
            onDismiss = { viewModel.selectCategory(null) }
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifEmpty { transaction.type.replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (transaction.type) {
                    "income" -> "+${currencyFormat.format(transaction.amount)}"
                    "expense" -> "-${currencyFormat.format(transaction.amount)}"
                    else -> currencyFormat.format(transaction.amount)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    "income" -> MaterialTheme.colorScheme.secondary
                    "expense" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}