package com.moneymanager.app.ui.dialogs

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.R
import com.moneymanager.app.ui.constants.INVESTMENT_PLATFORMS
import com.moneymanager.app.ui.components.CategoryGrid
import com.moneymanager.app.ui.components.NumericKeypad
import com.moneymanager.app.ui.components.SplitRowCard
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TagEntity
import com.moneymanager.data.entity.TransactionEntity
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
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onSaveAndDuplicate: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onCreateTag: (String) -> Unit,
) {
    val isEdit = transaction != null
    var type by rememberSaveable { mutableStateOf(transaction?.type ?: initialType ?: "expense") }
    var amount by rememberSaveable { mutableStateOf(transaction?.amount?.let { if (it < 0) (-it).toString() else it.toString() } ?: "") }
    var selectedAccountId by rememberSaveable { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id) }
    var selectedToAccountId by rememberSaveable { mutableStateOf(transaction?.toAccountId) }
    var showToAccountDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedCategoryId by rememberSaveable { mutableStateOf<Long?>(transaction?.categoryId) }
    var selectedPeerId by rememberSaveable { mutableStateOf<Long?>(transaction?.peerContactId) }
    var peerName by rememberSaveable { mutableStateOf(peers.find { it.id == transaction?.peerContactId }?.displayName ?: "") }
    var selectedDate by rememberSaveable { mutableStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var description by rememberSaveable { mutableStateOf(transaction?.note ?: "") }
    var selectedGoalId by rememberSaveable { mutableStateOf<Long?>(transaction?.goalId) }
    var selectedPlatform by rememberSaveable { mutableStateOf<String?>(transaction?.investmentPlatform) }
    var splitEnabled by rememberSaveable { mutableStateOf(transaction?.isSplitParent == true) }
    var splitRows by rememberSaveable {
        mutableStateOf(
            if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
                splitChildren.mapIndexed { index, child ->
                    val parentCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                        categories.find { it.id == child.categoryId }?.parentId
                    } else {
                        child.categoryId
                    }
                    val subCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                        child.categoryId
                    } else {
                        null
                    }
                    SplitRowData(index, parentCatId, subCatId, child.note, child.amount.toString())
                }
            } else {
                listOf(SplitRowData(0), SplitRowData(1))
            }
        )
    }
    var tagQuery by rememberSaveable { mutableStateOf("") }
    var showTagDropdown by rememberSaveable { mutableStateOf(false) }
    var selectedTagIds by rememberSaveable {
        mutableStateOf(
            if (transaction?.tagIds?.isNotEmpty() == true)
                transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            else emptySet<Long>()
        )
    }
    var receiptData by rememberSaveable { mutableStateOf<String?>(transaction?.receiptPath) }
    var showReceiptPreview by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showAccountDropdown by rememberSaveable { mutableStateOf(false) }
    var showCategoryDropdown by rememberSaveable { mutableStateOf(false) }
    var showPeerDropdown by rememberSaveable { mutableStateOf(false) }
    var expectedReturnDate by rememberSaveable { mutableStateOf<Long?>(transaction?.expectedReturnDate) }
    var showExpectedReturnDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTypeDropdown by rememberSaveable { mutableStateOf(false) }

    var showNoteInput by rememberSaveable { mutableStateOf(transaction?.note?.isNotEmpty() == true) }
    var showTagInput by rememberSaveable { mutableStateOf(transaction?.tagIds?.isNotEmpty() == true) }
    var showReceiptInput by rememberSaveable { mutableStateOf(transaction?.receiptPath != null) }
    var showSplitInput by rememberSaveable { mutableStateOf(transaction?.isSplitParent == true) }

    var showSubCategoryDropdown by rememberSaveable { mutableStateOf(false) }
    var showGoalDropdown by rememberSaveable { mutableStateOf(false) }
    var showPlatformDropdown by rememberSaveable { mutableStateOf(false) }
    var splitIdCounter by rememberSaveable { mutableStateOf(2) }
    var showSplitCategoryDropdown by rememberSaveable { mutableStateOf<Int?>(null) }

    val context = LocalContext.current

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

    LaunchedEffect(splitChildren) {
        if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
            splitRows = splitChildren.mapIndexed { index, child ->
                val parentCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                    categories.find { it.id == child.categoryId }?.parentId
                } else {
                    child.categoryId
                }
                val subCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                    child.categoryId
                } else {
                    null
                }
                SplitRowData(index, parentCatId, subCatId, child.note, child.amount.toString())
            }
        }
    }

    var pendingTagName by rememberSaveable { mutableStateOf<String?>(null) }
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

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { receiptData = FileHelper.saveReceipt(context, it) }
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
            peerContactId = if (type == "lend" || type == "receive") selectedPeerId else null,
            expectedReturnDate = if (type == "lend" || type == "receive") expectedReturnDate else null,
            goalId = selectedGoalId,
            tagIds = selectedTagIds.joinToString(","),
            date = selectedDate,
            note = description,
            description = description,
            receiptPath = receiptData,
            isRecurring = transaction?.isRecurring ?: false,
            recurringId = transaction?.recurringId,
            isSplitParent = splitEnabled,
            isTransfer = type == "transfer",
            toAccountId = if (type == "transfer") selectedToAccountId else null,
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

    if (showExpectedReturnDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expectedReturnDate ?: System.currentTimeMillis())
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
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                                Image(it.asImageBitmap(), "Receipt", modifier = Modifier.fillMaxWidth())
                            }
                        } else {
                            Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                            Text("PDF Receipt", modifier = Modifier.align(Alignment.CenterHorizontally))
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
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(
                            onClick = { showTypeDropdown = true },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "New ${type.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            val types = listOf("expense", "income", "savings", "transfer", "lend", "receive")
                            types.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        type = t
                                        selectedCategoryId = null
                                        selectedToAccountId = null
                                        selectedPlatform = null
                                        selectedGoalId = null
                                        selectedPeerId = null
                                        showTypeDropdown = false
                                    },
                                    leadingIcon = {
                                        val icon = when (t) {
                                            "expense" -> Icons.Default.MoneyOff
                                            "income" -> Icons.Default.Money
                                            "savings" -> Icons.AutoMirrored.Filled.TrendingUp
                                            "transfer" -> Icons.Default.SwapHoriz
                                            "lend" -> Icons.Default.Handshake
                                            "receive" -> Icons.AutoMirrored.Filled.CallReceived
                                            else -> Icons.Default.Edit
                                        }
                                        Icon(icon, null)
                                    }
                                )
                            }
                        }
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text(
                                text = CurrencyUtils.getCurrencySymbol(currency),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (amount.isEmpty()) "0.00" else amount,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (amount.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable { }
                            )
                        }

                        NumericKeypad(
                            onNumberClick = { num ->
                                if (num == ".") {
                                    if (amount.isEmpty()) amount = "0."
                                    else if (!amount.contains(".")) amount += "."
                                } else if (num in listOf("+", "-", "*", "/")) {
                                    if (amount.isNotEmpty() && !amount.last().isDigit()) {
                                        amount = amount.dropLast(1) + num
                                    } else if (amount.isNotEmpty()) {
                                        amount += num
                                    }
                                } else {
                                    if (amount == "0") amount = num
                                    else amount += num
                                }
                            },
                            onDeleteClick = {
                                if (amount.isNotEmpty()) amount = amount.dropLast(1)
                            },
                            onClearClick = { amount = "" },
                            onEvaluate = {
                                try {
                                    val result = com.moneymanager.app.ui.utils.evaluateExpression(amount)
                                    amount = if (result % 1.0 == 0.0) result.toInt().toString() else "%.2f".format(Locale.US, result)
                                } catch (e: Exception) { }
                            }
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { showDatePicker = true }
                                )
                                Text(" | ", color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { showTimePicker = true }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VerticalDivider(Modifier.height(16.dp).padding(horizontal = 8.dp))
                                if (type == "transfer") {
                                    Text(
                                        text = accounts.find { it.id == selectedAccountId }?.name ?: "From",
                                        modifier = Modifier.clickable { showAccountDropdown = true },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp).padding(horizontal = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = accounts.find { it.id == selectedToAccountId }?.name ?: "To",
                                        modifier = Modifier.clickable { showToAccountDropdown = true },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    val account = accounts.find { it.id == selectedAccountId }
                                    Text(
                                        text = account?.name ?: "Select Account",
                                        modifier = Modifier.clickable { showAccountDropdown = true },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    Box {
                        DropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.emoji} ${acc.name}") },
                                    onClick = { selectedAccountId = acc.id; showAccountDropdown = false }
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showToAccountDropdown,
                            onDismissRequest = { showToAccountDropdown = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.emoji} ${acc.name}") },
                                    onClick = { selectedToAccountId = acc.id; showToAccountDropdown = false }
                                )
                            }
                        }
                    }

                    if (type != "transfer" && !splitEnabled) {
                        if (type == "lend" || type == "receive") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Lending Partner", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPeerDropdown = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(peerName.ifBlank { "Select Person" })
                                        Icon(Icons.Default.Person, null)
                                    }
                                }
                                Box {
                                    DropdownMenu(expanded = showPeerDropdown, onDismissRequest = { showPeerDropdown = false }) {
                                        if (peers.isEmpty()) {
                                            DropdownMenuItem(text = { Text("No partners found") }, onClick = { showPeerDropdown = false })
                                        }
                                        peers.forEach { peer ->
                                            DropdownMenuItem(
                                                text = { Text(peer.displayName) },
                                                onClick = {
                                                    selectedPeerId = peer.id
                                                    peerName = peer.displayName
                                                    showPeerDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Text("Expected Return Date", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExpectedReturnDatePicker = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(expectedReturnDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "No return date")
                                        Icon(Icons.Default.DateRange, null)
                                    }
                                }
                            }
                        } else if (selectedCategoryId == null || (selectedParentCategory == null && selectedSubCategory == null)) {
                            CategoryGrid(
                                categories = parentCategories,
                                selectedCategoryId = null,
                                onCategoryClick = { cat ->
                                    selectedCategoryId = cat.id
                                },
                                onShowAllClick = { showCategoryDropdown = true }
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val displayCat = selectedParentCategory ?: selectedSubCategory
                                    FilterChip(
                                        selected = true,
                                        onClick = { },
                                        label = { Text(displayCat?.name ?: "") },
                                        leadingIcon = { Text(displayCat?.emoji ?: "") },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(48.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    )
                                    TextButton(onClick = {
                                        selectedCategoryId = null
                                    }) {
                                        Text("Change")
                                    }
                                }

                                if (subCategories.isNotEmpty()) {
                                    Text("Sub-category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(subCategories) { sub ->
                                            FilterChip(
                                                selected = selectedCategoryId == sub.id,
                                                onClick = { selectedCategoryId = sub.id },
                                                label = { Text(sub.name) },
                                                leadingIcon = { Text(sub.emoji) },
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.height(48.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Box {
                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                categories.filter { it.type == type || (type == "savings" && it.type == "expense") }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${if (cat.parentId != null) "  ↳ " else ""}${cat.emoji} ${cat.name}") },
                                        onClick = { selectedCategoryId = cat.id; showCategoryDropdown = false }
                                    )
                                }
                            }
                        }
                    }

                    if (type == "savings") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Savings Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            OutlinedCard(
                                onClick = { showPlatformDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Text(selectedPlatform ?: "Select Platform", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }

                            if (goals.isNotEmpty()) {
                                OutlinedCard(
                                    onClick = { showGoalDropdown = true },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(Modifier.width(12.dp))
                                            Text(goals.find { it.id == selectedGoalId }?.name ?: "Link to Goal", style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }
                        }

                        DropdownMenu(expanded = showPlatformDropdown, onDismissRequest = { showPlatformDropdown = false }) {
                            INVESTMENT_PLATFORMS.forEach { platform ->
                                DropdownMenuItem(text = { Text(platform) }, onClick = { selectedPlatform = platform; showPlatformDropdown = false })
                            }
                        }
                        DropdownMenu(expanded = showGoalDropdown, onDismissRequest = { showGoalDropdown = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedGoalId = null; showGoalDropdown = false })
                            goals.forEach { goal ->
                                DropdownMenuItem(text = { Text(goal.name) }, onClick = { selectedGoalId = goal.id; showGoalDropdown = false })
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showNoteInput = !showNoteInput }) {
                            Icon(
                                Icons.Default.Notes,
                                "Note",
                                tint = if (showNoteInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showTagInput = !showTagInput }) {
                            Icon(
                                Icons.Default.LocalOffer,
                                "Tag",
                                tint = if (showTagInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showReceiptInput = !showReceiptInput }) {
                            Icon(
                                Icons.Default.AttachFile,
                                "Receipt",
                                tint = if (showReceiptInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showSplitInput = !showSplitInput; splitEnabled = !splitEnabled }) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallSplit,
                                "Split",
                                tint = if (showSplitInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (showNoteInput) {
                        OutlinedTextField(
                            value = description, onValueChange = { description = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    if (showTagInput) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedTags.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    selectedTags.forEach { tag ->
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedTagIds = selectedTagIds - tag.id },
                                            label = { Text(tag.name) },
                                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                                        )
                                    }
                                }
                            }
                            Box {
                                OutlinedTextField(
                                    value = tagQuery, onValueChange = { tagQuery = it; showTagDropdown = true },
                                    label = { Text("Search or create tag") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = showTagDropdown && tagQuery.isNotEmpty(),
                                    onDismissRequest = { showTagDropdown = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    if (filteredTags.isEmpty() && tagQuery.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Create tag \"$tagQuery\"") },
                                            onClick = {
                                                onCreateTag(tagQuery)
                                                pendingTagName = tagQuery
                                                showTagDropdown = false
                                            }
                                        )
                                    }
                                    filteredTags.forEach { tag ->
                                        DropdownMenuItem(
                                            text = { Text(tag.name) },
                                            onClick = {
                                                selectedTagIds = selectedTagIds + tag.id
                                                tagQuery = ""
                                                showTagDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showReceiptInput) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (receiptData != null) {
                                Box(
                                    Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { showReceiptPreview = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (receiptData!!.startsWith("data:image")) {
                                        val bitmap = remember(receiptData) {
                                            runCatching {
                                                val b64 = receiptData!!.substringAfter("base64,")
                                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            }.getOrNull()
                                        }
                                        if (bitmap != null) {
                                            Image(bitmap.asImageBitmap(), "Receipt", modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Default.Image, null)
                                        }
                                    } else {
                                        Icon(Icons.Default.PictureAsPdf, null)
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { filePicker.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddAPhoto, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (receiptData == null) "Add Receipt" else "Change")
                            }
                        }
                    }

                    if (showSplitInput) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.primaryContainer
                                else if (splitRemaining < 0) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, mainAmount)}",
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Allocated", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitTotal)}",
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitRemaining)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.primary
                                        else if (splitRemaining < 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            splitRows.forEachIndexed { index, row ->
                                SplitRowCard(
                                    row = row,
                                    allCategories = categories,
                                    type = type,
                                    onUpdate = { updated -> splitRows = splitRows.toMutableList().also { it[index] = updated } },
                                    onRemove = { splitRows = splitRows.toMutableList().also { it.removeAt(index) } },
                                    showDropdown = showSplitCategoryDropdown == row.localId,
                                    onToggleDropdown = { showSplitCategoryDropdown = if (showSplitCategoryDropdown == row.localId) null else row.localId },
                                    remainingAmount = splitRemaining,
                                    currencySymbol = CurrencyUtils.getCurrencySymbol(currency)
                                )
                            }
                            TextButton(onClick = { splitRows = splitRows + SplitRowData(splitIdCounter++) }) {
                                Icon(Icons.Default.Add, null)
                                Text(" Add Split")
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp))
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val tx = buildTransaction() ?: return@Button
                                    if (splitEnabled) {
                                        onConfirm(tx, buildSplitChildren(tx.id))
                                    } else {
                                        onConfirm(tx, null)
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = (amount.toDoubleOrNull() ?: try { com.moneymanager.app.ui.utils.evaluateExpression(amount) } catch(e:Exception) { null }) != null && selectedAccountId != null &&
                                        (!splitEnabled || abs(splitRemaining) < 0.001) &&
                                        (type != "lend" && type != "receive" || selectedPeerId != null)
                            ) {
                                Text(if (isEdit) "UPDATE" else "SAVE", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!isEdit) {
                            Button(
                                onClick = {
                                    val tx = buildTransaction() ?: return@Button
                                    if (splitEnabled) {
                                        onSaveAndDuplicate(tx, buildSplitChildren(tx.id))
                                    } else {
                                        onSaveAndDuplicate(tx, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                enabled = (amount.toDoubleOrNull() ?: try { com.moneymanager.app.ui.utils.evaluateExpression(amount) } catch(e:Exception) { null }) != null && selectedAccountId != null &&
                                        (!splitEnabled || abs(splitRemaining) < 0.001) &&
                                        (type != "lend" && type != "receive" || selectedPeerId != null)
                            ) {
                                Text("SAVE & DUPLICATE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}