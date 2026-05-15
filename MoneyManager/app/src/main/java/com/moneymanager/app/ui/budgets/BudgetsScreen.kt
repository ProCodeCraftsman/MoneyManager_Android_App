package com.moneymanager.app.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.components.ScrollToTopBox
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity

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
    val lazyListState = rememberLazyListState()

    val displayMonth = remember(uiState.currentMonth) {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
            val date = sdf.parse(uiState.currentMonth)
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()).format(date!!)
        } catch (e: Exception) {
            uiState.currentMonth
        }
    }

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
                },
                actions = {
                    IconButton(onClick = { viewModel.changeMonth(-1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev")
                    }
                    Text(
                        text = displayMonth,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.changeMonth(1) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
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
        if (uiState.budgetsWithSpending.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No budgets yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Set budgets to track your spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(uiState.budgetsWithSpending) { budgetWithSpending ->
                        BudgetListItem(
                            budgetWithSpending = budgetWithSpending,
                            currencyFormat = currencyFormat,
                            onClick = { budgetToEdit = budgetWithSpending }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
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

@Composable
private fun BudgetListItem(
    budgetWithSpending: BudgetWithSpending,
    currencyFormat: java.text.NumberFormat,
    onClick: () -> Unit
) {
    val budget = budgetWithSpending.budget
    val category = budgetWithSpending.category
    val spent = budgetWithSpending.spent

    val isInvestment = category?.type == "savings" || category?.type == "investment"
    val progress = if (budget.amount > 0) (spent / budget.amount).toFloat().coerceIn(0f, 1f) else 0f
    val percentage = if (budget.amount > 0) (spent / budget.amount * 100) else 0.0

    val color = if (isInvestment) {
        when {
            percentage < 60 -> Color(0xFFF44336)
            percentage < 100 -> Color(0xFFFFC107)
            else -> Color(0xFF4CAF50)
        }
    } else {
        when {
            percentage < 80 -> Color(0xFF4CAF50)
            percentage < 100 -> Color(0xFFFFC107)
            else -> Color(0xFFF44336)
        }
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    emoji = category?.emoji ?: "📁",
                    iconType = category?.iconType ?: "emoji",
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category?.name ?: "Unknown",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isInvestment) {
                        if (percentage >= 100) "Target met!" else "${percentage.toInt()}% of target"
                    } else {
                        if (percentage >= 100) "Over budget!" else "${percentage.toInt()}% used"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currencyFormat.format(budget.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 20.dp),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 50.dp, end = 20.dp, top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${currencyFormat.format(spent)} spent",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(start = 50.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (budget == null) {
                    Text("Select Category", style = MaterialTheme.typography.labelLarge)
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        val selectedCategory = categories.find { it.id == selectedCategoryId }
                        OutlinedTextField(
                            value = selectedCategory?.let { it.name } ?: "Select Category",
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
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, fontSize = 16.sp)
                                            Text(cat.name)
                                        }
                                    },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Category:", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        CategoryIcon(emoji = category?.emoji ?: "📁", iconType = category?.iconType ?: "emoji", fontSize = 18.sp)
                        Text(category?.name ?: "", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    }
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
