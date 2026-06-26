package com.moneymanager.app.ui.recurring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.RecurringEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormScreen(
    viewModel: RecurringViewModel,
    recurringId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    
    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("expense") }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedSubCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var note by remember { mutableStateOf("") }
    var selectedFrequency by remember { mutableStateOf("monthly") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var existingRecurring by remember { mutableStateOf<RecurringEntity?>(null) }
    
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val isEditing = recurringId != null
    
    LaunchedEffect(recurringId) {
        if (recurringId != null) {
            viewModel.getRecurringById(recurringId)?.let { recurring ->
                existingRecurring = recurring
                amount = recurring.amount.toString()
                selectedType = recurring.type
                selectedAccount = uiState.accounts.find { it.id == recurring.accountId }
                selectedCategory = uiState.categories.find { it.id == recurring.categoryId }
                selectedSubCategory = uiState.categories.find { it.id == recurring.subCategoryId }
                note = recurring.note
                selectedFrequency = recurring.frequency
                startDate = recurring.nextDate
                endDate = recurring.endDate
                reminderEnabled = recurring.reminderEnabled
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditing) "Edit Recurring" else "New Recurring",
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            
            // Type selection
            Text("Type", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("income", "expense", "savings").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { 
                            selectedType = type
                            selectedCategory = null
                            selectedSubCategory = null
                        },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Account dropdown
            var accountExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Account") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                selectedAccount = account
                                accountExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Category dropdown
            val mainCategories = uiState.categories.filter { it.type == selectedType && it.parentId == null }
            if (mainCategories.isNotEmpty()) {
                var categoryExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category (Optional)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        mainCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    selectedSubCategory = null // Reset sub-category
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                // Sub-category dropdown
                if (selectedCategory != null) {
                    val subCategories = uiState.categories.filter { it.parentId == selectedCategory?.id }
                    if (subCategories.isNotEmpty()) {
                        var subCategoryExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = subCategoryExpanded,
                            onExpandedChange = { subCategoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedSubCategory?.name ?: "Select Sub-Category",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sub-Category (Optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subCategoryExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = subCategoryExpanded,
                                onDismissRequest = { subCategoryExpanded = false }
                            ) {
                                subCategories.forEach { subCat ->
                                    DropdownMenuItem(
                                        text = { Text(subCat.name) },
                                        onClick = {
                                            selectedSubCategory = subCat
                                            subCategoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )
            
            // Frequency dropdown
            var frequencyExpanded by remember { mutableStateOf(false) }
            val frequencies = listOf("daily", "weekly", "biweekly", "monthly", "yearly")
            ExposedDropdownMenuBox(
                expanded = frequencyExpanded,
                onExpandedChange = { frequencyExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedFrequency.replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Frequency") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = frequencyExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = frequencyExpanded,
                    onDismissRequest = { frequencyExpanded = false }
                ) {
                    frequencies.forEach { frequency ->
                        DropdownMenuItem(
                            text = { Text(frequency.replaceFirstChar { it.uppercase() }) },
                            onClick = {
                                selectedFrequency = frequency
                                frequencyExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Start date display
            var showStartDatePicker by remember { mutableStateOf(false) }
            OutlinedTextField(
                value = dateFormat.format(Date(startDate)),
                onValueChange = {},
                readOnly = true,
                label = { Text("Start Date") },
                modifier = Modifier.fillMaxWidth().clickable { showStartDatePicker = true },
                enabled = false, // Use clickable instead to show dialog
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            if (showStartDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = startDate)
                DatePickerDialog(
                    onDismissRequest = { showStartDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { startDate = it }
                            showStartDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            // End date selection
            var showEndDatePicker by remember { mutableStateOf(false) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = endDate?.let { dateFormat.format(Date(it)) } ?: "No End Date",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("End Date (Optional)") },
                    modifier = Modifier.weight(1f).clickable { showEndDatePicker = true },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (endDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                if (endDate != null) {
                    IconButton(onClick = { endDate = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear End Date")
                    }
                }
            }

            if (showEndDatePicker) {
                val datePickerState = rememberDatePickerState(initialSelectedDateMillis = endDate ?: (startDate + 86400000))
                DatePickerDialog(
                    onDismissRequest = { showEndDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { endDate = it }
                            showEndDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            
            // Reminder toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Reminder")
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { reminderEnabled = it }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    if (amountValue == null || selectedAccount == null) {
                        return@Button
                    }
                    
                    isLoading = true
                    coroutineScope.launch {
                        val recurring = RecurringEntity(
                            id = existingRecurring?.id ?: 0,
                            accountId = selectedAccount!!.id,
                            type = selectedType,
                            amount = amountValue,
                            categoryId = selectedCategory?.id,
                            subCategoryId = selectedSubCategory?.id,
                            note = note,
                            frequency = selectedFrequency,
                            nextDate = startDate,
                            endDate = endDate,
                            isActive = existingRecurring?.isActive ?: true,
                            reminderEnabled = reminderEnabled,
                            createdAt = existingRecurring?.createdAt ?: System.currentTimeMillis()
                        )
                        viewModel.saveRecurring(recurring)
                        isLoading = false
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.isNotBlank() && selectedAccount != null && !isLoading
            ) {
                Text(if (isLoading) "Saving..." else "Save Recurring")
            }
            
            // Delete button for editing
            if (isEditing && existingRecurring != null) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            existingRecurring?.let { viewModel.deleteRecurring(it) }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            }
        }
    }
}
