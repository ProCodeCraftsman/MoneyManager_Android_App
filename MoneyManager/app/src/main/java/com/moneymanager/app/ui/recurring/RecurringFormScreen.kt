package com.moneymanager.app.ui.recurring

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.moneymanager.app.ui.components.SplitRowCard
import com.moneymanager.app.ui.dialogs.SplitRowData
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.domain.transaction.SplitTransactionFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormScreen(
    viewModel: RecurringViewModel,
    recurringId: Long? = null,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("expense") }
    var selectedAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedToAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var selectedSubCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var description by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedPeerId by remember { mutableStateOf<Long?>(null) }
    var selectedTagIds by remember { mutableStateOf(emptySet<Long>()) }
    var selectedPlatform by remember { mutableStateOf("") }
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }
    var selectedFrequency by remember { mutableStateOf("monthly") }
    var startDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableStateOf<Long?>(null) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf("0") }
    var receiptPath by remember { mutableStateOf<String?>(null) }
    var expectedReturnDate by remember { mutableStateOf<Long?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var existingRecurring by remember { mutableStateOf<RecurringEntity?>(null) }

    // ── Split state ──
    var splitEnabled by remember { mutableStateOf(false) }
    var splitRows by remember { mutableStateOf(listOf(SplitRowData(0), SplitRowData(1))) }
    var splitIdCounter by remember { mutableStateOf(2) }
    var openSplitDropdownIndex by remember { mutableStateOf<Int?>(null) }
    val splitTotal = remember(splitRows) { splitRows.sumOf { it.amount.toDoubleOrNull() ?: 0.0 } }
    val splitRemaining = remember(splitTotal, amount) { (amount.toDoubleOrNull() ?: 0.0) - splitTotal }
    val splitAllowedForType = selectedType == "expense" || selectedType == "income" || selectedType == "savings"

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val isEditing = recurringId != null

    var isDataLoaded by rememberSaveable(recurringId) { mutableStateOf(false) }

    // Filter accounts based on type
    val filteredAccounts = remember(selectedType, uiState.accounts) {
        when (selectedType) {
            "expense", "savings" -> uiState.accounts.filter { it.type != "savings" }
            else -> uiState.accounts
        }
    }

    LaunchedEffect(recurringId, uiState.accounts, uiState.categories, uiState.tags, uiState.peers) {
        if (recurringId != null && !isDataLoaded && uiState.accounts.isNotEmpty() && uiState.categories.isNotEmpty()) {
            viewModel.getRecurringById(recurringId)?.let { recurring ->
                existingRecurring = recurring
                amount = if (recurring.amount % 1.0 == 0.0) recurring.amount.toLong().toString() else recurring.amount.toString()
                selectedType = recurring.type
                selectedAccount = uiState.accounts.find { it.id == recurring.accountId }
                selectedToAccount = uiState.accounts.find { it.id == recurring.toAccountId }
                selectedCategory = uiState.categories.find { it.id == recurring.categoryId }
                selectedSubCategory = uiState.categories.find { it.id == recurring.subCategoryId }
                selectedGoalId = recurring.goalId
                description = recurring.description
                note = recurring.note
                selectedPeerId = recurring.peerContactId
                selectedTagIds = if (recurring.tagIds.isNotEmpty())
                    recurring.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
                    else emptySet()
                selectedPlatform = recurring.investmentPlatform ?: ""
                selectedFrequency = recurring.frequency
                startDate = recurring.nextDate
                endDate = recurring.endDate
                reminderEnabled = recurring.reminderEnabled
                reminderDays = recurring.reminderDays.toString()
                receiptPath = recurring.receiptPath
                expectedReturnDate = recurring.expectedReturnDate
                if (!recurring.splitData.isNullOrBlank()) {
                    val decoded = SplitTransactionFactory.decodeSplitTemplate(recurring.splitData)
                    if (decoded.isNotEmpty()) {
                        splitEnabled = true
                        splitRows = decoded
                        splitIdCounter = decoded.size
                    }
                }
                isDataLoaded = true
            }
        }
    }

    val receiptPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { receiptPath = FileHelper.saveReceipt(context, it) }
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
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("income", "expense", "savings", "transfer", "lend", "borrow").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = {
                            selectedType = type
                            selectedCategory = null
                            selectedSubCategory = null
                            if (type != "transfer") selectedToAccount = null
                            if (type != "lend" && type != "borrow") selectedPeerId = null
                        },
                        label = { Text(type.replaceFirstChar { it.uppercase() }) }
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
                    filteredAccounts.forEach { account ->
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

            // Transfer To Account
            if (selectedType == "transfer") {
                var toAccountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = toAccountExpanded,
                    onExpandedChange = { toAccountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedToAccount?.name ?: "Select Destination Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = toAccountExpanded,
                        onDismissRequest = { toAccountExpanded = false }
                    ) {
                        uiState.accounts.filter { it.id != selectedAccount?.id }.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    selectedToAccount = account
                                    toAccountExpanded = false
                                }
                            )
                        }
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

            // Split (expense/income/savings only)
            if (splitAllowedForType) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split into multiple categories", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = splitEnabled,
                        onCheckedChange = { splitEnabled = it }
                    )
                }

                if (splitEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        splitRows.forEachIndexed { index, row ->
                            key(row.localId) {
                                SplitRowCard(
                                    row = row,
                                    allCategories = uiState.categories,
                                    type = selectedType,
                                    onUpdate = { updated -> splitRows = splitRows.toMutableList().also { it[index] = updated } },
                                    onRemove = { splitRows = splitRows.toMutableList().also { it.removeAt(index) } },
                                    showDropdown = openSplitDropdownIndex == index,
                                    onToggleDropdown = {
                                        openSplitDropdownIndex = if (openSplitDropdownIndex == index) null else index
                                    },
                                    remainingAmount = splitRemaining,
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { splitRows = splitRows + SplitRowData(splitIdCounter++) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Split Category")
                    }
                    Text(
                        if (abs(splitRemaining) < 0.01) "Fully allocated" else "Unallocated: ${"%.2f".format(Locale.US, splitRemaining)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note") },
                modifier = Modifier.fillMaxWidth()
            )

            // Receipt / attachment
            Text("Attachment (Optional)", style = MaterialTheme.typography.titleSmall)
            if (receiptPath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = receiptPath,
                        contentDescription = "Attached receipt",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    OutlinedButton(onClick = { receiptPath = null }) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Remove")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = { receiptPicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Attach Receipt / Image")
                }
            }

            // Peer (Lend/Borrow)
            if (selectedType == "lend" || selectedType == "borrow") {
                var peerExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = peerExpanded,
                    onExpandedChange = { peerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.peers.find { it.id == selectedPeerId }?.effectiveDisplayName ?: "Select Person",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Person") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = peerExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = peerExpanded,
                        onDismissRequest = { peerExpanded = false }
                    ) {
                        uiState.peers.forEach { peer ->
                            DropdownMenuItem(
                                text = { Text(peer.effectiveDisplayName) },
                                onClick = {
                                    selectedPeerId = peer.id
                                    peerExpanded = false
                                }
                            )
                        }
                    }
                }

                // Expected return / due date
                var showReturnDatePicker by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = expectedReturnDate?.let { dateFormat.format(Date(it)) } ?: "No Due Date",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Due Date (Optional)") },
                        modifier = Modifier.weight(1f).clickable { showReturnDatePicker = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = if (expectedReturnDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    if (expectedReturnDate != null) {
                        IconButton(onClick = { expectedReturnDate = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Due Date")
                        }
                    }
                }
                if (showReturnDatePicker) {
                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expectedReturnDate ?: startDate)
                    DatePickerDialog(
                        onDismissRequest = { showReturnDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { expectedReturnDate = it }
                                showReturnDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showReturnDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }
            }

            // Investment Platform (Savings)
            if (selectedType == "savings") {
                OutlinedTextField(
                    value = selectedPlatform,
                    onValueChange = { selectedPlatform = it },
                    label = { Text("Investment Platform") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Goal
            if (uiState.goals.isNotEmpty()) {
                var goalExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = goalExpanded,
                    onExpandedChange = { goalExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.goals.find { it.id == selectedGoalId }?.name ?: "Select Goal (Optional)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Goal") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = goalExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = goalExpanded,
                        onDismissRequest = { goalExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None") },
                            onClick = {
                                selectedGoalId = null
                                goalExpanded = false
                            }
                        )
                        uiState.goals.forEach { goal ->
                            DropdownMenuItem(
                                text = { Text(goal.name) },
                                onClick = {
                                    selectedGoalId = goal.id
                                    goalExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Tags
            if (uiState.tags.isNotEmpty()) {
                Text("Tags", style = MaterialTheme.typography.titleSmall)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    uiState.tags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedTagIds = if (isSelected) selectedTagIds - tag.id else selectedTagIds + tag.id
                            },
                            label = { Text(tag.name) }
                        )
                    }
                }
            }

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

            if (reminderEnabled) {
                OutlinedTextField(
                    value = reminderDays,
                    onValueChange = { reminderDays = it.filter { c -> c.isDigit() } },
                    label = { Text("Remind me this many days before") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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

                    val useSplit = splitAllowedForType && splitEnabled
                    var splitDataToSave: String? = null
                    if (useSplit) {
                        val activeRows = splitRows.filter { (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
                        if (activeRows.size < 2) {
                            Toast.makeText(context, "Split requires at least 2 split items", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (activeRows.any { it.categoryId == null }) {
                            Toast.makeText(context, "Please select a category for each split item", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (abs(splitRemaining) >= 0.01) {
                            Toast.makeText(context, "Split total must match the amount (Unallocated: %.2f)".format(Locale.US, splitRemaining), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        splitDataToSave = SplitTransactionFactory.encodeSplitTemplate(activeRows)
                    }

                    isLoading = true
                    coroutineScope.launch {
                        val recurring = RecurringEntity(
                            id = existingRecurring?.id ?: 0,
                            accountId = selectedAccount!!.id,
                            toAccountId = selectedToAccount?.id,
                            type = selectedType,
                            amount = amountValue,
                            categoryId = selectedCategory?.id,
                            subCategoryId = selectedSubCategory?.id,
                            peerContactId = selectedPeerId,
                            tagIds = selectedTagIds.joinToString(","),
                            description = description,
                            note = note,
                            goalId = selectedGoalId,
                            investmentPlatform = selectedPlatform.ifEmpty { null },
                            frequency = selectedFrequency,
                            nextDate = startDate,
                            endDate = endDate,
                            isActive = existingRecurring?.isActive ?: true,
                            reminderEnabled = reminderEnabled,
                            reminderDays = reminderDays.toIntOrNull() ?: 0,
                            receiptPath = receiptPath,
                            expectedReturnDate = if (selectedType == "lend" || selectedType == "borrow") expectedReturnDate else null,
                            splitData = splitDataToSave,
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
