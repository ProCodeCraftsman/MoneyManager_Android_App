package com.moneymanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterSheet(
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    selectedType: String,
    selectedAccountId: Long?,
    selectedCategoryId: Long?,
    selectedTagId: Long?,
    selectedStartDate: Long?,
    selectedEndDate: Long?,
    onTypeSelected: (String) -> Unit,
    onAccountSelected: (Long?) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onTagSelected: (Long?) -> Unit,
    onStartDateSelected: (Long?) -> Unit,
    onEndDateSelected: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentType by remember { mutableStateOf(selectedType) }
    var currentAccountId by remember { mutableStateOf(selectedAccountId) }
    var currentCategoryId by remember { mutableStateOf(selectedCategoryId) }
    var currentTagId by remember { mutableStateOf(selectedTagId) }
    var currentStartDate by remember { mutableStateOf(selectedStartDate) }
    var currentEndDate by remember { mutableStateOf(selectedEndDate) }

    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Transactions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = {
                    currentType = ""
                    currentAccountId = null
                    currentCategoryId = null
                    currentTagId = null
                    currentStartDate = null
                    currentEndDate = null
                    onClearFilters()
                }) {
                    Text("Clear All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type filter
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val types = listOf("" to "All", "income" to "Income", "expense" to "Expense", "savings" to "Savings", "transfer" to "Transfer")
                items(types) { (type, label) ->
                    FilterChip(
                        selected = currentType == type,
                        onClick = {
                            currentType = type
                            onTypeSelected(type)
                        },
                        label = { Text(label) },
                        leadingIcon = if (currentType == type) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Account filter
            Text(
                text = "Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = showAccountDropdown,
                onExpandedChange = { showAccountDropdown = it }
            ) {
                OutlinedTextField(
                    value = accounts.find { it.id == currentAccountId }?.name ?: "All Accounts",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showAccountDropdown) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = showAccountDropdown,
                    onDismissRequest = { showAccountDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All Accounts") },
                        onClick = {
                            currentAccountId = null
                            onAccountSelected(null)
                            showAccountDropdown = false
                        }
                    )
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                currentAccountId = account.id
                                onAccountSelected(account.id)
                                showAccountDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category filter
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                val selectedCategory = categories.find { it.id == currentCategoryId }
                OutlinedTextField(
                    value = selectedCategory?.name ?: "All Categories",
                    onValueChange = {},
                    readOnly = true,
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
                        text = { Text("All Categories") },
                        onClick = {
                            currentCategoryId = null
                            onCategorySelected(null)
                            showCategoryDropdown = false
                        }
                    )
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    if (category.parentId != null) "  ${category.name}" 
                                    else category.name
                                ) 
                            },
                            onClick = {
                                currentCategoryId = category.id
                                onCategorySelected(category.id)
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tag filter
            Text(
                text = "Tag",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = currentTagId == null,
                        onClick = {
                            currentTagId = null
                            onTagSelected(null)
                        },
                        label = { Text("All") }
                    )
                }
                items(tags) { tag ->
                    FilterChip(
                        selected = currentTagId == tag.id,
                        onClick = {
                            currentTagId = tag.id
                            onTagSelected(tag.id)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Date range filter
            Text(
                text = "Date Range",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentStartDate?.let { formatDate(it) } ?: "Start Date",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showStartDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { 
                            currentStartDate = null
                            onStartDateSelected(null)
                        }) {
                            if (currentStartDate != null) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
                OutlinedTextField(
                    value = currentEndDate?.let { formatDate(it) } ?: "End Date",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showEndDatePicker = true },
                    trailingIcon = {
                        IconButton(onClick = { 
                            currentEndDate = null
                            onEndDateSelected(null)
                        }) {
                            if (currentEndDate != null) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply button
            Button(
                onClick = {
                    onApply()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Filters")
            }
        }
    }

    // Date pickers
    if (showStartDatePicker) {
        DatePickerDialog(
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { date ->
                currentStartDate = date
                onStartDateSelected(date)
                showStartDatePicker = false
            },
            initialDate = currentStartDate
        )
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { date ->
                currentEndDate = date
                onEndDateSelected(date)
                showEndDatePicker = false
            },
            initialDate = currentEndDate
        )
    }
}

@Composable
private fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    initialDate: Long?
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate ?: System.currentTimeMillis()
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { onDateSelected(it) }
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            DatePicker(state = datePickerState)
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun parseColor(colorString: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}