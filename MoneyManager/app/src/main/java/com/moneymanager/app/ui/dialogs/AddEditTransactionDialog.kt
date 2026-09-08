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
    var isRecurring by rememberSaveable { mutableStateOf(transaction?.isRecurring == true) }

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
    var showAccountPickerSheet by rememberSaveable { mutableStateOf(false) }
    var accountPickerTarget by rememberSaveable { mutableStateOf("account") }
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
    var showSpecialFeaturesSheet by rememberSaveable { mutableStateOf(false) }

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

    val hasActiveSpecialFeatures = remember(
        isEmiEnabled, description, selectedTagIds, receiptData,
        splitEnabled, selectedGoalId, selectedPeerId, expectedReturnDate
    ) {
        isEmiEnabled || description.isNotBlank() || selectedTagIds.isNotEmpty() ||
                receiptData != null || splitEnabled || selectedGoalId != null ||
                selectedPeerId != null || expectedReturnDate != null
    }

    // ── Accent Colors (animated on type switch) ──
    val categoryColors = LocalCategoryColors.current
    val colorScheme = MaterialTheme.colorScheme
    val accentColor by animateColorAsState(
        targetValue = when (type) {
            "expense"  -> Color(0xFFE53935)
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
            "expense"  -> Color(0xFF4A1515)
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
            isRecurring = isRecurring,
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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Top Bar with Back Arrow
                DialogTopBar(
                    title = "${if (isEdit) "Edit" else "Add"} ${config.displayName}",
                    accentColor = accentColor,
                    onBackClick = { handleBackPress() }
                )

                // Transaction Type Tabs (Expense, Income, Savings, Transfer, Lending)
                TransactionTypeHeader(selectedType = type, onTypeSelected = ::onTypeSelected)

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
                    onAccountClick = {
                        accountPickerTarget = "account"
                        showAccountPickerSheet = true
                    },
                    onToAccountClick = {
                        accountPickerTarget = "to"
                        showAccountPickerSheet = true
                    },
                    onPeerClick = { showPeerDialog = true },
                    onExpectedReturnDateClick = { showExpectedReturnDatePicker = true }
                )

                if (showAccountPickerSheet) {
                    val isToAccount = accountPickerTarget == "to"
                    val sheetTitle = when {
                        isToAccount && type == "savings" -> "Select Investment Account"
                        isToAccount -> "Select To Account"
                        type == "transfer" || type == "savings" -> "Select From Account"
                        else -> "Select Account"
                    }
                    val targetAccounts = if (isToAccount) {
                        if (type == "savings") investmentAccounts else accounts.filter { it.id != selectedAccountId }
                    } else {
                        filteredAccounts.ifEmpty { accounts }
                    }
                    val currentSelectedId = if (isToAccount) selectedToAccountId else selectedAccountId

                    AccountPickerSheet(
                        title = sheetTitle,
                        accounts = targetAccounts,
                        selectedAccountId = currentSelectedId,
                        currency = currency,
                        accentColor = accentColor,
                        accentContainer = accentContainer,
                        onAccountSelected = { acc ->
                            if (isToAccount) {
                                selectedToAccountId = acc.id
                            } else {
                                aiSuggestedFields -= "account"
                                selectedAccountId = acc.id
                            }
                            showAccountPickerSheet = false
                        },
                        onDismiss = { showAccountPickerSheet = false }
                    )
                }

                // 2. Category Section with Special Features Trigger
                if (TransactionFeature.CATEGORY in features) {
                    FormCategorySection(
                        categories = categories,
                        categoryFilter = categoryFilter,
                        selectedCategoryId = selectedCategoryId,
                        expandedCategoryId = expandedCategoryId,
                        categoryUsageCounts = categoryUsageCounts,
                        hasActiveSpecialFeatures = hasActiveSpecialFeatures,
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
                        onMoreClick = { showCategorySearch = true },
                        onOpenSpecialFeatures = { showSpecialFeaturesSheet = true }
                    )
                }

                // 3. Keypad Fixed Directly Below Category Section
                NumericKeypad(
                    modifier = Modifier.fillMaxWidth(),
                    accentColor = accentColor,
                    accentContainer = accentContainer,
                    saveButtonText = "Save",
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
                    onCancelClick = { handleBackPress() },
                    onSaveClick = { handleSave() }
                )
            }
        }
    }

    // ── Unified Special Features Bottom Sheet ──
    if (showSpecialFeaturesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpecialFeaturesSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            SpecialFeaturesBottomSheetContent(
                type = type,
                features = features,
                isEmiEnabled = isEmiEnabled,
                emiTenure = emiTenure,
                note = description,
                selectedTagIds = selectedTagIds,
                tags = tags,
                receiptData = receiptData,
                splitEnabled = splitEnabled,
                splitRows = splitRows,
                selectedGoalId = selectedGoalId,
                goals = goals,
                selectedPeerId = selectedPeerId,
                peers = peers,
                expectedReturnDate = expectedReturnDate,
                accentColor = accentColor,
                imageAttachmentsEnabled = imageAttachmentsEnabled,
                onEmiToggle = { enabled ->
                    isEmiEnabled = enabled
                    if (enabled) showEmiDialog = true
                },
                onNoteChange = { description = it },
                onToggleTag = { tagId ->
                    selectedTagIds = if (tagId in selectedTagIds) selectedTagIds - tagId else selectedTagIds + tagId
                },
                onAddReceipt = { filePicker.launch("image/*") },
                onRemoveReceipt = { receiptData = null },
                onPreviewReceipt = { showReceiptPreview = true },
                onOpenSplit = { showSplitDialog = true },
                onOpenGoal = { showGoalDialog = true },
                onOpenPeer = { showPeerDialog = true },
                onOpenReturnDate = { showExpectedReturnDatePicker = true },
                onOpenTags = { showTagsDialog = true },
                onDismiss = { showSpecialFeaturesSheet = false }
            )
        }
    }

    // ── Secondary Field Modal Dialogs ──

    if (showNoteDialog) {
        SecondaryNoteSheet(
            note = description,
            accentColor = accentColor,
            onNoteChange = { description = it },
            onDismiss = { showNoteDialog = false }
        )
    }

    if (showTagsDialog) {
        SecondaryTagsSheet(
            tags = tags,
            selectedTagIds = selectedTagIds,
            tagQuery = tagQuery,
            accentColor = accentColor,
            onQueryChange = { tagQuery = it },
            onToggleTag = { tagId ->
                selectedTagIds = if (tagId in selectedTagIds) selectedTagIds - tagId else selectedTagIds + tagId
            },
            onDismiss = { showTagsDialog = false }
        )
    }

    if (showAttachDialog) {
        SecondaryAttachSheet(
            receiptData = receiptData,
            accentColor = accentColor,
            onPickFile = { filePicker.launch("image/*") },
            onRemoveFile = { receiptData = null },
            onPreviewFile = { showReceiptPreview = true },
            onDismiss = { showAttachDialog = false }
        )
    }

    if (showSplitDialog) {
        SecondarySplitSheet(
            splitEnabled = splitEnabled,
            splitRows = splitRows,
            splitRemaining = splitRemaining,
            splitTotal = splitTotal,
            categories = categories,
            type = type,
            currency = currency,
            accentColor = accentColor,
            accentContainer = accentContainer,
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

    if (showEmiDialog) {
        SecondaryEmiSheet(
            isEmiEnabled = isEmiEnabled,
            emiTenure = emiTenure,
            emiInterestRate = emiInterestRate,
            isNoCostEmi = isNoCostEmi,
            emiProcessingFee = emiProcessingFee,
            currency = currency,
            mainAmount = mainAmount,
            accentColor = accentColor,
            onEmiEnabledChange = { isEmiEnabled = it },
            onTenureChange = { emiTenure = it },
            onRateChange = { emiInterestRate = it },
            onNoCostChange = { isNoCostEmi = it; if (it) emiInterestRate = "0" },
            onFeeChange = { emiProcessingFee = it },
            onDismiss = { showEmiDialog = false }
        )
    }

    if (showGoalDialog) {
        SecondaryGoalSheet(
            goals = goals,
            selectedGoalId = selectedGoalId,
            currency = currency,
            accentColor = accentColor,
            accentContainer = accentContainer,
            onGoalSelected = { selectedGoalId = it; showGoalDialog = false },
            onDismiss = { showGoalDialog = false }
        )
    }

    if (showPlatformDialog) {
        SecondaryPlatformSheet(
            platform = selectedPlatform ?: "",
            accentColor = accentColor,
            onPlatformChange = { selectedPlatform = it.ifEmpty { null } },
            onDismiss = { showPlatformDialog = false }
        )
    }

    if (showPeerDialog) {
        SecondaryPeerSheet(
            peers = peers,
            selectedPeerId = selectedPeerId,
            accentColor = accentColor,
            accentContainer = accentContainer,
            onPeerSelected = { selectedPeerId = it; showPeerDialog = false },
            onDismiss = { showPeerDialog = false }
        )
    }

    if (showCategorySearch && TransactionFeature.CATEGORY in features) {
        FormCategorySearchSheet(
            query = categorySearchQuery,
            onQueryChange = { categorySearchQuery = it },
            categories = categories,
            categoryFilter = categoryFilter,
            categoryUsageCounts = categoryUsageCounts,
            accentColor = accentColor,
            accentContainer = accentContainer,
            onCategorySelected = { cat ->
                aiSuggestedFields -= "category"
                selectedCategoryId = cat.id
                expandedCategoryId = cat.parentId ?: cat.id
                showCategorySearch = false
            },
            onDismiss = { showCategorySearch = false }
        )
    }

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
            color = MaterialTheme.colorScheme.onSurface
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
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
                modifier = Modifier.weight(1.3f),
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        CurrencyUtils.getCurrencySymbol(currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else accentColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = amount.ifEmpty { "0" },
                        fontWeight = FontWeight.ExtraBold,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 32.sp,
                        lineHeight = 36.sp
                    )
                    Text(
                        "|",
                        color = accentColor,
                        fontWeight = FontWeight.Light,
                        fontSize = 30.sp
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
                    label = "DATE",
                    value = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(selectedDate)),
                    accentColor = accentColor,
                    onClick = onDateClick
                )
                CompactInfoTile(
                    icon = Icons.Default.AccountBalance,
                    label = if (type == "transfer" || type == "savings") "FROM ACCOUNT" else "ACCOUNT",
                    value = "${accounts.find { it.id == selectedAccountId }?.name ?: "Select Account"} v",
                    accentColor = accentColor,
                    onClick = onAccountClick
                )
                if (type == "transfer" || type == "savings") {
                    CompactInfoTile(
                        icon = Icons.Default.SwapHoriz,
                        label = if (type == "savings") "INVESTMENT ACCOUNT" else "TO ACCOUNT",
                        value = "${accounts.find { it.id == selectedToAccountId }?.name ?: "Select Account"} v",
                        accentColor = accentColor,
                        onClick = onToAccountClick
                    )
                }
                if (type == "lend" || type == "borrow") {
                    CompactInfoTile(
                        icon = Icons.Default.Person,
                        label = "PERSON",
                        value = peers.find { it.id == selectedPeerId }?.effectiveDisplayName ?: "Select Person",
                        accentColor = accentColor,
                        onClick = onPeerClick
                    )
                }
                if (showExpectedReturnDate && (type == "lend" || type == "borrow")) {
                    CompactInfoTile(
                        icon = Icons.AutoMirrored.Filled.EventNote,
                        label = "RETURN DATE",
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
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
    hasActiveSpecialFeatures: Boolean,
    accentColor: Color,
    accentContainer: Color,
    onCategoryClick: (CategoryEntity) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onOpenSpecialFeatures: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Category",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
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

                Surface(
                    onClick = onOpenSpecialFeatures,
                    shape = RoundedCornerShape(12.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasActiveSpecialFeatures) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(accentColor, CircleShape)
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Special Features >",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            fontSize = 11.sp
                        )
                    }
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
            val top4 = sortedParents.take(4).toMutableList()
            if (selectedCategoryId != null && top4.none { it.id == selectedCategoryId }) {
                val selectedCat = filtered.firstOrNull { it.id == selectedCategoryId }
                if (selectedCat != null) {
                    val catToAdd = if (selectedCat.parentId != null) filtered.firstOrNull { it.id == selectedCat.parentId } ?: selectedCat else selectedCat
                    if (top4.size == 4) top4[3] = catToAdd
                    else top4.add(catToAdd)
                }
            }
            top4
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.SpaceBetween,
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
                    .width(64.dp)
                    .clickable { onCategoryClick(cat) }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = itemBg,
                    modifier = Modifier.size(52.dp),
                    border = if (isSelected) BorderStroke(1.5.dp, accentColor) else null,
                    shadowElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CategoryIcon(
                            emoji = cat.emoji,
                            iconType = cat.iconType,
                            colorIndex = cat.colorIndex,
                            fontSize = 22.sp,
                            tint = if (isSelected) accentColor else Color.Unspecified
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    cat.name,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        if (expandedCategoryId == null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(64.dp)
                    .clickable { onMoreClick() }
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "More",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "More",
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
//  Unified Special Features Bottom Sheet
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpecialFeaturesBottomSheetContent(
    type: String,
    features: Set<TransactionFeature>,
    isEmiEnabled: Boolean,
    emiTenure: String,
    note: String,
    selectedTagIds: Set<Long>,
    tags: List<TagEntity>,
    receiptData: String?,
    splitEnabled: Boolean,
    splitRows: List<SplitRowData>,
    selectedGoalId: Long?,
    goals: List<GoalEntity>,
    selectedPeerId: Long?,
    peers: List<PeerContact>,
    expectedReturnDate: Long?,
    accentColor: Color,
    imageAttachmentsEnabled: Boolean,
    onEmiToggle: (Boolean) -> Unit,
    onNoteChange: (String) -> Unit,
    onToggleTag: (Long) -> Unit,
    onAddReceipt: () -> Unit,
    onRemoveReceipt: () -> Unit,
    onPreviewReceipt: () -> Unit,
    onOpenSplit: () -> Unit,
    onOpenGoal: () -> Unit,
    onOpenPeer: () -> Unit,
    onOpenReturnDate: () -> Unit,
    onOpenTags: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Sheet Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accentColor, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Special Features",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Smart transaction enhancements",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Pay via EMI Card (Expense only)
        if (type == "expense") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Pay via EMI",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (isEmiEnabled) "Split into easy monthly installments ($emiTenure mos)" else "Split into easy monthly installments",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isEmiEnabled,
                        onCheckedChange = { onEmiToggle(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentColor
                        )
                    )
                }
            }
        }

        // 2. Note & Description Card
        if (TransactionFeature.NOTE in features) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = accentColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Notes,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Note & Description",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = onNoteChange,
                        placeholder = {
                            Text(
                                "Add receipt note or comment...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        maxLines = 3
                    )
                }
            }
        }

        // 3. Tags & Labels Card (Master data tags only)
        if (TransactionFeature.TAGS in features) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = accentColor.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.LocalOffer,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Tags & Labels",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            "Quick Select",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                val isSelected = tag.id in selectedTagIds
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onToggleTag(tag.id) },
                                    label = {
                                        Text(
                                            "#${tag.name}",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                        selectedLabelColor = accentColor,
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                        selectedBorderColor = accentColor
                                    )
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "No tags created yet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = onOpenTags) {
                                Text("Manage Tags >", style = MaterialTheme.typography.labelSmall, color = accentColor)
                            }
                        }
                    }
                }
            }
        }

        // 4. Receipt & Split Bill Side-by-Side Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (imageAttachmentsEnabled && TransactionFeature.RECEIPT in features) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Receipt",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (receiptData == null) {
                            TextButton(
                                onClick = onAddReceipt,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text(
                                    "Add +",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = accentColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    onClick = onPreviewReceipt,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text("View", style = MaterialTheme.typography.labelSmall, color = accentColor)
                                }
                                IconButton(
                                    onClick = onRemoveReceipt,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (TransactionFeature.SPLIT in features) {
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
                    onClick = onOpenSplit
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallSplit,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Split Bill",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (!splitEnabled) "Off" else "${splitRows.size} splits",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (splitEnabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (splitEnabled) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // 5. Transaction Extras (Goal, Person, Return Date)
        if (TransactionFeature.GOAL in features) {
            val goalName = goals.find { it.id == selectedGoalId }?.name
            ExtraOptionTile(
                icon = Icons.Default.Flag,
                title = "Goal",
                subtitle = goalName ?: "Select goal >",
                accentColor = accentColor,
                onClick = onOpenGoal
            )
        }

        if (TransactionFeature.PEER in features) {
            val peerName = peers.find { it.id == selectedPeerId }?.effectiveDisplayName
            ExtraOptionTile(
                icon = Icons.Default.Person,
                title = "Person",
                subtitle = peerName ?: "Select person >",
                accentColor = accentColor,
                onClick = onOpenPeer
            )
        }

        if (TransactionFeature.RETURN_DATE in features) {
            val returnDateStr = expectedReturnDate?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) }
            ExtraOptionTile(
                icon = Icons.Default.CalendarToday,
                title = "Expected Return Date",
                subtitle = returnDateStr ?: "Not set >",
                accentColor = accentColor,
                onClick = onOpenReturnDate
            )
        }

        Spacer(Modifier.height(8.dp))

        // Save Details Button
        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            )
        ) {
            Text(
                "Save Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ExtraOptionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Secondary Option Sheets & Dialogs ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountPickerSheet(
    title: String,
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    currency: String,
    accentColor: Color,
    accentContainer: Color,
    onAccountSelected: (AccountEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filtered = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts
        else accounts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AccountBalance, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${accounts.size} available accounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (accounts.size > 5) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search account...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { acc ->
                    val isSelected = acc.id == selectedAccountId
                    Surface(
                        onClick = { onAccountSelected(acc) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) accentContainer.copy(alpha = 0.25f)
                               else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 0.5.dp,
                            color = if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            accountTypeIcon(acc.type),
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        acc.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        acc.type.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "${CurrencyUtils.getCurrencySymbol(currency)} ${"%.2f".format(Locale.US, acc.balance)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (acc.balance >= 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                                )
                                if (isSelected) {
                                    Surface(shape = CircleShape, color = accentColor, modifier = Modifier.size(20.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SecondaryNoteSheet(
    note: String,
    accentColor: Color,
    onNoteChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempNote by rememberSaveable { mutableStateOf(note) }
    val presets = remember { listOf("Dinner", "Groceries", "Uber / Cab", "Rent", "Utility Bill", "Shopping", "Salary", "Snacks", "Medicine", "Fuel") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.Notes, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Note & Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = tempNote,
                onValueChange = { tempNote = it },
                placeholder = { Text("Add transaction note or remarks...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                maxLines = 4
            )

            Text("Quick Presets", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    AssistChip(
                        onClick = {
                            tempNote = if (tempNote.isBlank()) preset else "$tempNote - $preset"
                        },
                        label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                        shape = RoundedCornerShape(16.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Button(
                onClick = {
                    onNoteChange(tempNote)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("Save Note", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SecondaryTagsSheet(
    tags: List<TagEntity>,
    selectedTagIds: Set<Long>,
    tagQuery: String,
    accentColor: Color,
    onQueryChange: (String) -> Unit,
    onToggleTag: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val filteredTags = remember(tags, tagQuery) {
        if (tagQuery.isBlank()) tags
        else tags.filter { it.name.contains(tagQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalOffer, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Tags & Labels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${selectedTagIds.size} selected", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = tagQuery,
                onValueChange = onQueryChange,
                placeholder = { Text("Search tags...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            )

            if (filteredTags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    filteredTags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleTag(tag.id) },
                            label = { Text("#${tag.name}", style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                selectedBorderColor = accentColor
                            )
                        )
                    }
                }
            } else {
                Text("No tags found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondaryAttachSheet(
    receiptData: String?,
    accentColor: Color,
    onPickFile: () -> Unit,
    onRemoveFile: () -> Unit,
    onPreviewFile: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AttachFile, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Attach Receipt / Image", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (receiptData == null) {
                Surface(
                    onClick = {
                        onPickFile()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, null, tint = accentColor, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Choose Receipt Image from Device", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = accentColor)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = accentColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Receipt Attached Successfully", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onPreviewFile,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Visibility, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Preview Image")
                            }
                            Button(
                                onClick = {
                                    onRemoveFile()
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Remove")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondarySplitSheet(
    splitEnabled: Boolean,
    splitRows: List<SplitRowData>,
    splitRemaining: Double,
    splitTotal: Double,
    categories: List<CategoryEntity>,
    type: String,
    currency: String,
    accentColor: Color,
    accentContainer: Color,
    onToggleSplitEnabled: (Boolean) -> Unit,
    onUpdateRow: (Int, SplitRowData) -> Unit,
    onRemoveRow: (Int) -> Unit,
    onAddRow: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.CallSplit, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Split Bill Allocation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Enable Split Transaction", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Divide into multiple categories & amounts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Switch(checked = splitEnabled, onCheckedChange = onToggleSplitEnabled, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor))
                }
            }

            if (splitEnabled) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (abs(splitRemaining) < 0.01) accentContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = BorderStroke(0.5.dp, if (abs(splitRemaining) < 0.01) accentColor else MaterialTheme.colorScheme.error)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Allocated: ${CurrencyUtils.getCurrencySymbol(currency)} ${"%.2f".format(Locale.US, splitTotal)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (abs(splitRemaining) < 0.01) "Fully Balanced ✓"
                            else "Unallocated: ${CurrencyUtils.getCurrencySymbol(currency)} ${"%.2f".format(Locale.US, splitRemaining)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(splitRemaining) < 0.01) accentColor else MaterialTheme.colorScheme.error
                        )
                    }
                }

                var openDropdownIndex by remember { mutableStateOf<Int?>(null) }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    splitRows.forEachIndexed { index, row ->
                        SplitRowCard(
                            row = row,
                            allCategories = categories,
                            type = type,
                            onUpdate = { updated -> onUpdateRow(index, updated) },
                            onRemove = { onRemoveRow(index) },
                            showDropdown = openDropdownIndex == index,
                            onToggleDropdown = {
                                openDropdownIndex = if (openDropdownIndex == index) null else index
                            },
                            remainingAmount = splitRemaining,
                            currencySymbol = CurrencyUtils.getCurrencySymbol(currency)
                        )
                    }
                }

                OutlinedButton(
                    onClick = onAddRow,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, accentColor)
                ) {
                    Icon(Icons.Default.Add, null, tint = accentColor)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Split Category", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SecondaryEmiSheet(
    isEmiEnabled: Boolean,
    emiTenure: String,
    emiInterestRate: String,
    isNoCostEmi: Boolean,
    emiProcessingFee: String,
    currency: String,
    mainAmount: Double,
    accentColor: Color,
    onEmiEnabledChange: (Boolean) -> Unit,
    onTenureChange: (String) -> Unit,
    onRateChange: (String) -> Unit,
    onNoCostChange: (Boolean) -> Unit,
    onFeeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val quickTenures = remember { listOf("3", "6", "9", "12", "18", "24") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CreditCard, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Pay via EMI Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Pay via EMI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text("Convert transaction into monthly installments", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                    Switch(checked = isEmiEnabled, onCheckedChange = onEmiEnabledChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor))
                }
            }

            if (isEmiEnabled) {
                Text("Select Tenure (Months)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickTenures.forEach { t ->
                        val isSelected = emiTenure == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { onTenureChange(t) },
                            label = { Text("$t Mos", style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                selectedLabelColor = accentColor,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                labelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                selectedBorderColor = accentColor
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("No Cost EMI", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Checkbox(checked = isNoCostEmi, onCheckedChange = onNoCostChange)
                    }
                }

                if (!isNoCostEmi) {
                    OutlinedTextField(
                        value = emiInterestRate,
                        onValueChange = onRateChange,
                        label = { Text("Interest Rate (% p.a.)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                OutlinedTextField(
                    value = emiProcessingFee,
                    onValueChange = onFeeChange,
                    label = { Text("Processing Fee (${CurrencyUtils.getCurrencySymbol(currency)})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                val tenureInt = emiTenure.toIntOrNull() ?: 6
                val feeDouble = emiProcessingFee.toDoubleOrNull() ?: 0.0
                val estEmi = if (tenureInt > 0 && mainAmount > 0) (mainAmount + feeDouble) / tenureInt else 0.0

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = accentColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.5.dp, accentColor.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Est. Monthly EMI", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${CurrencyUtils.getCurrencySymbol(currency)} ${"%.2f".format(Locale.US, estEmi)} / mo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondaryGoalSheet(
    goals: List<GoalEntity>,
    selectedGoalId: Long?,
    currency: String,
    accentColor: Color,
    accentContainer: Color,
    onGoalSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Flag, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Link Financial Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            val isNoneSelected = selectedGoalId == null
            Surface(
                onClick = { onGoalSelected(null) },
                shape = RoundedCornerShape(16.dp),
                color = if (isNoneSelected) accentContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(if (isNoneSelected) 1.5.dp else 0.5.dp, if (isNoneSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isNoneSelected, onClick = { onGoalSelected(null) }, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                    Spacer(Modifier.width(10.dp))
                    Text("None (Do not link goal)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            goals.forEach { goal ->
                val isSelected = goal.id == selectedGoalId
                Surface(
                    onClick = { onGoalSelected(goal.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) accentContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            RadioButton(selected = isSelected, onClick = { onGoalSelected(goal.id) }, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(goal.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    "Target: ${CurrencyUtils.getCurrencySymbol(currency)} ${"%.2f".format(Locale.US, goal.targetAmount)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SecondaryPlatformSheet(
    platform: String,
    accentColor: Color,
    onPlatformChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var tempPlatform by rememberSaveable { mutableStateOf(platform) }
    val presets = remember { listOf("Zerodha", "Groww", "Upstox", "Angel One", "Kuvera", "Paytm Money", "INDmoney", "Binance", "Coinbase", "WazirX", "Other") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.ShowChart, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Investment Platform", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = tempPlatform,
                onValueChange = { tempPlatform = it },
                placeholder = { Text("e.g. Zerodha, Groww, Upstox...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            )

            Text("Popular Platforms", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isSelected = tempPlatform.equals(preset, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { tempPlatform = preset },
                        label = { Text(preset, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(16.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accentColor.copy(alpha = 0.2f),
                            selectedLabelColor = accentColor,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            selectedBorderColor = accentColor
                        )
                    )
                }
            }

            Button(
                onClick = {
                    onPlatformChange(tempPlatform)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
            ) {
                Text("Done", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SecondaryPeerSheet(
    peers: List<PeerContact>,
    selectedPeerId: Long?,
    accentColor: Color,
    accentContainer: Color,
    onPeerSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filtered = remember(peers, searchQuery) {
        if (searchQuery.isBlank()) peers
        else peers.filter { it.effectiveDisplayName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Select Person", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (peers.size > 4) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search person...") },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )
            }

            val isNoneSelected = selectedPeerId == null
            Surface(
                onClick = { onPeerSelected(null) },
                shape = RoundedCornerShape(16.dp),
                color = if (isNoneSelected) accentContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(if (isNoneSelected) 1.5.dp else 0.5.dp, if (isNoneSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = isNoneSelected, onClick = { onPeerSelected(null) }, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                    Spacer(Modifier.width(10.dp))
                    Text("None (No person associated)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }

            filtered.forEach { peer ->
                val isSelected = peer.id == selectedPeerId
                Surface(
                    onClick = { onPeerSelected(peer.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) accentContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(if (isSelected) 1.5.dp else 0.5.dp, if (isSelected) accentColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = { onPeerSelected(peer.id) }, colors = RadioButtonDefaults.colors(selectedColor = accentColor))
                        Spacer(Modifier.width(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) accentColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    peer.effectiveDisplayName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(peer.effectiveDisplayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FormCategorySearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    categories: List<CategoryEntity>,
    categoryFilter: String,
    categoryUsageCounts: Map<Long, Int> = emptyMap(),
    accentColor: Color,
    accentContainer: Color,
    onCategorySelected: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val filtered = remember(categories, categoryFilter, query) {
        val byType = categories.filter { it.type == categoryFilter }
        if (query.isBlank()) byType.sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
        else byType.filter { it.name.contains(query, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Category, null, tint = accentColor, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Select Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search categories...") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                filtered.forEach { cat ->
                    val parentCat = if (cat.parentId != null) categories.firstOrNull { it.id == cat.parentId } else null
                    Surface(
                        onClick = { onCategorySelected(cat) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, colorIndex = cat.colorIndex, fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                if (parentCat != null) {
                                    Text("In ${parentCat.name}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun FormReceiptPreviewDialog(
    receiptData: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt Preview") },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = remember(receiptData) {
                    try {
                        val file = File(receiptData)
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)
                        else {
                            val bytes = Base64.decode(receiptData, Base64.DEFAULT)
                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                    } catch (_: Exception) { null }
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Receipt",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text("Unable to load receipt preview.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
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
                "expense"  -> Color(0xFFE53935)
                "income"   -> colorScheme.primary
                "savings"  -> colorScheme.tertiary
                "transfer" -> colorScheme.secondary
                "lend", "borrow" -> categoryColors.lending
                else       -> colorScheme.primary
            }

            val bgColor by animateColorAsState(
                if (isSelected) typeAccent else Color.Transparent,
                animationSpec = tween(220),
                label = "tabBg"
            )
            val contentColor by animateColorAsState(
                if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
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
                    .padding(vertical = 8.dp, horizontal = 2.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
