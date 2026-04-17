package com.moneymanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.CurrencyUtils
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: BudgetsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember(uiState.currencyCode) { 
        CurrencyUtils.getCurrencyFormat(uiState.currencyCode) 
    }
    val showAddDialog = remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(onClick = { showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Budget")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Monthly Budget - ${uiState.currentMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.budgetsWithSpending.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No budgets set this month",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.budgetsWithSpending) { budgetWithSpending ->
                    val budget = budgetWithSpending.budget
                    val category = budgetWithSpending.category
                    val spent = budgetWithSpending.spent

                    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                    val percentage = if (budget.amount > 0) (spent / budget.amount * 100) else 0.0

                    val color = when {
                        percentage < 80 -> Color(0xFF4CAF50)  // Green
                        percentage < 100 -> Color(0xFFFFC107)  // Amber
                        else -> Color(0xFFF44336)            // Red
                    }

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = category?.name ?: "Unknown",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currencyFormat.format(budget.amount),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = color,
                                trackColor = color.copy(alpha = 0.2f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    percentage < 100 -> "${percentage.toInt()}% used"
                                    else -> "${percentage.toInt()}% over budget"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = color
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog.value) {
        AlertDialog(
            onDismissRequest = { showAddDialog.value = false },
            title = { Text("Set Budget") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        label = { Text("Amount") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog.value = false }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog.value = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}