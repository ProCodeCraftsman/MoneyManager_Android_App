package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.TransactionFilterSheet
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    accountsViewModel: AccountsViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val accountsState by accountsViewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    var showAddDialog by remember { mutableStateOf(value = false) }
    var searchText by remember { mutableStateOf(value = "") }
    var showFilterSheet by remember { mutableStateOf(value = false) }

    // Calculate active filter count
    val activeFilterCount = listOfNotNull(
        uiState.filterType.takeIf { it.isNotEmpty() },
        uiState.filterAccountId,
        uiState.filterCategoryId,
        uiState.filterTagId,
        uiState.filterStartDate,
        uiState.filterEndDate
    ).size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                actions = {
                    BadgedBox(
                        badge = {
                            if (activeFilterCount > 0) {
                                Badge { Text(activeFilterCount.toString()) }
                            }
                        }
                    ) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Filter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchText,
                onValueChange = {
                    searchText = it
                    viewModel.setSearchQuery(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
                placeholder = { Text("Search transactions...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                            viewModel.setSearchQuery("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Active filter chips
            if (activeFilterCount > 0) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.filterType.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setTypeFilter("") },
                                label = { Text(uiState.filterType.replaceFirstChar { it.uppercase() }) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    uiState.filterAccountId?.let { accountId ->
                        item {
                            val accountName = accountsState.accounts.find { it.id == accountId }?.name ?: "Account"
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setAccountFilter(null) },
                                label = { Text(accountName) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    uiState.filterCategoryId?.let { categoryId ->
                        item {
                            val categoryName = uiState.allCategories.find { it.id == categoryId }?.name ?: "Category"
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setCategoryFilter(null) },
                                label = { Text(categoryName) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    uiState.filterTagId?.let { tagId ->
                        item {
                            val tag = uiState.allTags.find { it.id == tagId }
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setTagFilter(null) },
                                label = { Text(tag?.name ?: "Tag") },
                                leadingIcon = tag?.let {
                                    {
                                        Box(
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clip(CircleShape)
                                                .background(parseColor(it.color))
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    if (uiState.filterStartDate != null || uiState.filterEndDate != null) {
                        item {
                            val dateLabel = when {
                                uiState.filterStartDate != null && uiState.filterEndDate != null -> "Date Range"
                                uiState.filterStartDate != null -> "From Date"
                                else -> "To Date"
                            }
                            FilterChip(
                                selected = true,
                                onClick = { viewModel.setDateRangeFilter(null, null) },
                                label = { Text(dateLabel) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                    item {
                        TextButton(onClick = { viewModel.clearAllFilters() }) {
                            Text("Clear All")
                        }
                    }
                }
            }

            // Transaction list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (activeFilterCount > 0 || searchText.isNotEmpty()) 
                                    "No matching transactions" 
                                else "No transactions yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.transactions) { transaction ->
                        TransactionCard(
                            transaction = transaction,
                            tags = uiState.allTags,
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

    if (showAddDialog) {
        AddTransactionDialog(
            categories = uiState.allCategories,
            tags = uiState.allTags,
            accounts = accountsState.accounts,
            onDismiss = { showAddDialog = false }
        ) { transaction ->
            viewModel.addTransaction(transaction)
            showAddDialog = false
        }
    }

    if (showFilterSheet) {
        TransactionFilterSheet(
            accounts = accountsState.accounts,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            selectedType = uiState.filterType,
            selectedAccountId = uiState.filterAccountId,
            selectedCategoryId = uiState.filterCategoryId,
            selectedTagId = uiState.filterTagId,
            selectedStartDate = uiState.filterStartDate,
            selectedEndDate = uiState.filterEndDate,
            onTypeSelected = { viewModel.setTypeFilter(it) },
            onAccountSelected = { viewModel.setAccountFilter(it) },
            onCategorySelected = { viewModel.setCategoryFilter(it) },
            onTagSelected = { viewModel.setTagFilter(it) },
            onStartDateSelected = { start -> viewModel.setDateRangeFilter(start, uiState.filterEndDate) },
            onEndDateSelected = { end -> viewModel.setDateRangeFilter(uiState.filterStartDate, end) },
            onClearFilters = { viewModel.clearAllFilters() },
            onApply = { /* Filters applied via individual callbacks */ },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    tags: List<TagEntity>,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    // Parse tag IDs and find tag entities
    val transactionTags = if (transaction.tagIds.isNotEmpty()) {
        val tagIds = transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }
        tags.filter { it.id in tagIds }
    } else emptyList()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
            
            // Show tags
            if (transactionTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(transactionTags) { tag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.small
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(tag.color))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    accounts: List<AccountEntity>,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity) -> Unit
) {
    var type by remember { mutableStateOf("expense") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    // Separate parent and sub-categories
    val parentCategories = categories.filter { it.parentId == null }
    val subCategories = categories.filter { it.parentId != null }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Transaction") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Type selector
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("income", "expense", "savings").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                
                // Account selector
                ExposedDropdownMenuBox(
                    expanded = showAccountDropdown,
                    onExpandedChange = { showAccountDropdown = it }
                ) {
                    OutlinedTextField(
                        value = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showAccountDropdown,
                        onDismissRequest = { showAccountDropdown = false }
                    ) {
                        accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedAccountId = account.id
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }
                }
                
                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    singleLine = true
                )
                
                // Category selector
                ExposedDropdownMenuBox(
                    expanded = showCategoryDropdown,
                    onExpandedChange = { showCategoryDropdown = it }
                ) {
                    val selectedCategory = categories.find { it.id == selectedCategoryId }
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showCategoryDropdown,
                        onDismissRequest = { showCategoryDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedCategoryId = null
                                showCategoryDropdown = false
                            }
                        )
                        parentCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    showCategoryDropdown = false
                                }
                            )
                            // Show sub-categories indented
                            subCategories.filter { it.parentId == category.id }.forEach { subCategory ->
                                DropdownMenuItem(
                                    text = { Text("  ${subCategory.name}") },
                                    onClick = {
                                        selectedCategoryId = subCategory.id
                                        showCategoryDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note") },
                    singleLine = true
                )
                
                // Tags
                if (tags.isNotEmpty()) {
                    Text(
                        text = "Tags",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            FilterChip(
                                selected = tag.id in selectedTagIds,
                                onClick = {
                                    selectedTagIds = if (tag.id in selectedTagIds) {
                                        selectedTagIds - tag.id
                                    } else {
                                        selectedTagIds + tag.id
                                    }
                                },
                                label = { Text(tag.name) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(parseColor(tag.color))
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@TextButton
                    if (amt > 0 && selectedAccountId != null) {
                        onConfirm(
                            TransactionEntity(
                                accountId = selectedAccountId!!,
                                type = type,
                                amount = if (type == "expense") -amt else amt,
                                note = note,
                                categoryId = selectedCategoryId,
                                tagIds = selectedTagIds.joinToString(",")
                            )
                        )
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun parseColor(colorString: String): androidx.compose.ui.graphics.Color {
    return try {
        androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        androidx.compose.ui.graphics.Color.Gray
    }
}