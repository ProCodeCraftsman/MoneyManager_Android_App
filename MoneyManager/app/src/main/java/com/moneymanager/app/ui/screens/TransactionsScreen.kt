package com.moneymanager.app.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.moneymanager.app.ui.util.parseColor
import com.moneymanager.app.ui.util.CurrencyUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.TransactionFilterSheet
import com.moneymanager.app.ui.components.TransferDialog
import com.moneymanager.data.entity.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions

// Investment platforms for savings transactions
private val INVESTMENT_PLATFORMS = listOf(
    "Zerodha", "Groww", "Kite", "IndMoney", "Paytm Money",
    "ET Money", "Coin by Zerodha", "Angel One", "Upstox",
    "HDFC Securities", "ICICI Direct", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    initialType: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember(uiState.currency) {
        CurrencyUtils.getCurrencyFormat(uiState.currency)
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }
    var showAddDialog by remember { mutableStateOf(initialType != null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var preselectedType by remember { mutableStateOf(initialType) }

    LaunchedEffect(showAddDialog) {
        if (!showAddDialog) preselectedType = null
    }

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
                    // Transfer button
                    IconButton(onClick = { showTransferDialog = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer")
                    }
                    // Filter button with badge
                    BadgedBox(badge = {
                        if (activeFilterCount > 0) Badge { Text(activeFilterCount.toString()) }
                    }) {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
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
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Search bar
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it; viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
                placeholder = { Text("Search transactions…") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = { searchText = ""; viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // Active filter chips
            if (activeFilterCount > 0) {
                LazyRow(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (uiState.filterType.isNotEmpty()) item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setTypeFilter("") },
                            label = { Text(uiState.filterType.replaceFirstChar { it.uppercase() }) },
                            trailingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) }
                        )
                    }
                    uiState.filterAccountId?.let { id -> item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setAccountFilter(null) },
                            label = { Text(uiState.allAccounts.find { it.id == id }?.name ?: "Account") },
                            trailingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) }
                        )
                    }}
                    uiState.filterCategoryId?.let { id -> item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text(uiState.allCategories.find { it.id == id }?.name ?: "Category") },
                            trailingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) }
                        )
                    }}
                    uiState.filterTagId?.let { id -> item {
                        val tag = uiState.allTags.find { it.id == id }
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setTagFilter(null) },
                            label = { Text(tag?.name ?: "Tag") },
                            trailingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) }
                        )
                    }}
                    if (uiState.filterStartDate != null || uiState.filterEndDate != null) item {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setDateRangeFilter(null, null) },
                            label = { Text("Date Range") },
                            trailingIcon = { Icon(Icons.Default.Clear, null, Modifier.size(16.dp)) }
                        )
                    }
                    item { TextButton(onClick = { viewModel.clearAllFilters() }) { Text("Clear All") } }
                }
            }

            // Transaction list
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 0.dp, top = 8.dp, end = 0.dp, bottom = 80.dp)
            ) {
                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                            Text(
                                if (activeFilterCount > 0 || searchText.isNotEmpty()) "No matching transactions"
                                else "No transactions yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.transactions, key = { it.id }) { tx ->
                        TransactionCard(
                            transaction = tx,
                            tags = uiState.allTags,
                            accounts = uiState.allAccounts,
                            categories = uiState.allCategories,
                            dateFormat = dateFormat,
                            currencyFormat = currencyFormat,
                            onEdit = { editingTransaction = it },
                            onDuplicate = { viewModel.duplicateTransaction(it) },
                            onDelete = { viewModel.deleteTransaction(it) }
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        AddEditTransactionDialog(
            transaction = null,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            accounts = uiState.allAccounts,
            goals = uiState.allGoals,
            initialType = preselectedType,
            onDismiss = { showAddDialog = false },
            onConfirm = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children)
                else viewModel.addTransaction(tx)
                showAddDialog = false
            },
            onSaveAndDuplicate = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children)
                else viewModel.addTransaction(tx)
                preselectedType = tx.type
            },
            onCreateTag = { name -> viewModel.createTag(name) }
        )
    }

    // Edit dialog
    editingTransaction?.let { editing ->
        val splitChildren by if (editing.isSplitParent) {
            viewModel.getSplitChildren(editing.id).collectAsStateWithLifecycle(initialValue = emptyList())
        } else {
            remember { mutableStateOf(emptyList<TransactionEntity>()) }
        }

        AddEditTransactionDialog(
            transaction = editing,
            splitChildren = splitChildren,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            accounts = uiState.allAccounts,
            goals = uiState.allGoals,
            initialType = editing.type,
            onDismiss = { editingTransaction = null },
            onConfirm = { tx, children ->
                viewModel.updateTransaction(editing, tx, children)
                editingTransaction = null
            },
            onSaveAndDuplicate = { tx, children ->
                viewModel.updateTransaction(editing, tx, children)
                editingTransaction = null
            },
            onCreateTag = { name -> viewModel.createTag(name) }
        )
    }

    // Transfer dialog
    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.allAccounts,
            onDismiss = { showTransferDialog = false },
            onTransfer = { from, to, amount, note, date ->
                viewModel.addTransfer(from, to, amount, note, date)
                showTransferDialog = false
            }
        )
    }

    // Filter sheet
    if (showFilterSheet) {
        TransactionFilterSheet(
            accounts = uiState.allAccounts,
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
            onStartDateSelected = { viewModel.setDateRangeFilter(it, uiState.filterEndDate) },
            onEndDateSelected = { viewModel.setDateRangeFilter(uiState.filterStartDate, it) },
            onClearFilters = { viewModel.clearAllFilters() },
            onApply = {},
            onDismiss = { showFilterSheet = false }
        )
    }
}

