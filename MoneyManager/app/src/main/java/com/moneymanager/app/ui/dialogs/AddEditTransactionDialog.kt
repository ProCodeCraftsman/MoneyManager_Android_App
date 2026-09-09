package com.moneymanager.app.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.moneymanager.domain.transaction.SplitTransactionFactory
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
) : java.io.Serializable

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
                    val (pId, sId) = if (child.subCategoryId != null) {
                        child.categoryId to child.subCategoryId
                    } else {
                        child.categoryId?.let { cid ->
                            val cat = categories.firstOrNull { it.id == cid }
                            if (cat?.parentId != null) cat.parentId to cid else cid to null
                        } ?: (null to null)
                    }
                    val itemDesc = child.description.ifBlank { child.note }
                    SplitRowData(index, pId, sId, itemDesc, child.amount.toString())
                }
            } else {
                val categoryFilter = TransactionFormConfig.resolveCategoryType(type)
                val selectedCat = categories.firstOrNull { it.id == selectedCategoryId && it.type == categoryFilter }
                val initialParentId = selectedCat?.parentId ?: selectedCat?.id
                val initialSubId = if (selectedCat?.parentId != null) selectedCat.id else null
                listOf(
                    SplitRowData(0, initialParentId, initialSubId, description, amount),
                    SplitRowData(1)
                )
            }
        )
    }
    var splitIdCounter by rememberSaveable {
        mutableIntStateOf(
            if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) splitChildren.size else 2
        )
    }

    fun updateSplitRowsFromSelection() {
        val categoryFilter = TransactionFormConfig.resolveCategoryType(type)
        if (splitRows.all { it.categoryId == null }) {
            val selectedCat = categories.firstOrNull { it.id == selectedCategoryId && it.type == categoryFilter }
            val pId = selectedCat?.parentId ?: selectedCat?.id
            val sId = if (selectedCat?.parentId != null) selectedCat.id else null
            val desc0 = splitRows.firstOrNull()?.description?.ifBlank { description } ?: description
            val amt0 = splitRows.firstOrNull()?.amount?.ifBlank { amount } ?: amount
            splitRows = listOf(
                SplitRowData(0, pId, sId, desc0, amt0),
                SplitRowData(1, description = splitRows.getOrNull(1)?.description ?: "", amount = splitRows.getOrNull(1)?.amount ?: "")
            )
        }
    }

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
    var activeFeatureTab by rememberSaveable { mutableStateOf("category") }

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
        isEmiEnabled = false
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
        val isSplit = splitEnabled && TransactionFeature.SPLIT in features
        val effectiveCategoryId =
            if (TransactionFeature.CATEGORY in features && !isSplit) (selectedCat?.parentId ?: selectedCat?.id)
            else null
        val effectiveSubCategoryId =
            if (TransactionFeature.CATEGORY in features && !isSplit) (if (selectedCat?.parentId != null) selectedCat.id else null)
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
            isSplitParent = isSplit,
            isTransfer = type == "transfer" || type == "savings",
            toAccountId = if (type == "transfer" || type == "savings") selectedToAccountId else null,
            createdAt = transaction?.createdAt ?: System.currentTimeMillis()
        )
    }

    fun buildSplitChildren(parentId: Long): List<TransactionEntity> {
        return SplitTransactionFactory.buildSplitChildren(
            parentId = parentId,
            accountId = selectedAccountId!!,
            type = type,
            date = selectedDate,
            rows = splitRows,
        )
    }

    fun handleSave() {
        try {
            if (amount.contains("+") || amount.contains("-") || amount.contains("*") || amount.contains("/")) {
                val res = evaluateExpression(amount)
                amount = if (res % 1.0 == 0.0) "%.0f".format(Locale.US, res) else "%.2f".format(Locale.US, res)
            }
        } catch (_: Exception) {}

        if (splitEnabled && TransactionFeature.SPLIT in features) {
            val activeRows = splitRows.filter { (it.amount.toDoubleOrNull() ?: 0.0) > 0 }
            if (activeRows.size < 2) {
                Toast.makeText(context, "Split transaction requires at least 2 split items", Toast.LENGTH_SHORT).show()
                return
            }
            if (activeRows.any { it.categoryId == null }) {
                Toast.makeText(context, "Please select a category for each split item", Toast.LENGTH_SHORT).show()
                return
            }
            if (abs(splitRemaining) >= 0.01) {
                Toast.makeText(context, "Split total must match main amount (Unallocated: %.2f)".format(Locale.US, splitRemaining), Toast.LENGTH_SHORT).show()
                return
            }
        }

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
                val (pId, sId) = if (child.subCategoryId != null) {
                    child.categoryId to child.subCategoryId
                } else {
                    child.categoryId?.let { cid ->
                        val cat = categories.firstOrNull { it.id == cid }
                        if (cat?.parentId != null) cat.parentId to cid else cid to null
                    } ?: (null to null)
                }
                val itemDesc = child.description.ifBlank { child.note }
                SplitRowData(index, pId, sId, itemDesc, child.amount.toString())
            }
            splitIdCounter = splitChildren.size
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
            initialDraft.goalId?.let { selectedGoalId = it }
            initialDraft.investmentPlatform?.let { selectedPlatform = it }
            initialDraft.toAccountId?.let { selectedToAccountId = it }
            initialDraft.expectedReturnDate?.let { expectedReturnDate = it }

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
                if (initialDraft.goalId != null) add("goal")
                if (initialDraft.investmentPlatform != null) add("platform")
                if (initialDraft.toAccountId != null) add("toAccount")
                if (initialDraft.expectedReturnDate != null) add("returnDate")
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
                    modifier = Modifier.circularTypeSwipeable(type, ::onTypeSelected),
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
                        features = features,
                        transactionType = type,
                        note = description,
                        onNoteChange = { description = it },
                        selectedTagIds = selectedTagIds,
                        tags = tags,
                        onToggleTag = { tagId ->
                            if (tagId in selectedTagIds) selectedTagIds -= tagId
                            else selectedTagIds += tagId
                        },
                        isEmiEnabled = isEmiEnabled,
                        onEmiToggle = { enabled ->
                            isEmiEnabled = enabled
                            if (enabled) showEmiDialog = true
                        },
                        emiTenure = emiTenure,
                        onTenureChange = { emiTenure = it },
                        receiptData = receiptData,
                        imageAttachmentsEnabled = imageAttachmentsEnabled,
                        onAddReceipt = {
                            if (imageAttachmentsEnabled) {
                                filePicker.launch("image/*")
                            }
                        },
                        onPreviewReceipt = { showReceiptPreview = true },
                        splitEnabled = splitEnabled,
                        onOpenSplit = {
                            splitEnabled = true
                            updateSplitRowsFromSelection()
                            showSplitDialog = true
                        },
                        selectedGoalId = selectedGoalId,
                        goals = goals,
                        onOpenGoal = { showGoalDialog = true },
                        selectedPeerId = selectedPeerId,
                        peers = peers,
                        onOpenPeer = { showPeerDialog = true },
                        expectedReturnDate = expectedReturnDate,
                        onOpenReturnDate = { showExpectedReturnDatePicker = true },
                        activeFeatureTab = activeFeatureTab,
                        onTabSelected = { activeFeatureTab = it },
                        onOpenTagsManager = { showTagsDialog = true },
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
                    showSpecialFeaturesSheet = false
                    if (enabled) showEmiDialog = true
                },
                onNoteChange = { description = it },
                onToggleTag = { tagId ->
                    selectedTagIds = if (tagId in selectedTagIds) selectedTagIds - tagId else selectedTagIds + tagId
                },
                onAddReceipt = { filePicker.launch("image/*") },
                onRemoveReceipt = { receiptData = null },
                onPreviewReceipt = { showReceiptPreview = true },
                onOpenSplit = {
                    splitEnabled = true
                    updateSplitRowsFromSelection()
                    showSpecialFeaturesSheet = false
                    showSplitDialog = true
                },
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
            onToggleSplitEnabled = { enabled ->
                splitEnabled = enabled
                if (enabled) updateSplitRowsFromSelection()
            },
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
