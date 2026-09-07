package com.moneymanager.app.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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
import com.moneymanager.domain.ai.TransactionDraft
import com.moneymanager.app.ui.util.accountTypeIcon
import com.moneymanager.app.ui.util.evaluateExpression
import com.moneymanager.data.entity.*

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ── Confidence predicates (used by review banner and field tint logic) ──

internal object ConfidenceKey {
    const val TYPE          = "type"
    const val AMOUNT        = "amount"
    const val DATE          = "date"
    const val ACCOUNT_NAME  = "accountName"
    const val CATEGORY_NAME = "categoryName"
    const val PEER_NAME     = "peerContactName"
    const val DESCRIPTION   = "description"
    const val NOTE          = "note"
}

internal fun showReviewBanner(draft: TransactionDraft?): Boolean = draft?.needsReview == true

internal fun fieldIsLowConfidence(fieldName: String, draft: TransactionDraft?): Boolean =
    draft?.confidence?.get(fieldName) == "low"

data class SplitRowData(
    val localId: Int,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val description: String = "",
    val amount: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
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
    imageAttachmentsEnabled: Boolean = true,
    initialDraft: TransactionDraft? = null,
    onDraftDismiss: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onConfirmEmi: ((TransactionEntity, Int, Double, Boolean, Double) -> Unit)? = null,
) {
    val isEdit = transaction != null
    var aiSuggestedFields by remember { mutableStateOf(emptySet<String>()) }

    // ── Core State ──
    var type by rememberSaveable { mutableStateOf(transaction?.type ?: initialType?.takeIf { it.isNotBlank() } ?: "expense") }

    val filteredAccounts = remember(type, accounts) {
        when (type) {
            "expense", "savings" -> accounts.filter { it.type != "savings" }
            else -> accounts
        }
    }

    val investmentAccounts = remember(accounts) {
        accounts.filter { it.type == "savings" }
    }

    var amount by rememberSaveable {
        mutableStateOf(transaction?.amount?.let { if (it < 0) (-it).toString() else it.toString() } ?: "")
    }

    val isIncomingTransferLeg = (transaction?.type == "transfer") &&
        transaction.note.contains("transfer from", ignoreCase = true)
    var selectedAccountId by rememberSaveable {
        mutableStateOf(
            if (isIncomingTransferLeg) (transaction.toAccountId ?: filteredAccounts.firstOrNull()?.id)
            else (transaction?.accountId ?: filteredAccounts.firstOrNull()?.id)
        )
    }
    var selectedToAccountId by rememberSaveable {
        mutableStateOf(
            if (isIncomingTransferLeg) transaction.accountId
            else (transaction?.toAccountId ?: if (type == "savings") investmentAccounts.firstOrNull()?.id else null)
        )
    }
    var showToAccountDropdown by rememberSaveable { mutableStateOf(false) }

    // ── EMI State ──
    var isEmiEnabled by rememberSaveable { mutableStateOf(false) }
    var emiTenure by rememberSaveable { mutableStateOf("6") }
    var emiInterestRate by rememberSaveable { mutableStateOf("0") }
    var isNoCostEmi by rememberSaveable { mutableStateOf(true) }
    var emiProcessingFee by rememberSaveable { mutableStateOf("0") }

    var selectedCategoryId by rememberSaveable { 
        mutableStateOf(transaction?.subCategoryId ?: transaction?.categoryId) 
    }
    var selectedPeerId by rememberSaveable { mutableStateOf(transaction?.peerContactId) }
    var selectedDate by rememberSaveable { mutableLongStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var description by rememberSaveable { mutableStateOf(transaction?.note ?: "") }
    var selectedGoalId by rememberSaveable { mutableStateOf(transaction?.goalId) }
    var selectedPlatform by rememberSaveable { mutableStateOf(transaction?.investmentPlatform) }

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
    var splitIdCounter by rememberSaveable { mutableIntStateOf(2) }

    // ── Tags State ──
    var tagQuery by rememberSaveable { mutableStateOf("") }
    var selectedTagIds by rememberSaveable {
        mutableStateOf(
            if (transaction?.tagIds?.isNotEmpty() == true)
                transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            else emptySet()
        )
    }

    // ── Receipt State ──
    var receiptData by rememberSaveable { mutableStateOf(transaction?.receiptPath) }
    var showReceiptPreview by rememberSaveable { mutableStateOf(false) }

    // ── UI Modal Toggles ──
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showAccountDropdown by rememberSaveable { mutableStateOf(false) }
    var expectedReturnDate by rememberSaveable { mutableStateOf(transaction?.expectedReturnDate) }
    var showExpectedReturnDatePicker by rememberSaveable { mutableStateOf(false) }
    var expandedCategoryId by rememberSaveable {
        mutableStateOf(
            (transaction?.subCategoryId ?: transaction?.categoryId)?.let { catId ->
                categories.firstOrNull { it.id == catId }?.parentId
            }
        )
    }
    var categorySearchQuery by rememberSaveable { mutableStateOf("") }
    var showCategorySearch by rememberSaveable { mutableStateOf(false) }
    var showDiscardConfirmation by rememberSaveable { mutableStateOf(false) }

    // Secondary Option Dialog Toggles
    var showNoteDialog by rememberSaveable { mutableStateOf(false) }
    var showTagsDialog by rememberSaveable { mutableStateOf(false) }
    var showAttachDialog by rememberSaveable { mutableStateOf(false) }
    var showSplitDialog by rememberSaveable { mutableStateOf(false) }
    var showEmiDialog by rememberSaveable { mutableStateOf(false) }
    var showGoalDialog by rememberSaveable { mutableStateOf(false) }
    var showPlatformDialog by rememberSaveable { mutableStateOf(false) }
    var showPeerDialog by rememberSaveable { mutableStateOf(false) }

    // ── Config & Derived State ──
    val config = remember(type) { TransactionFormConfig.getType(type) }
    val features = config.features
    val categoryFilter = TransactionFormConfig.resolveCategoryType(type)

    val mainAmount = amount.toDoubleOrNull() ?: 0.0
    val splitTotal = splitRows.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val splitRemaining = mainAmount - splitTotal
    val context = LocalContext.current

    // ── Accent Colors (animated on type switch) ──
    val categoryColors = LocalCategoryColors.current
    val colorScheme = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(
        targetValue = when (type) {
            "expense"  -> colorScheme.error
            "income"   -> colorScheme.primary
            "savings"  -> colorScheme.tertiary
            "transfer" -> colorScheme.secondary
            "lend", "borrow" -> categoryColors.lending
            else       -> colorScheme.primary
        },
        animationSpec = tween(300),
        label = "accentColor"
    )
    val accentContainer by animateColorAsState(
        targetValue = when (type) {
            "expense"  -> colorScheme.errorContainer
            "income"   -> colorScheme.primaryContainer
            "savings"  -> colorScheme.tertiaryContainer
            "transfer" -> colorScheme.secondaryContainer
            "lend", "borrow" -> categoryColors.lending.copy(alpha = 0.18f)
            else       -> colorScheme.primaryContainer
        },
        animationSpec = tween(300),
        label = "accentContainer"
    )

    // ── Type Switch Handler ──
    fun onTypeSelected(newType: String) {
        if (newType == type) return
        aiSuggestedFields -= "type"
        type = newType
        selectedCategoryId = null
        expandedCategoryId = null
        selectedPeerId = null
        selectedToAccountId = null
        selectedGoalId = null
        selectedPlatform = null
        expectedReturnDate = null
        splitEnabled = false
        val validIds = when (newType) {
            "expense" -> accounts.filter { it.type != "savings" }
            "savings" -> accounts.filter { it.type == "savings" }
            else -> accounts
        }.map { it.id }
        if (selectedAccountId != null && selectedAccountId !in validIds) {
            selectedAccountId = validIds.firstOrNull()
        }
    }

    // ── Back Navigation Handler ──
    fun handleBackPress() {
        val initialCat = transaction?.subCategoryId ?: transaction?.categoryId
        val initialAcc = transaction?.accountId ?: filteredAccounts.firstOrNull()?.id
        val hasUnsavedData = (amount.isNotEmpty() && amount != "0" && amount != "0.0")
                || description.isNotEmpty()
                || selectedTagIds.isNotEmpty()
                || receiptData != null
                || splitEnabled
                || isEmiEnabled
                || (transaction == null && selectedCategoryId != null)
                || (transaction != null && selectedCategoryId != initialCat)
                || (transaction != null && selectedAccountId != initialAcc)

        if (hasUnsavedData) {
            showDiscardConfirmation = true
        } else {
            aiSuggestedFields = emptySet()
            onDraftDismiss?.invoke()
            onDismiss()
        }
    }

    BackHandler {
        handleBackPress()
    }

    // ── Transaction Builder ──
    fun buildTransaction(): TransactionEntity? {
        val amt = amount.toDoubleOrNull() ?: return null
        if (amt <= 0 || selectedAccountId == null) return null
        val selectedCat = categories.firstOrNull { it.id == selectedCategoryId }
        val effectiveCategoryId =
            if (TransactionFeature.CATEGORY in features) (selectedCat?.parentId ?: selectedCat?.id)
            else null
        val effectiveSubCategoryId =
            if (TransactionFeature.CATEGORY in features) (if (selectedCat?.parentId != null) selectedCat.id else null)
            else null
        return TransactionEntity(
            id = transaction?.id ?: 0,
            accountId = selectedAccountId!!,
            type = type,
            amount = amt,
            categoryId = effectiveCategoryId,
            subCategoryId = effectiveSubCategoryId,
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
            isTransfer = type == "transfer" || type == "savings",
            toAccountId = if (type == "transfer" || type == "savings") selectedToAccountId else null,
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

    fun handleSave() {
        try {
            if (amount.contains("+") || amount.contains("-") || amount.contains("*") || amount.contains("/")) {
                val res = evaluateExpression(amount)
                amount = if (res % 1.0 == 0.0) "%.0f".format(Locale.US, res) else "%.2f".format(Locale.US, res)
            }
        } catch (_: Exception) {}

        val tx = buildTransaction()
        if (tx != null) {
            if (isEmiEnabled && type == "expense" && onConfirmEmi != null) {
                val tenure = emiTenure.toIntOrNull() ?: 6
                val rate = emiInterestRate.toDoubleOrNull() ?: 0.0
                val fee = emiProcessingFee.toDoubleOrNull() ?: 0.0
                onConfirmEmi(tx, tenure, rate, isNoCostEmi, fee)
            } else {
                val children = if (splitEnabled && TransactionFeature.SPLIT in features)
                    buildSplitChildren(tx.id) else null
                onConfirm(tx, children)
            }
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

    // ── Draft Population ──
    var draftApplied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialDraft) {
        if (initialDraft != null && !draftApplied) {
            draftApplied = true
            initialDraft.typeId?.let { type = it }
            initialDraft.amount?.let { amount = if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() }
            initialDraft.categoryId?.let { selectedCategoryId = it }
            initialDraft.accountId?.let { selectedAccountId = it }
            initialDraft.peerContactId?.let { selectedPeerId = it }
            if (initialDraft.tagIds.isNotEmpty()) {
                selectedTagIds = initialDraft.tagIds.toSet()
            }
            val textNote = initialDraft.description ?: initialDraft.note
            if (textNote != null) {
                description = textNote
            }
            initialDraft.date?.let { selectedDate = it }
            initialDraft.receiptPath?.let { receiptData = it }

            aiSuggestedFields = buildSet {
                if (initialDraft.typeId != null) add("type")
                if (initialDraft.amount != null) add("amount")
                if (initialDraft.categoryId != null) add("category")
                if (initialDraft.accountId != null) add("account")
                if (initialDraft.peerContactId != null) add("peer")
                if (initialDraft.tagIds.isNotEmpty()) add("tags")
                if (initialDraft.description != null || initialDraft.note != null) add("note")
                if (initialDraft.date != null) add("date")
                if (initialDraft.receiptPath != null) add("receipt")
            }
        }
    }

    // ── File Picker ──
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { 
            receiptData = FileHelper.saveReceipt(context, it)
        }
    }

    // ── Overlay Picker Dialogs ──
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        aiSuggestedFields -= "date"
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
    Dialog(
        onDismissRequest = { handleBackPress() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                // Top Bar with Back Arrow (No Cancel Button)
                DialogTopBar(
                    title = "${if (isEdit) "Edit" else "Add"} ${config.displayName}",
                    accentColor = accentColor,
                    onBackClick = { handleBackPress() }
                )

                // Transaction Type Tabs
                TransactionTypeHeader(selectedType = type, onTypeSelected = ::onTypeSelected)

                Column(
                    Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source / AI Review Banner
                    if (initialDraft?.sourceType != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Draft from ${initialDraft.sourceType.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (showReviewBanner(initialDraft)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Some fields need review — AI confidence low.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }

                    // 1. Amount, Date, Account Card
                    FormAmountDateAccountCard(
                        amount = amount,
                        currency = currency,
                        selectedDate = selectedDate,
                        selectedAccountId = selectedAccountId,
                        selectedToAccountId = selectedToAccountId,
                        selectedPeerId = selectedPeerId,
                        type = type,
                        accounts = accounts,
                        peers = peers,
                        expectedReturnDate = expectedReturnDate,
                        showExpectedReturnDate = TransactionFeature.RETURN_DATE in features,
                        accentColor = accentColor,
                        onDateClick = { showDatePicker = true },
                        onAccountClick = { showAccountDropdown = true },
                        onToAccountClick = { showToAccountDropdown = true },
                        onPeerClick = { showPeerDialog = true },
                        onExpectedReturnDateClick = { showExpectedReturnDatePicker = true }
                    )

                    // Account Dropdown Menus
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
                                onClick = {
                                    aiSuggestedFields -= "account"
                                    selectedAccountId = acc.id
                                    showAccountDropdown = false
                                }
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showToAccountDropdown,
                        onDismissRequest = { showToAccountDropdown = false }
                    ) {
                        accounts.filter { it.id != selectedAccountId }.forEach { acc ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(accountTypeIcon(acc.type), null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(acc.name)
                                    }
                                },
                                onClick = {
                                    selectedToAccountId = acc.id
                                    showToAccountDropdown = false
                                }
                            )
                        }
                    }

                    // 2. Category Section (Directly above keypad)
                    if (TransactionFeature.CATEGORY in features) {
                        FormCategorySection(
                            categories = categories,
                            categoryFilter = categoryFilter,
                            selectedCategoryId = selectedCategoryId,
                            expandedCategoryId = expandedCategoryId,
                            categoryUsageCounts = categoryUsageCounts,
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                            onCategoryClick = { cat ->
                                aiSuggestedFields -= "category"
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

                    Spacer(Modifier.height(8.dp))

                    // 3. Keypad (Left) + More Side Panel (Right) immediately below Category
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(312.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left 4-Column Keypad (Fixed in place, wider)
                        NumericKeypad(
                            modifier = Modifier.weight(2.3f),
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                            saveButtonText = "Save ${config.displayName}",
                            saveButtonEnabled = amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0 && selectedAccountId != null,
                            onNumberClick = { num ->
                                aiSuggestedFields -= "amount"
                                if (num == ".") {
                                    val lastNumber = amount.split('+', '-', '*', '/').last()
                                    if (!lastNumber.contains(".")) {
                                        amount += if (lastNumber.isEmpty()) "0." else "."
                                    }
                                } else if (num in listOf("+", "-", "*", "/")) {
                                    if (amount.isNotEmpty() && !amount.last().isDigit() && amount.last() != '.')
                                        amount = amount.dropLast(1) + num
                                    else if (amount.isNotEmpty()) amount += num
                                } else {
                                    if (amount == "0") amount = num else amount += num
                                }
                            },
                            onDeleteClick = {
                                aiSuggestedFields -= "amount"
                                if (amount.isNotEmpty()) amount = amount.dropLast(1)
                            },
                            onClearClick = {
                                aiSuggestedFields -= "amount"
                                amount = ""
                            },
                            onEvaluate = {
                                try {
                                    val res = evaluateExpression(amount)
                                    amount = if (res % 1.0 == 0.0) "%.0f".format(Locale.US, res) else "%.2f".format(Locale.US, res)
                                } catch (_: Exception) {}
                            },
                            onSaveClick = { handleSave() }
                        )

                        // Vertical Divider Line
                        VerticalDivider(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                        )

                        // Right More Secondary Options Side Panel
                        MoreOptionsSidePanel(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            type = type,
                            features = features,
                            note = description,
                            selectedTagIds = selectedTagIds,
                            tags = tags,
                            receiptData = receiptData,
                            splitEnabled = splitEnabled,
                            splitRows = splitRows,
                            isEmiEnabled = isEmiEnabled,
                            emiTenure = emiTenure,
                            selectedGoalId = selectedGoalId,
                            goals = goals,
                            selectedPlatform = selectedPlatform,
                            selectedPeerId = selectedPeerId,
                            peers = peers,
                            expectedReturnDate = expectedReturnDate,
                            accentColor = accentColor,
                            imageAttachmentsEnabled = imageAttachmentsEnabled,
                            onOpenNote = { showNoteDialog = true },
                            onOpenTags = { showTagsDialog = true },
                            onOpenAttach = { showAttachDialog = true },
                            onOpenSplit = { showSplitDialog = true },
                            onOpenEmi = { showEmiDialog = true },
                            onOpenGoal = { showGoalDialog = true },
                            onOpenPlatform = { showPlatformDialog = true },
                            onOpenPeer = { showPeerDialog = true },
                            onOpenReturnDate = { showExpectedReturnDatePicker = true }
                        )
                    }

                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }

    // ── Secondary Field Modal Dialogs ──

    // Note Dialog
    if (showNoteDialog) {
        SecondaryNoteDialog(
            note = description,
            onNoteChange = { description = it },
            onDismiss = { showNoteDialog = false }
        )
    }

    // Tags Dialog
    if (showTagsDialog) {
        SecondaryTagsDialog(
            tags = tags,
            selectedTagIds = selectedTagIds,
            tagQuery = tagQuery,
            onQueryChange = { tagQuery = it },
            onToggleTag = { tagId ->
                selectedTagIds = if (tagId in selectedTagIds) selectedTagIds - tagId else selectedTagIds + tagId
            },
            onDismiss = { showTagsDialog = false }
        )
    }

    // Attach Dialog
    if (showAttachDialog) {
        SecondaryAttachDialog(
            receiptData = receiptData,
            onPickFile = { filePicker.launch("image/*") },
            onRemoveFile = { receiptData = null },
            onPreviewFile = { showReceiptPreview = true },
            onDismiss = { showAttachDialog = false }
        )
    }

    // Split Dialog
    if (showSplitDialog) {
        SecondarySplitDialog(
            splitEnabled = splitEnabled,
            splitRows = splitRows,
            splitRemaining = splitRemaining,
            splitTotal = splitTotal,
            categories = categories,
            type = type,
            currency = currency,
            onToggleSplitEnabled = { splitEnabled = it },
            onUpdateRow = { index, updated ->
                splitRows = splitRows.toMutableList().also { it[index] = updated }
            },
            onRemoveRow = { index ->
                splitRows = splitRows.toMutableList().also { it.removeAt(index) }
            },
            onAddRow = {
                splitRows = splitRows + SplitRowData(splitIdCounter++)
            },
            onDismiss = { showSplitDialog = false }
        )
    }

    // EMI Dialog
    if (showEmiDialog) {
        SecondaryEmiDialog(
            isEmiEnabled = isEmiEnabled,
            emiTenure = emiTenure,
            emiInterestRate = emiInterestRate,
            isNoCostEmi = isNoCostEmi,
            emiProcessingFee = emiProcessingFee,
            currency = currency,
            onEmiEnabledChange = { isEmiEnabled = it },
            onTenureChange = { emiTenure = it },
            onRateChange = { emiInterestRate = it },
            onNoCostChange = { isNoCostEmi = it; if (it) emiInterestRate = "0" },
            onFeeChange = { emiProcessingFee = it },
            onDismiss = { showEmiDialog = false }
        )
    }

    // Goal Dialog
    if (showGoalDialog) {
        SecondaryGoalDialog(
            goals = goals,
            selectedGoalId = selectedGoalId,
            onGoalSelected = { selectedGoalId = it; showGoalDialog = false },
            onDismiss = { showGoalDialog = false }
        )
    }

    // Platform Dialog
    if (showPlatformDialog) {
        SecondaryPlatformDialog(
            platform = selectedPlatform ?: "",
            onPlatformChange = { selectedPlatform = it.ifEmpty { null } },
            onDismiss = { showPlatformDialog = false }
        )
    }

    // Peer Dialog
    if (showPeerDialog) {
        SecondaryPeerDialog(
            peers = peers,
            selectedPeerId = selectedPeerId,
            onPeerSelected = { selectedPeerId = it; showPeerDialog = false },
            onDismiss = { showPeerDialog = false }
        )
    }

    // Category Search Dialog
    if (showCategorySearch && TransactionFeature.CATEGORY in features) {
        FormCategorySearchDialog(
            query = categorySearchQuery,
            onQueryChange = { categorySearchQuery = it },
            categories = categories,
            categoryFilter = categoryFilter,
            categoryUsageCounts = categoryUsageCounts,
            onCategorySelected = { cat ->
                aiSuggestedFields -= "category"
                selectedCategoryId = cat.id
                expandedCategoryId = cat.parentId ?: cat.id
                showCategorySearch = false
            },
            onDismiss = { showCategorySearch = false }
        )
    }

    // Discard Confirmation Dialog
    if (showDiscardConfirmation) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirmation = false },
            title = { Text("Discard unsaved changes?") },
            text = { Text("You have unsaved changes in this transaction. Are you sure you want to exit without saving?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirmation = false
                        aiSuggestedFields = emptySet()
                        onDraftDismiss?.invoke()
                        onDismiss()
                    }
                ) {
                    Text("Discard", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardConfirmation = false }) {
                    Text("Keep editing")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════
//  Top Bar & Amount/Date/Account Components
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DialogTopBar(
    title: String,
    accentColor: Color,
    onBackClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        IconButton(onClick = {}) {
            Icon(Icons.Default.MoreVert, "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FormAmountDateAccountCard(
    amount: String,
    currency: String,
    selectedDate: Long,
    selectedAccountId: Long?,
    selectedToAccountId: Long?,
    selectedPeerId: Long?,
    type: String,
    accounts: List<AccountEntity>,
    peers: List<PeerContact>,
    expectedReturnDate: Long?,
    showExpectedReturnDate: Boolean,
    accentColor: Color,
    onDateClick: () -> Unit,
    onAccountClick: () -> Unit,
    onToAccountClick: () -> Unit,
    onPeerClick: () -> Unit,
    onExpectedReturnDateClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Amount — left, prominent
            Column(
                modifier = Modifier
                    .weight(1.3f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        CurrencyUtils.getCurrencySymbol(currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = amount.ifEmpty { "0" },
                        fontWeight = FontWeight.ExtraBold,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 34.sp,
                        lineHeight = 38.sp
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 2.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Date & Account — right, compact stacked tiles
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CompactInfoTile(
                    icon = Icons.Default.CalendarToday,
                    label = "Date",
                    value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    accentColor = accentColor,
                    onClick = onDateClick
                )
                CompactInfoTile(
                    icon = Icons.Default.AccountBalance,
                    label = if (type == "transfer" || type == "savings") "From Account" else "Account",
                    value = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account",
                    accentColor = accentColor,
                    onClick = onAccountClick
                )
                if (type == "transfer" || type == "savings") {
                    CompactInfoTile(
                        icon = Icons.Default.SwapHoriz,
                        label = if (type == "savings") "Investment Account" else "To Account",
                        value = accounts.find { it.id == selectedToAccountId }?.name ?: "Select Account",
                        accentColor = accentColor,
                        onClick = onToAccountClick
                    )
                }
                if (type == "lend" || type == "borrow") {
                    CompactInfoTile(
                        icon = Icons.Default.Person,
                        label = "Person",
                        value = peers.find { it.id == selectedPeerId }?.effectiveDisplayName ?: "Select Person",
                        accentColor = accentColor,
                        onClick = onPeerClick
                    )
                }
                if (showExpectedReturnDate && (type == "lend" || type == "borrow")) {
                    CompactInfoTile(
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        label = "Return Date",
                        value = expectedReturnDate?.let {
                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Not set",
                        accentColor = accentColor,
                        onClick = onExpectedReturnDateClick
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactInfoTile(
    icon: ImageVector,
    label: String,
    value: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, modifier = Modifier.size(13.dp), tint = accentColor)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Category Section & Shortcuts Carousel
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FormCategorySection(
    categories: List<CategoryEntity>,
    categoryFilter: String,
    selectedCategoryId: Long?,
    expandedCategoryId: Long?,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    accentColor: Color,
    accentContainer: Color,
    onCategoryClick: (CategoryEntity) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (expandedCategoryId != null) {
                    TextButton(
                        onClick = onBackClick,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(12.dp), tint = accentColor)
                        Spacer(Modifier.width(2.dp))
                        Text("Back", style = MaterialTheme.typography.labelSmall, color = accentColor)
                    }
                }
                TextButton(
                    onClick = onMoreClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(
                        "See all >",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
        CategoryCarousel(
            categories = categories,
            type = categoryFilter,
            selectedCategoryId = selectedCategoryId,
            expandedCategoryId = expandedCategoryId,
            categoryUsageCounts = categoryUsageCounts,
            accentColor = accentColor,
            accentContainer = accentContainer,
            onCategoryClick = onCategoryClick,
            onMoreClick = onMoreClick
        )
    }
}

@Composable
internal fun CategoryCarousel(
    categories: List<CategoryEntity>,
    type: String,
    selectedCategoryId: Long?,
    expandedCategoryId: Long?,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    accentContainer: Color = MaterialTheme.colorScheme.primaryContainer,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    onCategoryClick: (CategoryEntity) -> Unit,
    onMoreClick: () -> Unit,
) {
    val filtered = remember(categories, type) { categories.filter { it.type == type } }
    val sortedParents = remember(filtered, categoryUsageCounts) {
        val subsByParent = filtered.filter { it.parentId != null }.groupBy { it.parentId }
        filtered.filter { it.parentId == null }.sortedByDescending { cat ->
            val direct = categoryUsageCounts[cat.id] ?: 0
            val subCounts = subsByParent[cat.id]?.sumOf { categoryUsageCounts[it.id] ?: 0 } ?: 0
            direct + subCounts
        }
    }

    val displayCats = remember(sortedParents, expandedCategoryId, selectedCategoryId, filtered) {
        if (expandedCategoryId != null) {
            val subs = filtered.filter { it.parentId == expandedCategoryId }
                .sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
            val parent = sortedParents.firstOrNull { it.id == expandedCategoryId }
            if (parent != null) listOf(parent) + subs else subs
        } else {
            val top5 = sortedParents.take(5).toMutableList()
            if (selectedCategoryId != null && top5.none { it.id == selectedCategoryId }) {
                val selectedCat = filtered.firstOrNull { it.id == selectedCategoryId }
                if (selectedCat != null) {
                    val catToAdd = if (selectedCat.parentId != null) filtered.firstOrNull { it.id == selectedCat.parentId } ?: selectedCat else selectedCat
                    if (top5.size == 5) top5[4] = catToAdd
                    else top5.add(catToAdd)
                }
            }
            top5
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displayCats.forEach { cat ->
            val isSelected = selectedCategoryId == cat.id || expandedCategoryId == cat.id
            val itemBg by animateColorAsState(
                if (isSelected) accentContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                animationSpec = tween(200),
                label = "catBg"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onCategoryClick(cat) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = itemBg,
                        modifier = Modifier.size(52.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, colorIndex = cat.colorIndex, fontSize = 22.sp)
                        }
                    }
                    if (isSelected) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(1.5.dp)
                                .background(accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, null,
                                modifier = Modifier.size(8.dp),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }

        if (expandedCategoryId == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(60.dp)
                    .clickable { onMoreClick() }
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MoreHoriz, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Right More Options Side Panel
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MoreOptionsSidePanel(
    modifier: Modifier = Modifier,
    type: String,
    features: Set<TransactionFeature>,
    note: String,
    selectedTagIds: Set<Long>,
    tags: List<TagEntity>,
    receiptData: String?,
    splitEnabled: Boolean,
    splitRows: List<SplitRowData>,
    isEmiEnabled: Boolean,
    emiTenure: String,
    selectedGoalId: Long?,
    goals: List<GoalEntity>,
    selectedPlatform: String?,
    selectedPeerId: Long?,
    peers: List<PeerContact>,
    expectedReturnDate: Long?,
    accentColor: Color,
    imageAttachmentsEnabled: Boolean,
    onOpenNote: () -> Unit,
    onOpenTags: () -> Unit,
    onOpenAttach: () -> Unit,
    onOpenSplit: () -> Unit,
    onOpenEmi: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenPlatform: () -> Unit,
    onOpenPeer: () -> Unit,
    onOpenReturnDate: () -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag Handle
        Box(
            modifier = Modifier
                .padding(top = 2.dp, bottom = 2.dp)
                .width(26.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
        )

        // Note Card
        if (TransactionFeature.NOTE in features) {
            MoreOptionCard(
                icon = Icons.AutoMirrored.Filled.Notes,
                title = "Note",
                subtitle = note.ifBlank { "Add note" },
                isActive = note.isNotBlank(),
                accentColor = accentColor,
                onClick = onOpenNote
            )
        }

        // Tags Card
        if (TransactionFeature.TAGS in features) {
            val count = selectedTagIds.size
            val tagSubtitle = when (count) {
                0 -> "Add tags"
                1 -> tags.find { it.id in selectedTagIds }?.name ?: "1 tag"
                else -> "$count tags"
            }
            MoreOptionCard(
                icon = Icons.Default.LocalOffer,
                title = "Tags",
                subtitle = tagSubtitle,
                isActive = count > 0,
                accentColor = accentColor,
                onClick = onOpenTags
            )
        }

        // Attach Card
        if (imageAttachmentsEnabled && TransactionFeature.RECEIPT in features) {
            MoreOptionCard(
                icon = Icons.Default.AttachFile,
                title = "Attach",
                subtitle = if (receiptData == null) "Add file" else "1 file",
                isActive = receiptData != null,
                accentColor = accentColor,
                onClick = onOpenAttach
            )
        }

        // Split Card
        if (TransactionFeature.SPLIT in features) {
            MoreOptionCard(
                icon = Icons.AutoMirrored.Filled.CallSplit,
                title = "Split",
                subtitle = if (!splitEnabled) "Off" else "${splitRows.size} splits",
                isActive = splitEnabled,
                accentColor = accentColor,
                onClick = onOpenSplit
            )
        }

        // Pay via EMI Card (Expense Only)
        if (type == "expense") {
            MoreOptionCard(
                icon = Icons.Default.CreditCard,
                title = "Pay via EMI",
                subtitle = if (!isEmiEnabled) "Off" else "$emiTenure mos",
                isActive = isEmiEnabled,
                accentColor = accentColor,
                onClick = onOpenEmi
            )
        }

        // Goal Card
        if (TransactionFeature.GOAL in features) {
            val goalName = goals.find { it.id == selectedGoalId }?.name
            MoreOptionCard(
                icon = Icons.Default.Flag,
                title = "Goal",
                subtitle = goalName ?: "Select goal",
                isActive = goalName != null,
                accentColor = accentColor,
                onClick = onOpenGoal
            )
        }

        // Platform Card
        if (TransactionFeature.PLATFORM in features) {
            MoreOptionCard(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                title = "Platform",
                subtitle = if (selectedPlatform.isNullOrBlank()) "Add platform" else selectedPlatform,
                isActive = !selectedPlatform.isNullOrBlank(),
                accentColor = accentColor,
                onClick = onOpenPlatform
            )
        }

        // Person Card (Lend / Borrow)
        if (TransactionFeature.PEER in features) {
            val peerName = peers.find { it.id == selectedPeerId }?.effectiveDisplayName
            MoreOptionCard(
                icon = Icons.Default.Person,
                title = "Person",
                subtitle = peerName ?: "Select person",
                isActive = peerName != null,
                accentColor = accentColor,
                onClick = onOpenPeer
            )
        }

        // Return Date Card (Lend / Borrow)
        if (TransactionFeature.RETURN_DATE in features) {
            val returnDateStr = expectedReturnDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) }
            MoreOptionCard(
                icon = Icons.Default.CalendarToday,
                title = "Return Date",
                subtitle = returnDateStr ?: "Not set",
                isActive = returnDateStr != null,
                accentColor = accentColor,
                onClick = onOpenReturnDate
            )
        }
    }
}

@Composable
private fun MoreOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isActive: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) accentColor.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(
            0.5.dp,
            if (isActive) accentColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = if (isActive) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Secondary Field Dialog Overlays
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SecondaryNoteDialog(
    note: String,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Note", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Add note...") },
                    maxLines = 3
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Done") }
                }
            }
        }
    }
}

@Composable
private fun SecondaryTagsDialog(
    tags: List<TagEntity>,
    selectedTagIds: Set<Long>,
    tagQuery: String,
    onQueryChange: (String) -> Unit,
    onToggleTag: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = tagQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search tags...") },
                    singleLine = true
                )
                val filtered = remember(tags, tagQuery) {
                    if (tagQuery.isBlank()) tags else tags.filter { it.name.contains(tagQuery, ignoreCase = true) }
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(filtered) { tag ->
                        val isSelected = tag.id in selectedTagIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleTag(tag.id) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                            Checkbox(checked = isSelected, onCheckedChange = { onToggleTag(tag.id) })
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SecondaryAttachDialog(
    receiptData: String?,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onPreviewFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Attachment / Receipt", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (receiptData != null) {
                    Text("1 File Attached", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onPreviewFile) { Text("View Receipt") }
                        Button(
                            onClick = { onRemoveFile(); onDismiss() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("Remove") }
                    }
                } else {
                    Button(onClick = { onPickFile(); onDismiss() }) {
                        Icon(Icons.Default.AttachFile, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Pick Photo / File")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

@Composable
private fun SecondarySplitDialog(
    splitEnabled: Boolean,
    splitRows: List<SplitRowData>,
    splitRemaining: Double,
    splitTotal: Double,
    categories: List<CategoryEntity>,
    type: String,
    currency: String,
    onToggleSplitEnabled: (Boolean) -> Unit,
    onUpdateRow: (Int, SplitRowData) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onAddRow: () -> Unit,
    onDismiss: () -> Unit,
) {
    var activeDropdownRowId by remember { mutableStateOf<Int?>(null) }
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split Transaction", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = splitEnabled, onCheckedChange = onToggleSplitEnabled)
                }

                if (splitEnabled) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "Remaining: ${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitRemaining)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "Allocated: ${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitTotal)}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(splitRows.size) { index ->
                            val row = splitRows[index]
                            SplitRowCard(
                                row = row,
                                allCategories = categories,
                                type = type,
                                onUpdate = { updated -> onUpdateRow(index, updated) },
                                onRemove = { onRemoveRow(index) },
                                showDropdown = activeDropdownRowId == row.localId,
                                onToggleDropdown = {
                                    activeDropdownRowId = if (activeDropdownRowId == row.localId) null else row.localId
                                },
                                remainingAmount = splitRemaining,
                                currencySymbol = CurrencyUtils.getCurrencySymbol(currency)
                            )
                        }
                    }

                    TextButton(onClick = onAddRow) {
                        Icon(Icons.Default.Add, null)
                        Text(" Add Split Row")
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SecondaryEmiDialog(
    isEmiEnabled: Boolean,
    emiTenure: String,
    emiInterestRate: String,
    isNoCostEmi: Boolean,
    emiProcessingFee: String,
    currency: String,
    onEmiEnabledChange: (Boolean) -> Unit,
    onTenureChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onNoCostChange: (Boolean) -> Unit,
    onFeeChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Pay via EMI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Switch(checked = isEmiEnabled, onCheckedChange = onEmiEnabledChange)
                }

                if (isEmiEnabled) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = emiTenure,
                            onValueChange = onTenureChange,
                            label = { Text("Tenure (Mos)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = emiInterestRate,
                            onValueChange = onRateChange,
                            label = { Text("Interest Rate (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("No-Cost EMI")
                        Checkbox(checked = isNoCostEmi, onCheckedChange = onNoCostChange)
                    }

                    OutlinedTextField(
                        value = emiProcessingFee,
                        onValueChange = onFeeChange,
                        label = { Text("Processing Fee (${CurrencyUtils.getCurrencySymbol(currency)})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SecondaryGoalDialog(
    goals: List<GoalEntity>,
    selectedGoalId: Long?,
    onGoalSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().clickable { onGoalSelected(null) }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("None")
                    RadioButton(selected = selectedGoalId == null, onClick = { onGoalSelected(null) })
                }
                goals.forEach { goal ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onGoalSelected(goal.id) }.padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(goal.name)
                        RadioButton(selected = selectedGoalId == goal.id, onClick = { onGoalSelected(goal.id) })
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun SecondaryPlatformDialog(
    platform: String,
    onPlatformChange: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Investment Platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = platform,
                    onValueChange = onPlatformChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Zerodha, Groww") },
                    singleLine = true
                )
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Done") }
            }
        }
    }
}

@Composable
private fun SecondaryPeerDialog(
    peers: List<PeerContact>,
    selectedPeerId: Long?,
    onPeerSelected: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.6f), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select Person", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(
                    Modifier.fillMaxWidth().clickable { onPeerSelected(null) }.padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("None")
                    RadioButton(selected = selectedPeerId == null, onClick = { onPeerSelected(null) })
                }
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(peers) { peer ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onPeerSelected(peer.id) }.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(peer.effectiveDisplayName)
                            RadioButton(selected = selectedPeerId == peer.id, onClick = { onPeerSelected(peer.id) })
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
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
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Search Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                            CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, colorIndex = cat.colorIndex, fontSize = 24.sp)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(cat.name, fontWeight = FontWeight.Medium)
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
                } else if (File(receiptData).exists()) {
                    val bitmap = remember(receiptData) {
                        BitmapFactory.decodeFile(receiptData)
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

// ═══════════════════════════════════════════════════════════════
//  Transaction Type Tabs Header
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun TransactionTypeHeader(selectedType: String, onTypeSelected: (String) -> Unit) {
    val types = TransactionFormConfig.allTypes
    val categoryColors = LocalCategoryColors.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        types.forEach { item ->
            val isSelected = selectedType == item.id
            val typeAccent = when (item.id) {
                "expense"  -> colorScheme.error
                "income"   -> colorScheme.primary
                "savings"  -> colorScheme.tertiary
                "transfer" -> colorScheme.secondary
                "lend", "borrow" -> categoryColors.lending
                else       -> colorScheme.primary
            }
            val typeContainer = when (item.id) {
                "expense"  -> colorScheme.errorContainer
                "income"   -> colorScheme.primaryContainer
                "savings"  -> colorScheme.tertiaryContainer
                "transfer" -> colorScheme.secondaryContainer
                "lend", "borrow" -> categoryColors.lending.copy(alpha = 0.18f)
                else       -> colorScheme.primaryContainer
            }

            val bgColor by animateColorAsState(
                if (isSelected) typeContainer else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                if (isSelected) typeAccent else colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                animationSpec = tween(220),
                label = "tabContent"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable { onTypeSelected(item.id) }
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