// ---------------------------------------------------------------------------
// Transaction Card
// ---------------------------------------------------------------------------

@Composable
fun TransactionCard(
    transaction: TransactionEntity,
    tags: List<TagEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onEdit: (TransactionEntity) -> Unit,
    onDuplicate: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    val transactionTags = remember(transaction.tagIds, tags) {
        if (transaction.tagIds.isEmpty()) emptyList()
        else {
            val ids = transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }
            tags.filter { it.id in ids }
        }
    }
    val account = remember(transaction.accountId, accounts) { accounts.find { it.id == transaction.accountId } }
    val category = remember(transaction.categoryId, categories) { categories.find { it.id == transaction.categoryId } }
    val parentCategory = remember(category, categories) {
        category?.parentId?.let { pid -> categories.find { it.id == pid } }
    }
    val toAccount = remember(transaction.toAccountId, accounts) {
        transaction.toAccountId?.let { id -> accounts.find { it.id == id } }
    }

    val typeColor = when (transaction.type) {
        "income" -> Color(0xFF4CAF50)
        "expense" -> MaterialTheme.colorScheme.error
        "savings" -> Color(0xFF8B5E3C)
        "transfer" -> Color(0xFF5B6FB5)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val typeIcon = when {
        transaction.isSplitParent -> "🔀"
        transaction.isTransfer || transaction.type == "transfer" -> "⇌"
        transaction.type == "savings" -> "📈"
        else -> category?.emoji ?: when (transaction.type) {
            "income" -> "💰"
            else -> "💸"
        }
    }
    val amountText = when (transaction.type) {
        "income" -> "+${currencyFormat.format(transaction.amount)}"
        "expense", "savings" -> "-${currencyFormat.format(transaction.amount)}"
        "transfer" -> "⇌ ${currencyFormat.format(transaction.amount)}"
        else -> currencyFormat.format(transaction.amount)
    }

    Card(Modifier.fillMaxWidth().clickable { onEdit(transaction) }) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon circle
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                Alignment.Center
            ) {
                Text(typeIcon, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.width(12.dp))
            // Content
            Column(Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifEmpty { transaction.type.replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                // Metadata line
                val meta = buildString {
                    append(dateFormat.format(Date(transaction.date)))
                    account?.let { append(" · ${it.name}") }
                    if (transaction.isTransfer && toAccount != null) append(" → ${toAccount.name}")
                    if (parentCategory != null) append(" · ${parentCategory.name} › ${category?.name}")
                    else category?.let { append(" · ${it.name}") }
                    if (transaction.isSplitParent) append(" · Split")
                    transaction.investmentPlatform?.let { append(" · $it") }
                }
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Tags
                if (transactionTags.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(transactionTags) { tag ->
                            Row(
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(6.dp).clip(CircleShape).background(parseColor(tag.color)))
                                Spacer(Modifier.width(4.dp))
                                Text(tag.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // Amount + actions
            Column(horizontalAlignment = Alignment.End) {
                Text(amountText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = typeColor)
                Row {
                    IconButton(onClick = { onDuplicate(transaction) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, "Duplicate", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDelete(transaction) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, "Delete", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Add / Edit Transaction Dialog
// ---------------------------------------------------------------------------

data class SplitRowData(val localId: Int, val categoryId: Long? = null, val description: String = "", val amount: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity?,
    splitChildren: List<TransactionEntity> = emptyList(),
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    accounts: List<AccountEntity>,
    goals: List<GoalEntity>,
    initialType: String?,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onSaveAndDuplicate: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onCreateTag: (String) -> Unit,
) {
    val isEdit = transaction != null
    var type by remember { mutableStateOf(transaction?.type?.takeIf { it != "transfer" } ?: initialType ?: "expense") }
    var amount by remember { mutableStateOf(transaction?.amount?.let { if (it < 0) (-it).toString() else it.toString() } ?: "") }
    var selectedAccountId by remember { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(transaction?.categoryId) }
    var selectedDate by remember { mutableStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var description by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedGoalId by remember { mutableStateOf<Long?>(transaction?.goalId) }
    var selectedPlatform by remember { mutableStateOf<String?>(transaction?.investmentPlatform) }
    var splitEnabled by remember { mutableStateOf(transaction?.isSplitParent == true) }
    var splitRows by remember {
        mutableStateOf(
            if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
                splitChildren.mapIndexed { index, child ->
                    SplitRowData(index, child.categoryId, child.note, child.amount.toString())
                }
            } else {
                listOf(SplitRowData(0), SplitRowData(1))
            }
        )
    }
    var tagQuery by remember { mutableStateOf("") }
    var showTagDropdown by remember { mutableStateOf(false) }
    var selectedTagIds by remember {
        mutableStateOf(
            if (transaction?.tagIds?.isNotEmpty() == true)
                transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            else emptySet<Long>()
        )
    }
    var receiptData by remember { mutableStateOf<String?>(transaction?.receiptPath) }
    var showReceiptPreview by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showSubCategoryDropdown by remember { mutableStateOf(false) }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var showPlatformDropdown by remember { mutableStateOf(false) }
    var splitIdCounter by remember { mutableStateOf(2) }
    var showSplitCategoryDropdown by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val dateDisplayFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }

    val parentCategories = remember(categories, type) {
        categories.filter { it.parentId == null && (it.type == type || (type == "savings" && it.type == "expense")) }
    }
    val selectedParentCategory = remember(selectedCategoryId, categories) {
        val cat = categories.find { it.id == selectedCategoryId }
        if (cat?.parentId != null) categories.find { it.id == cat.parentId } else cat
    }
    val selectedSubCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }?.takeIf { it.parentId != null }
    }
    val subCategories = remember(selectedParentCategory, categories) {
        selectedParentCategory?.let { parent -> categories.filter { it.parentId == parent.id } } ?: emptyList()
    }

    val filteredTags = remember(tagQuery, tags) {
        if (tagQuery.isEmpty()) tags else tags.filter { it.name.contains(tagQuery, ignoreCase = true) }
    }
    val selectedTags = remember(selectedTagIds, tags) { tags.filter { it.id in selectedTagIds } }

    // Update splitRows if splitChildren changes (useful when editing and children are loaded asynchronously)
    LaunchedEffect(splitChildren) {
        if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
            splitRows = splitChildren.mapIndexed { index, child ->
                SplitRowData(index, child.categoryId, child.note, child.amount.toString())
            }
        }
    }

    // Auto-select newly created tag
    var pendingTagName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tags) {
        pendingTagName?.let { name ->
            val newTag = tags.find { it.name.equals(name, ignoreCase = true) }
            if (newTag != null) {
                selectedTagIds = selectedTagIds + newTag.id
                pendingTagName = null
                tagQuery = ""
            }
        }
    }

    // Receipt picker
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { receiptData = uriToBase64(context, it) }
    }

    val mainAmount = amount.toDoubleOrNull() ?: 0.0
    val splitTotal = splitRows.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val splitRemaining = mainAmount - splitTotal

    fun buildTransaction(): TransactionEntity? {
        val amt = amount.toDoubleOrNull() ?: return null
        if (amt <= 0 || selectedAccountId == null) return null
        val effectiveCategoryId = selectedSubCategory?.id ?: selectedParentCategory?.id
        return TransactionEntity(
            id = transaction?.id ?: 0,
            accountId = selectedAccountId!!,
            type = type,
            amount = amt,
            categoryId = effectiveCategoryId,
            goalId = selectedGoalId,
            tagIds = selectedTagIds.joinToString(","),
            date = selectedDate,
            note = description,
            receiptPath = receiptData,
            isRecurring = transaction?.isRecurring ?: false,
            recurringId = transaction?.recurringId,
            isSplitParent = splitEnabled,
            isTransfer = false,
            investmentPlatform = if (type == "savings") selectedPlatform else null,
            createdAt = transaction?.createdAt ?: System.currentTimeMillis()
        )
    }

    fun buildSplitChildren(parentId: Long): List<TransactionEntity> {
        return splitRows.mapNotNull { row ->
            val rowAmt = row.amount.toDoubleOrNull() ?: return@mapNotNull null
            if (rowAmt <= 0) return@mapNotNull null
            TransactionEntity(
                accountId = selectedAccountId!!,
                type = type,
                amount = rowAmt,
                categoryId = row.categoryId,
                tagIds = "",
                date = selectedDate,
                note = row.description,
                isSplitChild = true,
                parentTransactionId = parentId
            )
        }
    }

    // Date/Time pickers
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        val timeCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                        selectedDate = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    selectedDate = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    // Receipt full-size preview
    if (showReceiptPreview && receiptData != null) {
        Dialog(onDismissRequest = { showReceiptPreview = false }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    receiptData?.let { data ->
                        if (data.startsWith("data:image")) {
                            val bitmap = remember(data) {
                                runCatching {
                                    val b64 = data.substringAfter("base64,")
                                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }.getOrNull()
                            }
                            bitmap?.let {
                                Image(it.asImageBitmap(), "Receipt", Modifier.fillMaxWidth())
                            }
                        } else {
                            Icon(Icons.Default.PictureAsPdf, null, Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                            Text("PDF Receipt", Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showReceiptPreview = false }, Modifier.align(Alignment.End)) { Text("Close") }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // Header
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isEdit) "Edit Transaction" else "Add Transaction", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Close") }
                }
                HorizontalDivider()

                // Scrollable body
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Type toggle (Income / Expense / Savings)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("income", "expense", "savings").forEach { t ->
                            FilterChip(
                                selected = type == t,
                                onClick = { type = t; selectedCategoryId = null },
                                label = { Text(t.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    // Amount
                    OutlinedTextField(
                        value = amount, onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )

                    // Date & Time
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                            onValueChange = {}, readOnly = true,
                            label = { Text("Date") },
                            trailingIcon = { Icon(Icons.Default.DateRange, null) },
                            modifier = Modifier.weight(1f).clickable { showDatePicker = true }
                        )
                        OutlinedTextField(
                            value = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedDate)),
                            onValueChange = {}, readOnly = true,
                            label = { Text("Time") },
                            trailingIcon = { Icon(Icons.Default.Schedule, null) },
                            modifier = Modifier.weight(1f).clickable { showTimePicker = true }
                        )
                    }

                    // Account
                    ExposedDropdownMenuBox(showAccountDropdown, { showAccountDropdown = it }) {
                        OutlinedTextField(
                            value = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account",
                            onValueChange = {}, readOnly = true, label = { Text("Account") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showAccountDropdown) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(showAccountDropdown, { showAccountDropdown = false }) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text(acc.name) },
                                    onClick = { selectedAccountId = acc.id; showAccountDropdown = false }
                                )
                            }
                        }
                    }

                    // Category (parent)
                    if (!splitEnabled) {
                        ExposedDropdownMenuBox(showCategoryDropdown, { showCategoryDropdown = it }) {
                            OutlinedTextField(
                                value = selectedParentCategory?.let { "${it.emoji} ${it.name}" } ?: "Select Category",
                                onValueChange = {}, readOnly = true, label = { Text("Category") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showCategoryDropdown) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(showCategoryDropdown, { showCategoryDropdown = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { selectedCategoryId = null; showCategoryDropdown = false })
                                parentCategories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${cat.emoji} ${cat.name}") },
                                        onClick = { selectedCategoryId = cat.id; showCategoryDropdown = false }
                                    )
                                }
                            }
                        }

                        // Sub-category (only when parent has children)
                        if (subCategories.isNotEmpty()) {
                            ExposedDropdownMenuBox(showSubCategoryDropdown, { showSubCategoryDropdown = it }) {
                                OutlinedTextField(
                                    value = selectedSubCategory?.name ?: "Select Sub-category (Optional)",
                                    onValueChange = {}, readOnly = true, label = { Text("Sub-category") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showSubCategoryDropdown) },
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                                )
                                ExposedDropdownMenu(showSubCategoryDropdown, { showSubCategoryDropdown = false }) {
                                    DropdownMenuItem(text = { Text("None") }, onClick = {
                                        selectedCategoryId = selectedParentCategory?.id; showSubCategoryDropdown = false
                                    })
                                    subCategories.forEach { sub ->
                                        DropdownMenuItem(
                                            text = { Text(sub.name) },
                                            onClick = { selectedCategoryId = sub.id; showSubCategoryDropdown = false }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text("Category overridden by splits below", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Description
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )

                    // Savings-only fields
                    if (type == "savings") {
                        // Goal link
                        ExposedDropdownMenuBox(showGoalDropdown, { showGoalDropdown = it }) {
                            OutlinedTextField(
                                value = goals.find { it.id == selectedGoalId }?.let { "${it.emoji} ${it.name}" } ?: "Link to Goal (Optional)",
                                onValueChange = {}, readOnly = true, label = { Text("Goal Link") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showGoalDropdown) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(showGoalDropdown, { showGoalDropdown = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { selectedGoalId = null; showGoalDropdown = false })
                                goals.forEach { goal ->
                                    DropdownMenuItem(
                                        text = { Text("${goal.emoji} ${goal.name}") },
                                        onClick = { selectedGoalId = goal.id; showGoalDropdown = false }
                                    )
                                }
                            }
                        }
                        // Investment platform
                        ExposedDropdownMenuBox(showPlatformDropdown, { showPlatformDropdown = it }) {
                            OutlinedTextField(
                                value = selectedPlatform ?: "Select Platform (Optional)",
                                onValueChange = {}, readOnly = true, label = { Text("Investment Platform") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showPlatformDropdown) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(showPlatformDropdown, { showPlatformDropdown = false }) {
                                DropdownMenuItem(text = { Text("None") }, onClick = { selectedPlatform = null; showPlatformDropdown = false })
                                INVESTMENT_PLATFORMS.forEach { p ->
                                    DropdownMenuItem(text = { Text(p) }, onClick = { selectedPlatform = p; showPlatformDropdown = false })
                                }
                            }
                        }
                    }

                    // Split toggle
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Split by category", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        Switch(checked = splitEnabled, onCheckedChange = { splitEnabled = it })
                    }

                    // Split rows
                    if (splitEnabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            splitRows.forEachIndexed { index, row ->
                                SplitRowCard(
                                    row = row,
                                    categories = categories.filter { it.parentId == null && (it.type == type || (type == "savings" && it.type == "expense")) },
                                    onUpdate = { updated -> splitRows = splitRows.toMutableList().also { it[index] = updated } },
                                    onRemove = { splitRows = splitRows.toMutableList().also { it.removeAt(index) } },
                                    showDropdown = showSplitCategoryDropdown == row.localId,
                                    onToggleDropdown = { showSplitCategoryDropdown = if (showSplitCategoryDropdown == row.localId) null else row.localId }
                                )
                            }
                            // Running total
                            val balanced = kotlin.math.abs(splitRemaining) < 0.001
                            Text(
                                "Allocated: ${String.format("%.2f", splitTotal)}  Remaining: ${String.format("%.2f", splitRemaining)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (balanced) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = {
                                splitRows = splitRows + SplitRowData(splitIdCounter++)
                            }) {
                                Icon(Icons.Default.Add, null)
                                Text(" Add Split")
                            }
                        }
                    }

                    // Tags (searchable with inline create)
                    Text("Tags", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    // Selected tag chips
                    if (selectedTags.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(selectedTags) { tag ->
                                InputChip(
                                    selected = true,
                                    onClick = { selectedTagIds = selectedTagIds - tag.id },
                                    label = { Text(tag.name) },
                                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                                )
                            }
                        }
                    }
                    ExposedDropdownMenuBox(showTagDropdown, { showTagDropdown = it }) {
                        OutlinedTextField(
                            value = tagQuery, onValueChange = { tagQuery = it; showTagDropdown = true },
                            label = { Text("Search or create tag") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showTagDropdown) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(showTagDropdown, { showTagDropdown = false }) {
                            if (filteredTags.isEmpty() && tagQuery.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Create \"$tagQuery\"") },
                                    leadingIcon = { Icon(Icons.Default.Add, null) },
                                    onClick = {
                                        pendingTagName = tagQuery
                                        onCreateTag(tagQuery)
                                        showTagDropdown = false
                                    }
                                )
                            }
                            filteredTags.forEach { tag ->
                                DropdownMenuItem(
                                    text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(10.dp).clip(CircleShape).background(parseColor(tag.color)))
                                        Spacer(Modifier.width(8.dp))
                                        Text(tag.name)
                                    }},
                                    onClick = {
                                        selectedTagIds = if (tag.id in selectedTagIds) selectedTagIds - tag.id else selectedTagIds + tag.id
                                        tagQuery = ""
                                        showTagDropdown = false
                                    },
                                    trailingIcon = if (tag.id in selectedTagIds) ({ Icon(Icons.Default.Check, null) }) else null
                                )
                            }
                        }
                    }

                    // Notes (multi-line)
                    OutlinedTextField(
                        value = description, onValueChange = { description = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                        maxLines = 4
                    )

                    // Receipt upload
                    Text("Receipt", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (receiptData != null) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (receiptData!!.startsWith("data:image")) {
                                val bitmap = remember(receiptData) {
                                    runCatching {
                                        val b64 = receiptData!!.substringAfter("base64,")
                                        val bytes = Base64.decode(b64, Base64.DEFAULT)
                                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                    }.getOrNull()
                                }
                                bitmap?.let {
                                    Image(it.asImageBitmap(), "Receipt thumbnail",
                                        Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).clickable { showReceiptPreview = true })
                                }
                            } else {
                                Box(Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showReceiptPreview = true }, Alignment.Center) {
                                    Icon(Icons.Default.PictureAsPdf, "PDF", Modifier.size(36.dp))
                                }
                            }
                            Column {
                                TextButton(onClick = { filePicker.launch("image/*") }) { Text("Replace") }
                                TextButton(onClick = { receiptData = null }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    } else {
                        OutlinedButton(onClick = { filePicker.launch("image/*") }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AttachFile, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Attach Receipt (Image or PDF)")
                        }
                    }
                }

                // Footer buttons
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, Modifier.weight(1f)) { Text("Cancel") }
                    if (!isEdit) {
                        OutlinedButton(
                            onClick = {
                                val tx = buildTransaction() ?: return@OutlinedButton
                                val children = if (splitEnabled) buildSplitChildren(0) else null
                                onSaveAndDuplicate(tx, children)
                                
                                // Reset form for next entry (keep same type/account/category)
                                amount = ""
                                description = ""
                                selectedDate = System.currentTimeMillis()
                                receiptData = null
                                selectedTagIds = emptySet()
                                if (splitEnabled) {
                                    splitRows = listOf(SplitRowData(0), SplitRowData(1))
                                }
                            },
                            modifier = Modifier.weight(1.3f),
                            enabled = amount.toDoubleOrNull() != null && selectedAccountId != null &&
                                (!splitEnabled || kotlin.math.abs(splitRemaining) < 0.001)
                        ) { Text("Save & Dup.", maxLines = 1) }
                    }
                    Button(
                        onClick = {
                            val tx = buildTransaction() ?: return@Button
                            if (splitEnabled) {
                                val children = buildSplitChildren(0)
                                onConfirm(tx, children)
                            } else {
                                onConfirm(tx, null)
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        enabled = amount.toDoubleOrNull() != null && selectedAccountId != null &&
                            (!splitEnabled || kotlin.math.abs(splitRemaining) < 0.001)
                    ) { Text(if (isEdit) "Update" else "Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitRowCard(
    row: SplitRowData,
    categories: List<CategoryEntity>,
    onUpdate: (SplitRowData) -> Unit,
    onRemove: () -> Unit,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(showDropdown, { if (it) onToggleDropdown() else onToggleDropdown() }) {
                OutlinedTextField(
                    value = categories.find { it.id == row.categoryId }?.let { "${it.emoji} ${it.name}" } ?: "Category",
                    onValueChange = {}, readOnly = true, label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showDropdown) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                )
                ExposedDropdownMenu(showDropdown, onToggleDropdown) {
                    DropdownMenuItem(text = { Text("None") }, onClick = { onUpdate(row.copy(categoryId = null)); onToggleDropdown() })
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text("${cat.emoji} ${cat.name}") },
                            onClick = { onUpdate(row.copy(categoryId = cat.id)); onToggleDropdown() }
                        )
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = row.description, onValueChange = { onUpdate(row.copy(description = it)) },
                    label = { Text("Description") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = row.amount, onValueChange = { onUpdate(row.copy(amount = it)) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(100.dp), singleLine = true
                )
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove split", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun uriToBase64(context: Context, uri: Uri): String? = runCatching {
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val b64 = Base64.encodeToString(bytes, Base64.DEFAULT)
    "data:$mimeType;base64,$b64"
}.getOrNull()

