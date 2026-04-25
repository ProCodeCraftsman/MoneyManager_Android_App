package com.moneymanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
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
    var showAddDialog by remember { mutableStateOf(false) }
    var budgetToEdit by remember { mutableStateOf<BudgetWithSpending?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Budgets", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { /* Could open month picker */ }
                        ) {
                            IconButton(onClick = { viewModel.changeMonth(-1) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                            }
                            Text(
                                text = uiState.currentMonth,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { viewModel.changeMonth(1) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
                            }
                        }
                    }
                },
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
            FloatingActionButton(onClick = { showAddDialog = true }) {
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

                    val isInvestment = category?.type == "savings" || category?.type == "investment"
                    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
                    val percentage = if (budget.amount > 0) (spent / budget.amount * 100) else 0.0

                    val color = if (isInvestment) {
                        // Investment Targets logic
                        when {
                            percentage < 60 -> Color(0xFFF44336)  // Red
                            percentage < 100 -> Color(0xFFFFC107) // Amber
                            else -> Color(0xFF4CAF50)            // Green
                        }
                    } else {
                        // Spending Limits logic
                        when {
                            percentage < 80 -> Color(0xFF4CAF50)  // Green
                            percentage < 100 -> Color(0xFFFFC107) // Amber
                            else -> Color(0xFFF44336)            // Red
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { budgetToEdit = budgetWithSpending }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = category?.emoji ?: "📁", style = MaterialTheme.typography.titleMedium)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = category?.name ?: "Unknown",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isInvestment) {
                                        if (percentage >= 100) "Target met!" else "${percentage.toInt()}% of target"
                                    } else {
                                        if (percentage >= 100) "Over budget" else "${percentage.toInt()}% used"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = color
                                )
                                Text(
                                    text = "${currencyFormat.format(spent)} spent",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    if (showAddDialog) {
        BudgetDialog(
            categories = uiState.categories.filter { cat -> 
                uiState.budgetsWithSpending.none { it.budget.categoryId == cat.id } 
            },
            onDismiss = { showAddDialog = false },
            onSave = { categoryId, amount, autoCreateNextMonth ->
                viewModel.addBudget(categoryId, amount, autoCreateNextMonth)
                showAddDialog = false
            }
        )
    }

    if (budgetToEdit != null) {
        BudgetDialog(
            budget = budgetToEdit!!.budget,
            category = budgetToEdit!!.category,
            categories = uiState.categories,
            onDismiss = { budgetToEdit = null },
            onSave = { _, amount, _ ->
                viewModel.updateBudget(budgetToEdit!!.budget.copy(amount = amount))
                budgetToEdit = null
            },
            onDelete = {
                viewModel.deleteBudget(budgetToEdit!!.budget)
                budgetToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetDialog(
    budget: BudgetEntity? = null,
    category: CategoryEntity? = null,
    categories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (categoryId: Long, amount: Double, autoCreateNextMonth: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var amount by remember { mutableStateOf(budget?.amount?.toString() ?: "") }
    var selectedCategoryId by remember { mutableStateOf(budget?.categoryId ?: categories.firstOrNull()?.id) }
    var expanded by remember { mutableStateOf(false) }
    var autoCreateNextMonth by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (budget == null) "Set Budget" else "Edit Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (budget == null) {
                    Text("Select Category", style = MaterialTheme.typography.labelLarge)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        val selectedCategory = categories.find { it.id == selectedCategoryId }
                        OutlinedTextField(
                            value = selectedCategory?.let { "${it.emoji} ${it.name}" } ?: "Select Category",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text("${cat.emoji} ${cat.name}") },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Category: ${category?.emoji} ${category?.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Monthly Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (budget == null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { autoCreateNextMonth = !autoCreateNextMonth }
                    ) {
                        Checkbox(
                            checked = autoCreateNextMonth,
                            onCheckedChange = { autoCreateNextMonth = it }
                        )
                        Text(
                            text = "Auto-create in next month",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    selectedCategoryId?.let { onSave(it, amt, autoCreateNextMonth) }
                },
                enabled = amount.isNotBlank() && selectedCategoryId != null
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}