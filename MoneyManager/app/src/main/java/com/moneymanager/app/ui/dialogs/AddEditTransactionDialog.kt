package com.moneymanager.app.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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

/**
 * Canonical keys for the confidence map on [TransactionDraft].
 * Use these constants in both the AI pipeline (when populating the map) and
 * in the UI (when reading it) to prevent silent key-mismatch bugs like CR-01.
 */
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

/**
 * Returns true when the draft signals that at least one field had low confidence
 * and should be explicitly reviewed by the user.
 */
internal fun showReviewBanner(draft: TransactionDraft?): Boolean = draft?.needsReview == true

/**
 * Returns true when the AI confidence for [fieldName] in [draft] is "low".
 * Any other value ("high", "medium", unknown, or null draft) returns false,
 * keeping the safe default behaviour (trust T-41-01).
 */
internal fun fieldIsLowConfidence(fieldName: String, draft: TransactionDraft?): Boolean =
    draft?.confidence?.get(fieldName) == "low"

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
    initialDraft: TransactionDraft? = null,
    onDraftDismiss: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
) {
    val isEdit = transaction != null
    var aiSuggestedFields by remember { mutableStateOf(emptySet<String>()) }

    // ── Core State ──
    var type by rememberSaveable { mutableStateOf(transaction?.type ?: initialType?.takeIf { it.isNotBlank() } ?: "expense") }

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
    // BUG-02 fix: normalise transfer direction so the form always shows source→destination.
    val isIncomingTransferLeg = transaction?.type == "transfer" &&
        transaction.note.contains("transfer from", ignoreCase = true)
    var selectedAccountId by rememberSaveable {
        mutableStateOf(
            if (isIncomingTransferLeg) transaction?.toAccountId ?: filteredAccounts.firstOrNull()?.id
            else transaction?.accountId ?: filteredAccounts.firstOrNull()?.id
        )
    }
    var selectedToAccountId by rememberSaveable {
        mutableStateOf(
            if (isIncomingTransferLeg) transaction?.accountId
            else transaction?.toAccountId
        )
    }
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
    var expandedCategoryId by rememberSaveable {
        mutableStateOf<Long?>(
            transaction?.categoryId?.let { catId ->
                categories.firstOrNull { it.id == catId }?.parentId
            }
        )
    }
    var isCalculatorVisible by rememberSaveable { mutableStateOf(transaction?.amount == null || transaction.amount == 0.0) }
    var categorySearchQuery by rememberSaveable { mutableStateOf("") }
    var showCategorySearch by rememberSaveable { mutableStateOf(false) }
    // Note is hidden by default; shown if editing an existing note
    var showNoteInput by rememberSaveable { mutableStateOf(transaction?.note?.isNotEmpty() == true) }
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
        aiSuggestedFields = aiSuggestedFields - "type"
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

    // ── Transaction Builder ──
    fun buildTransaction(): TransactionEntity? {
        val amt = amount.toDoubleOrNull() ?: return null
        if (amt <= 0 || selectedAccountId == null) return null
        val effectiveCategoryId =
            if (TransactionFeature.CATEGORY in features) (selectedSubCategory?.id ?: selectedParentCategory?.id)
            else null
        val effectiveSubCategoryId =
            if (TransactionFeature.CATEGORY in features) selectedSubCategory?.id else null
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

    // ── Draft Population ──
    // WR-04: guard against re-firing when the parent recomposes with a new (but equivalent)
    // TransactionDraft instance, which would silently reset fields the user has already edited.
    // LaunchedEffect uses === (referential equality) for its key comparison, so even a
    // structurally identical draft allocated on each recomposition would trigger a reset.
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
                showTagInput = true
            }
            if (initialDraft.description != null) {
                description = initialDraft.description
                showNoteInput = true
            } else if (initialDraft.note != null) {
                description = initialDraft.note
                showNoteInput = true
            }
            initialDraft.date?.let { selectedDate = it }
            initialDraft.receiptPath?.let {
                receiptData = it
                showReceiptInput = true
            }

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
                        aiSuggestedFields = aiSuggestedFields - "date"
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
                    aiSuggestedFields = aiSuggestedFields - "date"
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
    Dialog(onDismissRequest = { aiSuggestedFields = emptySet(); onDraftDismiss?.invoke(); onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize()) {
                DialogTopBar(
                    title = "${if (isEdit) "Edit" else "Add"} ${config.displayName}",
                    accentColor = accentColor,
                    onDismiss = { aiSuggestedFields = emptySet(); onDraftDismiss?.invoke(); onDismiss() }
                )

                val isTypeAiField = "type" in aiSuggestedFields
                BadgedBox(
                    badge = {
                        if (isTypeAiField) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Icon(
                                    Icons.Default.AutoAwesome, contentDescription = "AI suggested",
                                    modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                ) {
                    val typeFieldBg = when {
                        fieldIsLowConfidence(ConfidenceKey.TYPE, initialDraft) && isTypeAiField ->
                            MaterialTheme.colorScheme.errorContainer
                        isTypeAiField ->
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                        else -> null
                    }
                    Box(
                        modifier = if (typeFieldBg != null) Modifier.background(
                            typeFieldBg,
                            RoundedCornerShape(4.dp)
                        ).padding(horizontal = 2.dp) else Modifier
                    ) {
                        TransactionTypeHeader(selectedType = type, onTypeSelected = ::onTypeSelected)
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Spacer(Modifier.height(2.dp))

                    // ── Source Banner ──
                    if (initialDraft?.sourceType != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                buildString {
                                    append("Draft from ")
                                    append(initialDraft.sourceType.replaceFirstChar { it.uppercase() })
                                    if (initialDraft.sourceSender != null) {
                                        append(" · ${initialDraft.sourceSender}")
                                    }
                                    if (initialDraft.date != null) {
                                        val elapsedMs = System.currentTimeMillis() - initialDraft.date
                                        if (elapsedMs in 60_000..Long.MAX_VALUE) {
                                            val minutesAgo = elapsedMs / 60_000
                                            append(" · ${minutesAgo} minutes ago")
                                        } else {
                                            append(" · just now")
                                        }
                                    }
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // ── Review Banner (amber warning strip for low-confidence AI drafts) ──
                    if (showReviewBanner(initialDraft)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.tertiaryContainer,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                "Some fields need your review — AI was not confident about highlighted values.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // 1. Amount, Date & Account Card
                    val isAmountDateAccountAiField = "amount" in aiSuggestedFields || "date" in aiSuggestedFields || "account" in aiSuggestedFields
                    val isAmountDateAccountLowConfidence = isAmountDateAccountAiField && (
                        (fieldIsLowConfidence(ConfidenceKey.AMOUNT, initialDraft) && "amount" in aiSuggestedFields) ||
                        (fieldIsLowConfidence(ConfidenceKey.DATE, initialDraft) && "date" in aiSuggestedFields) ||
                        (fieldIsLowConfidence(ConfidenceKey.ACCOUNT_NAME, initialDraft) && "account" in aiSuggestedFields)
                    )
                    BadgedBox(
                        badge = {
                            if (isAmountDateAccountAiField) {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Icon(
                                        Icons.Default.AutoAwesome, contentDescription = "AI suggested",
                                        modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    ) {
                        val amountDateAccountBg = when {
                            isAmountDateAccountLowConfidence ->
                                MaterialTheme.colorScheme.errorContainer
                            isAmountDateAccountAiField ->
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else -> null
                        }
                        Box(
                            modifier = if (amountDateAccountBg != null) Modifier.background(
                                amountDateAccountBg,
                                RoundedCornerShape(4.dp)
                            ).padding(horizontal = 2.dp) else Modifier
                        ) {
                            Box {
                                FormAmountDateAccountCard(
                            amount = amount,
                            currency = currency,
                            selectedDate = selectedDate,
                            selectedAccountId = selectedAccountId,
                            accounts = accounts,
                            expectedReturnDate = expectedReturnDate,
                            showExpectedReturnDate = TransactionFeature.RETURN_DATE in features,
                            accentColor = accentColor,
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
                                    onClick = { aiSuggestedFields = aiSuggestedFields - "account"; selectedAccountId = acc.id; showAccountDropdown = false }
                                )
                            }
                        }
                            }
                        }
                    }

                    // 2. Categories
                    if (TransactionFeature.CATEGORY in features) {
                        val isCategoryAiField = "category" in aiSuggestedFields
                        BadgedBox(
                            badge = {
                                if (isCategoryAiField) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Icon(
                                            Icons.Default.AutoAwesome, contentDescription = "AI suggested",
                                            modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        ) {
                            val categoryFieldBg = when {
                                fieldIsLowConfidence(ConfidenceKey.CATEGORY_NAME, initialDraft) && isCategoryAiField ->
                                    MaterialTheme.colorScheme.errorContainer
                                isCategoryAiField ->
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else -> null
                            }
                            Box(
                                modifier = if (categoryFieldBg != null) Modifier.background(
                                    categoryFieldBg,
                                    RoundedCornerShape(4.dp)
                                ).padding(horizontal = 2.dp) else Modifier
                            ) {
                                FormCategorySection(
                                    categories = categories,
                                    categoryFilter = categoryFilter,
                                    selectedCategoryId = selectedCategoryId,
                                    expandedCategoryId = expandedCategoryId,
                                    categoryUsageCounts = categoryUsageCounts,
                                    accentColor = accentColor,
                                    accentContainer = accentContainer,
                                    onCategoryClick = { cat ->
                                        aiSuggestedFields = aiSuggestedFields - "category"
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
                        }
                    }

                    // 3. Peer Picker
                    if (TransactionFeature.PEER in features) {
                        val isPeerAiField = "peer" in aiSuggestedFields
                        BadgedBox(
                            badge = {
                                if (isPeerAiField) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Icon(
                                            Icons.Default.AutoAwesome, contentDescription = "AI suggested",
                                            modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        ) {
                            val peerFieldBg = when {
                                fieldIsLowConfidence(ConfidenceKey.PEER_NAME, initialDraft) && isPeerAiField ->
                                    MaterialTheme.colorScheme.errorContainer
                                isPeerAiField ->
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else -> null
                            }
                            Box(
                                modifier = if (peerFieldBg != null) Modifier.background(
                                    peerFieldBg,
                                    RoundedCornerShape(4.dp)
                                ).padding(horizontal = 2.dp) else Modifier
                            ) {
                                FormPeerSection(
                                    peers = peers,
                                    selectedPeerId = selectedPeerId,
                                    onPeerSelected = { aiSuggestedFields = aiSuggestedFields - "peer"; selectedPeerId = it }
                                )
                            }
                        }
                    }

                    // 4. Transfer To-Account
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

                    // 5. Goal & Platform
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

                    // 6. Utility Action Chips (Note / Tags / Attach / Split)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            UtilityActionItemData(
                                Icons.AutoMirrored.Filled.Notes, "Note",
                                if (description.isEmpty()) "Add note" else description,
                                showNoteInput
                            ) { showNoteInput = !showNoteInput },
                            UtilityActionItemData(
                                Icons.Default.LocalOffer, "Tags",
                                if (selectedTagIds.isEmpty()) "Add tags" else "${selectedTagIds.size} tags",
                                showTagInput
                            ) { showTagInput = !showTagInput },
                            UtilityActionItemData(
                                Icons.Default.AttachFile, "Attach",
                                if (receiptData == null) "Add" else "1 file",
                                showReceiptInput
                            ) { showReceiptInput = !showReceiptInput },
                            UtilityActionItemData(
                                Icons.AutoMirrored.Filled.CallSplit, "Split",
                                if (!splitEnabled) "Off" else "On",
                                splitEnabled
                            ) { splitEnabled = !splitEnabled },
                        ).filter { a ->
                            when (a.label) {
                                "Tags"  -> TransactionFeature.TAGS in features
                                "Split" -> TransactionFeature.SPLIT in features
                                else    -> true
                            }
                        }.forEach { a ->
                            UtilityActionItem(
                                icon = a.icon,
                                label = a.label,
                                subtext = a.subtext,
                                onClick = a.onClick,
                                isActive = a.active,
                                accentColor = accentColor,
                                accentContainer = accentContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // 7. Split Section
                    AnimatedVisibility(
                        visible = TransactionFeature.SPLIT in features && splitEnabled,
                        enter = expandVertically() + fadeIn(tween(200)),
                        exit = shrinkVertically() + fadeOut(tween(200))
                    ) {
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

                    // 8. Note Input (expandable)
                    AnimatedVisibility(
                        visible = showNoteInput,
                        enter = expandVertically() + fadeIn(tween(200)),
                        exit = shrinkVertically() + fadeOut(tween(200))
                    ) {
                        val isNoteAiField = "note" in aiSuggestedFields
                        BadgedBox(
                            badge = {
                                if (isNoteAiField) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Icon(
                                            Icons.Default.AutoAwesome, contentDescription = "AI suggested",
                                            modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        ) {
                            val noteFieldBg = when {
                                (fieldIsLowConfidence(ConfidenceKey.DESCRIPTION, initialDraft) || fieldIsLowConfidence(ConfidenceKey.NOTE, initialDraft)) && isNoteAiField ->
                                    MaterialTheme.colorScheme.errorContainer
                                isNoteAiField ->
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else -> null
                            }
                            Box(
                                modifier = if (noteFieldBg != null) Modifier.background(
                                    noteFieldBg,
                                    RoundedCornerShape(4.dp)
                                ).padding(horizontal = 2.dp) else Modifier
                            ) {
                                OutlinedTextField(
                                    value = description,
                                    onValueChange = { aiSuggestedFields = aiSuggestedFields - "note"; description = it },
                                    label = { Text("Note") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // 9. Tag Input (expandable)
                    AnimatedVisibility(
                        visible = TransactionFeature.TAGS in features && showTagInput,
                        enter = expandVertically() + fadeIn(tween(200)),
                        exit = shrinkVertically() + fadeOut(tween(200))
                    ) {
                        val isTagsAiField = "tags" in aiSuggestedFields
                        BadgedBox(
                            badge = {
                                if (isTagsAiField) {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Icon(
                                            Icons.Default.AutoAwesome, contentDescription = "AI suggested",
                                            modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        ) {
                            Box(
                                modifier = if (isTagsAiField) Modifier.background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                    RoundedCornerShape(4.dp)
                                ).padding(horizontal = 2.dp) else Modifier
                            ) {
                                // BUG-01 fix: pass filtered tags + callbacks so the dropdown renders.
                                FormTagSection(
                                    selectedTags = selectedTags,
                                    filteredTags = filteredTags.filter { it.id !in selectedTagIds },
                                    tagQuery = tagQuery,
                                    showDropdown = showTagDropdown,
                                    onTagQueryChange = { newQuery ->
                                        tagQuery = newQuery
                                        showTagDropdown = newQuery.isNotEmpty()
                                    },
                                    onRemoveTag = { aiSuggestedFields = aiSuggestedFields - "tags"; selectedTagIds = selectedTagIds - it },
                                    onTagSelected = { tag ->
                                        aiSuggestedFields = aiSuggestedFields - "tags"
                                        selectedTagIds = selectedTagIds + tag.id
                                        tagQuery = ""
                                        showTagDropdown = false
                                    },
                                    onDismissDropdown = { showTagDropdown = false }
                                )
                            }
                        }
                    }

                    // 10. Numeric Keypad (expandable)
                    AnimatedVisibility(
                        visible = isCalculatorVisible,
                        enter = expandVertically() + fadeIn(tween(200)),
                        exit = shrinkVertically() + fadeOut(tween(200))
                    ) {
                        NumericKeypad(
                            accentColor = accentColor,
                            accentContainer = accentContainer,
                            onNumberClick = { num ->
                                aiSuggestedFields = aiSuggestedFields - "amount"
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
                            onDeleteClick = { aiSuggestedFields = aiSuggestedFields - "amount"; if (amount.isNotEmpty()) amount = amount.dropLast(1) },
                            onClearClick = { aiSuggestedFields = aiSuggestedFields - "amount"; amount = "" },
                            onEvaluate = {
                                aiSuggestedFields = aiSuggestedFields - "amount"
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
                        amountValid = amount.isNotEmpty() && amount.toDoubleOrNull() != null,
                        accountValid = selectedAccountId != null,
                        accentColor = accentColor,
                        onCancel = { aiSuggestedFields = emptySet(); onDraftDismiss?.invoke(); onDismiss() },
                        onSave = {
                            val tx = buildTransaction()
                            if (tx != null) {
                                val children = if (splitEnabled && TransactionFeature.SPLIT in features)
                                    buildSplitChildren(tx.id) else null
                                onConfirm(tx, children)
                            }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
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
                aiSuggestedFields = aiSuggestedFields - "category"
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
private fun DialogTopBar(title: String, accentColor: Color, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onDismiss) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onSurface)
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
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
    accentColor: Color,
    onAmountClick: () -> Unit,
    onDateClick: () -> Unit,
    onAccountClick: () -> Unit,
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
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Amount — left, large + prominent
            Column(
                modifier = Modifier.weight(1.4f).clickable { onAmountClick() },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "AMOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontSize = 9.sp
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        CurrencyUtils.getCurrencySymbol(currency),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        else accentColor.copy(alpha = 0.85f),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = if (amount.isEmpty()) "0" else amount,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (amount.isEmpty())
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 40.sp,
                        lineHeight = 44.sp
                    )
                }
            }

            VerticalDivider(
                modifier = Modifier.fillMaxHeight().padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            // Date & Account — right, compact stacked tiles
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    label = "Account",
                    value = accounts.find { it.id == selectedAccountId }?.name ?: "Select Account",
                    accentColor = accentColor,
                    onClick = onAccountClick
                )
                if (showExpectedReturnDate) {
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
            if (expandedCategoryId != null) {
                TextButton(
                    onClick = onBackClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(14.dp), tint = accentColor)
                    Spacer(Modifier.width(2.dp))
                    Text("Back", style = MaterialTheme.typography.labelSmall, color = accentColor)
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
        value = selectedPeer?.effectiveDisplayName ?: "Select person",
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
                text = { Text(peer.effectiveDisplayName) },
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
                            "${acc.type} · ${acc.currency}",
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
                    color = if (abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
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
    filteredTags: List<TagEntity>,
    tagQuery: String,
    showDropdown: Boolean,
    onTagQueryChange: (String) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onTagSelected: (TagEntity) -> Unit,
    onDismissDropdown: () -> Unit,
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
        Box {
            OutlinedTextField(
                value = tagQuery,
                onValueChange = onTagQueryChange,
                label = { Text("Search or add tag") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            DropdownMenu(
                expanded = showDropdown && filteredTags.isNotEmpty(),
                onDismissRequest = onDismissDropdown
            ) {
                filteredTags.forEach { tag ->
                    DropdownMenuItem(
                        text = { Text(tag.name) },
                        onClick = { onTagSelected(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FormActionButtons(
    isEdit: Boolean,
    amountValid: Boolean,
    accountValid: Boolean,
    accentColor: Color,
    onCancel: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f).height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text("Cancel") }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(2f).height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                disabledContainerColor = accentColor.copy(alpha = 0.38f)
            ),
            enabled = amountValid && accountValid
        ) {
            Text(
                if (isEdit) "Update" else "Save",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
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

// ═══════════════════════════════════════════════════════════════
//  Reusable UI Components (internal)
// ═══════════════════════════════════════════════════════════════

@Composable
internal fun TransactionTypeHeader(selectedType: String, onTypeSelected: (String) -> Unit) {
    val types = TransactionFormConfig.allTypes
    val categoryColors = LocalCategoryColors.current
    val colorScheme = MaterialTheme.colorScheme

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
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
                    .padding(vertical = 7.dp, horizontal = 2.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.height(3.dp))
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

@Composable
internal fun UtilityActionItem(
    icon: ImageVector,
    label: String,
    subtext: String,
    onClick: () -> Unit,
    isActive: Boolean,
    accentColor: Color,
    accentContainer: Color,
    modifier: Modifier = Modifier,
) {
    val bgColor by animateColorAsState(
        if (isActive) accentContainer.copy(alpha = 0.25f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label = "chipBg"
    )
    val contentColor by animateColorAsState(
        if (isActive) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(200),
        label = "chipContent"
    )
    val borderColor by animateColorAsState(
        if (isActive) accentColor.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
        animationSpec = tween(200),
        label = "chipBorder"
    )

    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = contentColor)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                fontSize = 10.sp
            )
            Text(
                subtext,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.65f),
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
    val parents = remember(filtered, categoryUsageCounts) {
        val subsByParent = filtered.filter { it.parentId != null }.groupBy { it.parentId }
        filtered.filter { it.parentId == null }.sortedByDescending { cat ->
            val direct = categoryUsageCounts[cat.id] ?: 0
            val subCounts = subsByParent[cat.id]?.sumOf { categoryUsageCounts[it.id] ?: 0 } ?: 0
            direct + subCounts
        }
    }
    val displayCats = if (expandedCategoryId != null) {
        val subs = filtered.filter { it.parentId == expandedCategoryId }
            .sortedByDescending { categoryUsageCounts[it.id] ?: 0 }
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
            val itemBg by animateColorAsState(
                if (isSelected) accentContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                animationSpec = tween(200),
                label = "catBg"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(62.dp).clickable { onCategoryClick(cat) }
            ) {
                Box(contentAlignment = Alignment.TopEnd) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = itemBg,
                        modifier = Modifier.size(56.dp),
                        border = if (isSelected) BorderStroke(1.5.dp, accentColor.copy(alpha = 0.7f)) else null,
                        shadowElevation = if (isSelected) 3.dp else 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, fontSize = 24.sp)
                        }
                    }
                    if (isSelected) {
                        // Check indicator: surface ring + accent fill
                        Box(
                            Modifier
                                .size(17.dp)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                                .padding(2.dp)
                                .background(accentColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check, null,
                                modifier = Modifier.size(9.dp),
                                tint = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
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
                modifier = Modifier.width(62.dp).clickable { onMoreClick() }
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.MoreHoriz, null,
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
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
