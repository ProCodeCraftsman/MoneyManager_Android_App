package com.moneymanager.app.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.components.NumericKeypad
import com.moneymanager.app.ui.components.SplitRowCard
import com.moneymanager.app.ui.theme.LocalCategoryColors
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.app.ui.util.accountTypeIcon
import com.moneymanager.app.ui.utils.evaluateExpression
import com.moneymanager.data.entity.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class SplitRowData(
    val localId: Int,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val description: String = "",
    val amount: String = ""
)

private data class UtilityActionItemData(
    val icon: ImageVector,
    val label: String,
    val subtext: String,
    val active: Boolean,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity?,
    splitChildren: List<TransactionEntity> = emptyList(),
    currency: String,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    accounts: List<AccountEntity>,
    peers: List<PeerContact> = emptyList(),
    goals: List<GoalEntity>,
    initialType: String?,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
) {
    val isEdit = transaction != null

    // ── Core State ──
    var type by rememberSaveable { mutableStateOf(transaction?.type ?: initialType ?: "expense") }

    // Filter accounts based on transaction type
    val filteredAccounts = remember(type, accounts) {
        when (type) {
            "expense" -> accounts.filter { it.type != "savings" }
            "savings" -> accounts.filter { it.type == "savings" }
            else -> accounts
        }
    }

    var amount by rememberSaveable {
        mutableStateOf(transaction?.amount?.let { if (it < 0) (-it).toString() else it.toString() } ?: "")
    }
    var selectedAccountId by rememberSaveable { mutableStateOf(transaction?.accountId ?: filteredAccounts.firstOrNull()?.id) }
    var selectedToAccountId by rememberSaveable { mutableStateOf(transaction?.toAccountId) }
    var showToAccountDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(transaction?.categoryId) }
    var selectedPeerId by rememberSaveable { mutableStateOf<Long?>(transaction?.peerContactId) }
    var selectedDate by rememberSaveable { mutableStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var description by rememberSaveable { mutableStateOf(transaction?.note ?: "") }
    var selectedGoalId by rememberSaveable { mutableStateOf<Long?>(transaction?.goalId) }
    var selectedPlatform by rememberSaveable { mutableStateOf<String?>(transaction?.investmentPlatform) }

    // ── Split State ──
    var splitEnabled by rememberSaveable { mutableStateOf(transaction?.isSplitParent == true) }
    var splitRows by rememberSaveable {
        mutableStateOf(
            if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
                splitChildren.mapIndexed { index, child ->
                    val pId = child.categoryId?.let { cid ->
                        val cat = categories.firstOrNull { it.id == cid }
                        if (cat?.parentId != null) cat.parentId else cid
                    }
                    val sId = child.categoryId?.let { cid ->
                        val cat = categories.firstOrNull { it.id == cid }
                        if (cat?.parentId != null) cid else null
                    }
                    SplitRowData(index, pId, sId, child.note, child.amount.toString())
                }
            } else {
                listOf(SplitRowData(0), SplitRowData(1))
            }
        )
    }
    var splitIdCounter by rememberSaveable { mutableStateOf(2) }
    var showSplitCategoryDropdown by rememberSaveable { mutableStateOf<Int?>(null) }

    // ── Tags State ──
    var tagQuery by rememberSaveable { mutableStateOf("") }
    var showTagDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedTagIds by rememberSaveable {
        mutableStateOf(
            if (transaction?.tagIds?.isNotEmpty() == true)
                transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            else emptySet()
        )
    }
    var pendingTagName by rememberSaveable { mutableStateOf<String?>(null) }

    // ── Receipt State ──
    var receiptData by rememberSaveable { mutableStateOf<String?>(transaction?.receiptPath) }
    var showReceiptPreview by rememberSaveable { mutableStateOf(false) }

    // ── UI Visibility Toggles ──
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showAccountDropdown by rememberSaveable { mutableStateOf(false) }
    var expectedReturnDate by rememberSaveable { mutableStateOf<Long?>(transaction?.expectedReturnDate) }
    var showExpectedReturnDatePicker by rememberSaveable { mutableStateOf(false) }
    var expandedCategoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var isCalculatorVisible by rememberSaveable { mutableStateOf(transaction?.amount == null || transaction.amount == 0.0) }
    var categorySearchQuery by rememberSaveable { mutableStateOf("") }
    var showCategorySearch by rememberSaveable { mutableStateOf(false) }
    var showNoteInput by rememberSaveable { mutableStateOf(true) }
    var showTagInput by rememberSaveable { mutableStateOf(transaction?.tagIds?.isNotEmpty() == true) }
    var showReceiptInput by rememberSaveable { mutableStateOf(transaction?.receiptPath != null) }
    var showGoalDropdown by rememberSaveable { mutableStateOf(false) }

    // ── Config & Derived State ──
    val config = remember(type) { TransactionFormConfig.getType(type) }
    val features = config.features
    val categoryFilter = TransactionFormConfig.resolveCategoryType(type)

    val parentCategories = remember(categories, categoryFilter) {
        categories.filter { it.parentId == null && it.type == categoryFilter }
    }
    val selectedParentCategory = remember(selectedCategoryId, categories) {
        val cat = categories.firstOrNull { it.id == selectedCategoryId }
        if (cat?.parentId != null) categories.firstOrNull { it.id == cat.parentId } else cat
    }
    val selectedSubCategory = remember(selectedCategoryId, categories) {
        categories.firstOrNull { it.id == selectedCategoryId }?.takeIf { it.parentId != null }
    }
    val subCategories = remember(selectedParentCategory, categories) {
        selectedParentCategory?.let { p -> categories.filter { it.parentId == p.id } } ?: emptyList()
    }
    val filteredTags = remember(tagQuery, tags) {
        if (tagQuery.isEmpty()) tags else tags.filter { it.name.contains(tagQuery, ignoreCase = true) }
    }
    val selectedTags = remember(selectedTagIds, tags) { tags.filter { it.id in selectedTagIds } }
    val mainAmount = amount.toDoubleOrNull() ?: 0.0
    val splitTotal = splitRows.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val splitRemaining = mainAmount - splitTotal
    val context = LocalContext.current

    // ── Type Switch Handler ──
    fun onTypeSelected(newType: String) {
        if (newType == type) return
        type = newType
        selectedCategoryId = null
        expandedCategoryId = null
        selectedPeerId = null
        selectedToAccountId = null
        selectedGoalId = null
        selectedPlatform = null
        expectedReturnDate = null
        splitEnabled = false
        // Reset account if no longer valid for this type
        val validIds = when (newType) {
            "expense" -> accounts.filter { it.type != "savings" }
            "savings" -> accounts.filter { it.type == "savings" }
            else -> accounts
        }.map { it.id }
        if (selectedAccountId != null && selectedAccountId !in validIds) {
            selectedAccountId = validIds.firstOrNull()
        }
    }

    // ── Transaction Builders ──
    fun buildTransaction(): TransactionEntity? {
        val amt = amount.toDoubleOrNull() ?: return null
        if (amt <= 0 || selectedAccountId == null) return null
        val effectiveCategoryId =
            if (TransactionFeature.CATEGORY in features) (selectedSubCategory?.id ?: selectedParentCategory?.id)
            else null
        return TransactionEntity(
            id = transaction?.id ?: 0,
            accountId = selectedAccountId!!,
            type = type,
            amount = amt,
            categoryId = effectiveCategoryId,
            peerContactId = if (TransactionFeature.PEER in features) selectedPeerId else null,
            expectedReturnDate = if (TransactionFeature.RETURN_DATE in features) expectedReturnDate else null,
            goalId = if (TransactionFeature.GOAL in features) selectedGoalId else null,
            investmentPlatform = if (TransactionFeature.PLATFORM in features) selectedPlatform else null,
            tagIds = selectedTagIds.joinToString(","),
            date = selectedDate,
            note = description,
            description = description,
            receiptPath = receiptData,
            isRecurring = transaction?.isRecurring ?: false,
            recurringId = transaction?.recurringId,
            isSplitParent = splitEnabled && TransactionFeature.SPLIT in features,
            isTransfer = type == "transfer",
            toAccountId = if (type == "transfer") selectedToAccountId else null,
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
                categoryId = row.subCategoryId ?: row.categoryId,
                tagIds = "",
                date = selectedDate,
                note = row.description,
                description = row.description,
                isSplitChild = true,
                isTransfer = type == "transfer",
                parentTransactionId = parentId
            )
        }
    }

    // ── Effects ──
    LaunchedEffect(splitChildren) {
        if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
            splitRows = splitChildren.mapIndexed { index, child ->
                val pId = child.categoryId?.let { cid ->
                    val cat = categories.firstOrNull { it.id == cid }
                    if (cat?.parentId != null) cat.parentId else cid
                }
                val sId = child.categoryId?.let { cid ->
                    val cat = categories.firstOrNull { it.id == cid }
                    if (cat?.parentId != null) cid else null
                }
                SplitRowData(index, pId, sId, child.note, child.amount.toString())
            }
        }
    }

    LaunchedEffect(tags) {
        pendingTagName?.let { name ->
            val newTag = tags.firstOrNull { it.name.equals(name, ignoreCase = true) }
            if (newTag != null) {
                selectedTagIds = selectedTagIds + newTag.id
                pendingTagName = null
                tagQuery = ""
            }
        }
    }

    // ── File Picker ──
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { receiptData = FileHelper.saveReceipt(context, it) }
    }

    // ── Overlay Dialogs ──
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
        ) { DatePicker(state = datePickerState) }
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

    if (showExpectedReturnDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expectedReturnDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showExpectedReturnDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expectedReturnDate = datePickerState.selectedDateMillis
                    showExpectedReturnDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    expectedReturnDate = null
                    showExpectedReturnDatePicker = false
                }) { Text("Clear") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showReceiptPreview && receiptData != null) {
        FormReceiptPreviewDialog(receiptData = receiptData!!, onDismiss = { showReceiptPreview = false })
    }

    // ── Main Dialog ──
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                // Top Bar
                DialogTopBar(
                    title = "${if (isEdit) "Edit" else "Add"} ${config.displayName}",
                    onDismiss = onDismiss
                )

                // Type Selector
                TransactionTypeHeader(selectedType = type, onTypeSelected = ::onTypeSelected)

                // Scrollable Form Body
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Amount, Date & Account Card
                    Box {
                        FormAmountDateAccountCard(
                            amount = amount,
                            currency = currency,
                            selectedDate = selectedDate,
                            selectedAccountId = selectedAccountId,
                            accounts = filteredAccounts,
                            expectedReturnDate = expectedReturnDate,
                            showExpectedReturnDate = TransactionFeature.RETURN_DATE in features,
                            onAmountClick = { isCalculatorVisible = !isCalculatorVisible },
                            onDateClick = { showDatePicker = true },
                            onAccountClick = { showAccountDropdown = true },
                            onExpectedReturnDateClick = { showExpectedReturnDatePicker = true }
                        )
                        DropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false }
                        ) {
                            (filteredAccounts.ifEmpty { accounts }).forEach { acc ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(accountTypeIcon(acc.type), null, modifier = Modifier.size(20.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(acc.name)
                                        }
                                    },
                                    onClick = { selectedAccountId = acc.id; showAccountDropdown = false }
                                )
                            }
                        }
                    }

                    // 2. Categories (CATEGORY feature types only)
                    if (TransactionFeature.CATEGORY in features) {
                        FormCategorySection(
                            categories = categories,
                            categoryFilter = categoryFilter,
                            selectedCategoryId = selectedCategoryId,
                            expandedCategoryId = expandedCategoryId,
                            categoryUsageCounts = categoryUsageCounts,
                            onCategoryClick = { cat ->
                                if (cat.id == selectedCategoryId) {
                                    selectedCategoryId = null
                                    if (cat.parentId == null) expandedCategoryId = null
                                } else {
                                    selectedCategoryId = cat.id
                                    if (cat.parentId == null) expandedCategoryId = cat.id
                                }
                            },
                            onBackClick = { expandedCategoryId = null },
                            onMoreClick = { showCategorySearch = true }
                        )
                    }

                    // 3. Peer Picker (PEER feature types only)
                    if (TransactionFeature.PEER in features) {
                        FormPeerSection(
                            peers = peers,
                            selectedPeerId = selectedPeerId,
                            onPeerSelected = { selectedPeerId = it }
                        )
                    }

                    // 4. Transfer To-Account (TO_ACCOUNT feature types only)
                    if (TransactionFeature.TO_ACCOUNT in features) {
                        FormTransferSection(
                            accounts = accounts,
                            selectedAccountId = selectedAccountId,
                            selectedToAccountId = selectedToAccountId,
                            showToAccountDropdown = showToAccountDropdown,
                            onToAccountSelected = { selectedToAccountId = it },
                            onToggleToAccountDropdown = { showToAccountDropdown = !showToAccountDropdown }
                        )
                    }

                    // 5. Goal & Platform (savings-specific)
                    if (TransactionFeature.GOAL in features) {
                        FormGoalSection(
                            goals = goals,
                            selectedGoalId = selectedGoalId,
                            showGoalDropdown = showGoalDropdown,
                            onGoalSelected = { selectedGoalId = it },
                            onToggleGoalDropdown = { showGoalDropdown = !showGoalDropdown }
                        )
                    }

                    if (TransactionFeature.PLATFORM in features) {
                        OutlinedTextField(
                            value = selectedPlatform ?: "",
                            onValueChange = { selectedPlatform = it.ifEmpty { null } },
                            label = { Text("Investment Platform") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // 6. Utility Actions Row
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(
                            UtilityActionItemData(Icons.Default.Notes, "Note", if (description.isEmpty()) "Add note" else description, showNoteInput) { showNoteInput = !showNoteInput },
                            UtilityActionItemData(Icons.Default.LocalOffer, "Tags", if (selectedTagIds.isEmpty()) "Add tags" else "${selectedTagIds.size}", showTagInput) { showTagInput = !showTagInput },
                            UtilityActionItemData(Icons.Default.AttachFile, "Attach", if (receiptData == null) "Add" else "1", showReceiptInput) { showReceiptInput = !showReceiptInput },
                            UtilityActionItemData(Icons.AutoMirrored.Filled.CallSplit, "Split", if (!splitEnabled) "Split" else "On", splitEnabled) { splitEnabled = !splitEnabled },
                        ).filter { a ->
                            when (a.label) {
                                "Tags" -> TransactionFeature.TAGS in features
                                "Split" -> TransactionFeature.SPLIT in features
                                else -> true
                            }
                        }.forEach { a ->
                            UtilityActionItem(a.icon, a.label, a.subtext, a.onClick, a.active, Modifier.weight(1f))
                        }
                    }

                    // 7. Split Section (SPLIT feature + enabled)
                    if (TransactionFeature.SPLIT in features && splitEnabled) {
                        FormSplitSection(
                            splitRows = splitRows,
                            splitRemaining = splitRemaining,
                            splitTotal = splitTotal,
                            categories = categories,
                            type = type,
                            currency = currency,
                            showSplitCategoryDropdown = showSplitCategoryDropdown,
                            onUpdateRow = { index, updated ->
                                splitRows = splitRows.toMutableList().also { it[index] = updated }
                            },
                            onRemoveRow = { index ->
                                splitRows = splitRows.toMutableList().also { it.removeAt(index) }
                            },
                            onToggleDropdown = { rowId ->
                                showSplitCategoryDropdown =
                                    if (showSplitCategoryDropdown == rowId) null else rowId
                            },
                            onAddRow = { splitRows = splitRows + SplitRowData(splitIdCounter++) }
                        )
                    }

                    // 8. Note Input
                    if (showNoteInput) {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1
                        )
                    }

                    // 9. Tag Input (TAGS feature only)
                    if (TransactionFeature.TAGS in features && showTagInput) {
                        FormTagSection(
                            selectedTags = selectedTags,
                            tagQuery = tagQuery,
                            onTagQueryChange = { tagQuery = it; showTagDropdown = true },
                            onRemoveTag = { selectedTagIds = selectedTagIds - it }
                        )
                    }

                    // 10. Numeric Keypad
                    if (isCalculatorVisible) {
                        NumericKeypad(
                            onNumberClick = { num ->
                                if (num == ".") {
                                    if (amount.isEmpty()) amount = "0."
                                    else if (!amount.contains(".")) amount += "."
                                } else if (num in listOf("+", "-", "*", "/")) {
                                    if (amount.isNotEmpty() && !amount.last().isDigit())
                                        amount = amount.dropLast(1) + num
                                    else if (amount.isNotEmpty()) amount += num
                                } else {
                                    if (amount == "0") amount = num else amount += num
                                }
                            },
                            onDeleteClick = { if (amount.isNotEmpty()) amount = amount.dropLast(1) },
                            onClearClick = { amount = "" },
                            onEvaluate = {
                                try {
                                    val res = evaluateExpression(amount)
                                    amount = if (res % 1.0 == 0.0) res.toInt().toString()
                                    else "%.2f".format(Locale.US, res)
                                    isCalculatorVisible = false
                                } catch (_: Exception) {}
                            }
                        )
                    }

                    // 11. Action Buttons
                    FormActionButtons(
                        isEdit = isEdit,
                        amountValid = amount.isNotEmpty(),
                        accountValid = selectedAccountId != null,
                        onCancel = onDismiss,
                        onSave = {
                            val tx = buildTransaction()
                            if (tx != null) {
                                val children = if (splitEnabled && TransactionFeature.SPLIT in features)
                                    buildSplitChildren(tx.id) else null
                                onConfirm(tx, children)
                            }
                        }
                    )
                }
            }
        }
    }

    // ── Category Search Dialog ──
    if (showCategorySearch && TransactionFeature.CATEGORY in features) {
        FormCategorySearchDialog(
            query = categorySearchQuery,
            onQueryChange = { categorySearchQuery = it },
            categories = categories,
            categoryFilter = categoryFilter,
            categoryUsageCounts = categoryUsageCounts,
            onCategorySelected = { cat ->
                selectedCategoryId = cat.id
                expandedCategoryId = cat.parentId
                showCategorySearch = false
            },
            onDismiss = { showCategorySearch = false }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Extracted Section Composables
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DialogTopBar(title: String, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Back") }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        IconButton(onClick = {}, enabled = false) {}
    }
}

@Composable
private fun FormAmountDateAccountCard(
    amount: String,
    currency: String,
    selectedDate: Long,
    selectedAccountId: Long?,
    accounts: List<AccountEntity>,
    expectedReturnDate: Long?,
    showExpectedReturnDate: Boolean,
    onAmountClick: () -> Unit,
    onDateClick: () -> Unit,
    onAccountClick: () -> Unit,
    onExpectedReturnDateClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Amount
                Column(modifier = Modifier.weight(1f).clickable { onAmountClick() }) {
                    Text(
                        "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            CurrencyUtils.getCurrencySymbol(currency),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (amount.isEmpty()) "0" else amount,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (amount.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                VerticalDivider(
                    modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )

                // Date & Account
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth().clickable { onDateClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { onAccountClick() },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountBalance, null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            accounts.find { it.id == selectedAccountId }?.name ?: "Select",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (showExpectedReturnDate) {
                        Row(
                            Modifier.fillMaxWidth().clickable { onExpectedReturnDateClick() },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.EventNote, null,
                                modifier = Modifier.size(14.dp), tint = Color(0xFFE65100)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                expectedReturnDate?.let {
                                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(it))
                                } ?: "Return Date",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormCategorySection(
    categories: List<CategoryEntity>,
    categoryFilter: String,
    selectedCategoryId: Long?,
    expandedCategoryId: Long?,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    onCategoryClick: (CategoryEntity) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Categories", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            if (expandedCategoryId != null) {
                TextButton(onClick = onBackClick, contentPadding = PaddingValues(0.dp)) {
                    Text("Back", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        CategoryCarousel(
            categories = categories,
            type = categoryFilter,
            selectedCategoryId = selectedCategoryId,
            expandedCategoryId = expandedCategoryId,
            categoryUsageCounts = categoryUsageCounts,
            onCategoryClick = onCategoryClick,
            onMoreClick = onMoreClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdownCard(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    items: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        ExposedDropdownMenuBox(expanded, onExpandedChange) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).padding(8.dp),
                singleLine = true
            )
            ExposedDropdownMenu(expanded, onDismissRequest, content = items)
        }
    }
}

@Composable
private fun FormPeerSection(
    peers: List<PeerContact>,
    selectedPeerId: Long?,
    onPeerSelected: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedPeer = peers.find { it.id == selectedPeerId }

    FormDropdownCard(
        label = "Person",
        value = selectedPeer?.displayName ?: "Select person",
        expanded = expanded,
        onExpandedChange = { expanded = it },
        onDismissRequest = { expanded = false }
    ) {
        if (selectedPeerId != null) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = { onPeerSelected(null); expanded = false }
            )
        }
        peers.forEach { peer ->
            DropdownMenuItem(
                text = { Text(peer.displayName) },
                onClick = { onPeerSelected(peer.id); expanded = false }
            )
        }
    }
}
@Composable
private fun FormTransferSection(
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    selectedToAccountId: Long?,
    showToAccountDropdown: Boolean,
    onToAccountSelected: (Long?) -> Unit,
    onToggleToAccountDropdown: () -> Unit,
) {
    val eligibleAccounts = remember(accounts, selectedAccountId) {
        accounts.filter { it.id != selectedAccountId }
    }
    FormDropdownCard(
        label = "To Account",
        value = accounts.find { it.id == selectedToAccountId }?.name ?: "Select destination account",
        expanded = showToAccountDropdown,
        onExpandedChange = { if (it) onToggleToAccountDropdown() },
        onDismissRequest = { onToggleToAccountDropdown() }
    ) {
        eligibleAccounts.forEach { acc ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(acc.name)
                        Text(
                            "${acc.type} \u00B7 ${acc.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                onClick = { onToAccountSelected(acc.id); onToggleToAccountDropdown() }
            )
        }
    }
}

@Composable
private fun FormGoalSection(
    goals: List<GoalEntity>,
    selectedGoalId: Long?,
    showGoalDropdown: Boolean,
    onGoalSelected: (Long?) -> Unit,
    onToggleGoalDropdown: () -> Unit,
) {
    FormDropdownCard(
        label = "Goal",
        value = goals.find { it.id == selectedGoalId }?.name ?: "Select goal (optional)",
        expanded = showGoalDropdown,
        onExpandedChange = { if (it) onToggleGoalDropdown() },
        onDismissRequest = { onToggleGoalDropdown() }
    ) {
        if (selectedGoalId != null) {
            DropdownMenuItem(
                text = { Text("None") },
                onClick = { onGoalSelected(null); onToggleGoalDropdown() }
            )
        }
        goals.forEach { goal ->
            DropdownMenuItem(
                text = { Text(goal.name) },
                onClick = { onGoalSelected(goal.id); onToggleGoalDropdown() }
            )
        }
    }
}

@Composable
private fun FormSplitSection(
    splitRows: List<SplitRowData>,
    splitRemaining: Double,
    splitTotal: Double,
    categories: List<CategoryEntity>,
    type: String,
    currency: String,
    showSplitCategoryDropdown: Int?,
    onUpdateRow: (Int, SplitRowData) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onToggleDropdown: (Int) -> Unit,
    onAddRow: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Remaining: ${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitRemaining)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (abs(splitRemaining) < 0.01) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
                Text(
                    "Allocated: ${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitTotal)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            splitRows.forEachIndexed { index, row ->
                SplitRowCard(
                    row = row,
                    allCategories = categories,
                    type = type,
                    onUpdate = { updated -> onUpdateRow(index, updated) },
                    onRemove = { onRemoveRow(index) },
                    showDropdown = showSplitCategoryDropdown == row.localId,
                    onToggleDropdown = { onToggleDropdown(row.localId) },
                    remainingAmount = splitRemaining,
                    currencySymbol = CurrencyUtils.getCurrencySymbol(currency)
                )
            }
            TextButton(onClick = onAddRow, modifier = Modifier.height(32.dp)) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Text(" Add Split", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun FormTagSection(
    selectedTags: List<TagEntity>,
    tagQuery: String,
    onTagQueryChange: (String) -> Unit,
    onRemoveTag: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (selectedTags.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                selectedTags.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveTag(tag.id) },
                        label = { Text(tag.name, fontSize = 10.sp) },
                        trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(10.dp)) },
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
        OutlinedTextField(
            value = tagQuery,
            onValueChange = onTagQueryChange,
            label = { Text("Search tags") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun FormActionButtons(
    isEdit: Boolean,
    amountValid: Boolean,
    accountValid: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(8.dp)
        ) { Text("Cancel") }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
            enabled = amountValid && accountValid
        ) { Text(if (isEdit) "Update" else "Save", color = Color.White) }
    }
}

@Composable
private fun FormReceiptPreviewDialog(receiptData: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                if (receiptData.startsWith("data:image")) {
                    val bitmap = remember(receiptData) {
                        runCatching {
                            val b64 = receiptData.substringAfter("base64,")
                            val bytes = Base64.decode(b64, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }.getOrNull()
                    }
                    bitmap?.let {
                        Image(it.asImageBitmap(), "Receipt", modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Icon(
                        Icons.Default.PictureAsPdf, null,
                        modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally)
                    )
                    Text("PDF Receipt", modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun FormCategorySearchDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<CategoryEntity>,
    categoryFilter: String,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
            Column(Modifier.padding(16.dp)) {
                Text("Search Category", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) }
                )
                Spacer(Modifier.height(8.dp))
                val filteredCats = remember(query, categories, categoryFilter, categoryUsageCounts) {
                    categories.filter {
                        it.type == categoryFilter && it.name.contains(query, ignoreCase = true)
                    }.sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
                }
                LazyColumn(Modifier.weight(1f)) {
                    items(filteredCats) { cat ->
                        Row(
                            Modifier.fillMaxWidth()
                                .clickable { onCategorySelected(cat) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(cat.name)
                                if (cat.parentId != null) {
                                    val parent = categories.find { it.id == cat.parentId }
                                    Text(
                                        parent?.name ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
internal fun TransactionTypeHeader(selectedType: String, onTypeSelected: (String) -> Unit) {
    val types = TransactionFormConfig.allTypes
    val categoryColors = LocalCategoryColors.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        types.forEach { item ->
            val isSelected = selectedType == item.id
            val color = when (item.id) {
                "expense" -> categoryColors.expense
                "income" -> categoryColors.income
                "transfer" -> categoryColors.transfer
                "savings" -> categoryColors.savings
                "lend", "borrow" -> categoryColors.lending
                else -> categoryColors.expense
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clickable { onTypeSelected(item.id) }
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent,
                    modifier = Modifier.size(36.dp),
                    border = if (isSelected) BorderStroke(1.dp, color.copy(alpha = 0.5f)) else null
                ) {
                    Icon(
                        item.icon,
                        item.label,
                        tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(6.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun UtilityActionItem(
    icon: ImageVector,
    label: String,
    subtext: String,
    onClick: () -> Unit,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon, null,
                modifier = Modifier.size(20.dp),
                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtext,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 8.sp
                )
            }
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
internal fun CategoryCarousel(
    categories: List<CategoryEntity>,
    type: String,
    selectedCategoryId: Long?,
    expandedCategoryId: Long?,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    onCategoryClick: (CategoryEntity) -> Unit,
    onMoreClick: () -> Unit,
) {
    val filtered = remember(categories, type) { categories.filter { it.type == type } }
    val parents = remember(filtered, categoryUsageCounts) {
        val subsByParent = filtered.filter { it.parentId != null }.groupBy { it.parentId }
        filtered.filter { it.parentId == null }.sortedByDescending { cat ->
            val direct = categoryUsageCounts[cat.id] ?: 0
            val subCounts = subsByParent[cat.id]?.sumOf { categoryUsageCounts[it.id] ?: 0 } ?: 0
            direct + subCounts
        }
    }
    val displayCats = if (expandedCategoryId != null) {
        val subs = filtered.filter { it.parentId == expandedCategoryId }.sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
        val p = parents.firstOrNull { it.id == expandedCategoryId }
        if (p != null) listOf(p) + subs else subs
    } else parents.take(5)

    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayCats.forEach { cat ->
            val isSelected = selectedCategoryId == cat.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp).clickable { onCategoryClick(cat) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp),
                        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                    ) {
                        Box(contentAlignment = Alignment.Center) { CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, fontSize = 20.sp) }
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Default.CheckCircle, null,
                            modifier = Modifier.size(14.dp).background(Color.White, CircleShape),
                            tint = Color.Red
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 8.sp
                )
            }
        }
        if (expandedCategoryId == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(56.dp).clickable { onMoreClick() }
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color(0xFF00897B), modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text("More", style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, fontSize = 8.sp)
            }
        }
    }
}
