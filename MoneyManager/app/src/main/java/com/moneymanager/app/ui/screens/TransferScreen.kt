package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class TransferUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    application: Application,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) : AndroidViewModel(application) {

    val uiState: StateFlow<TransferUiState> = accountRepository.getAllAccounts()
        .map { accounts ->
            TransferUiState(accounts = accounts, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = TransferUiState()
        )

    fun executeTransfer(
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        note: String,
        date: Long = System.currentTimeMillis(),
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Get accounts
                val fromAccount = accountRepository.getAccountById(fromAccountId)
                val toAccount = accountRepository.getAccountById(toAccountId)

                if (fromAccount == null || toAccount == null) {
                    return@launch
                }

                // Validate balance
                if (fromAccount.balance < amount) {
                    return@launch
                }

                val timestamp = date
                val fromNote = if (note.isBlank()) "Transfer to ${toAccount.name}" else "Transfer to ${toAccount.name}: $note"
                val toNote = if (note.isBlank()) "Transfer from ${fromAccount.name}" else "Transfer from ${fromAccount.name}: $note"

                // Create two transactions
                val fromTransaction = TransactionEntity(
                    accountId = fromAccountId,
                    type = "transfer",
                    amount = -amount,
                    note = fromNote,
                    date = timestamp,
                    isTransfer = true
                )
                val toTransaction = TransactionEntity(
                    accountId = toAccountId,
                    type = "transfer",
                    amount = amount,
                    note = toNote,
                    date = timestamp,
                    isTransfer = true
                )

                // Insert transactions
                transactionRepository.insertTransaction(fromTransaction)
                transactionRepository.insertTransaction(toTransaction)

                // Update account balances
                accountRepository.updateAccountBalance(fromAccountId, -amount)
                accountRepository.updateAccountBalance(toAccountId, amount)

                onSuccess()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    viewModel: TransferViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showFromDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val dateDisplayFmt = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val transferAmount = amount.toDoubleOrNull() ?: 0.0
    val fromAccount = uiState.accounts.find { it.id == fromAccountId }
    val canTransfer = fromAccountId != null && toAccountId != null &&
            fromAccountId != toAccountId && transferAmount > 0 &&
            (fromAccount?.balance ?: 0.0) >= transferAmount

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dpState.selectedDateMillis?.let { millis ->
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
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val tpState = rememberTimePickerState(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = tpState) },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    c.set(Calendar.HOUR_OF_DAY, tpState.hour)
                    c.set(Calendar.MINUTE, tpState.minute)
                    selectedDate = c.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // From Account
            Text(
                text = "From Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExposedDropdownMenuBox(
                expanded = showFromDropdown,
                onExpandedChange = { showFromDropdown = it }
            ) {
                OutlinedTextField(
                    value = fromAccount?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFromDropdown) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = showFromDropdown,
                    onDismissRequest = { showFromDropdown = false }
                ) {
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(account.name)
                                    Text(
                                        "Balance: ${CurrencyUtils.getCurrencySymbol(account.currency)}${String.format("%.2f", account.balance)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                fromAccountId = account.id
                                // Reset to account if same
                                if (toAccountId == account.id) {
                                    toAccountId = null
                                }
                                showFromDropdown = false
                            }
                        )
                    }
                }
            }

            // To Account
            Text(
                text = "To Account",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExposedDropdownMenuBox(
                expanded = showToDropdown,
                onExpandedChange = { showToDropdown = it }
            ) {
                OutlinedTextField(
                    value = uiState.accounts.find { it.id == toAccountId }?.name ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showToDropdown) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = showToDropdown,
                    onDismissRequest = { showToDropdown = false }
                ) {
                    uiState.accounts.filter { it.id != fromAccountId }.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name) },
                            onClick = {
                                toAccountId = account.id
                                showToDropdown = false
                            }
                        )
                    }
                }
            }

            // Amount
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Date & Time
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = dateDisplayFmt.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = { Icon(Icons.Default.DateRange, null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showDatePicker = true }
                )
                OutlinedTextField(
                    value = timeFmt.format(Date(selectedDate)),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time") },
                    trailingIcon = { Icon(Icons.Default.Schedule, null) },
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showTimePicker = true }
                )
            }

                // Description (optional)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Description (Optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Show insufficient balance warning
            if (fromAccount != null && transferAmount > fromAccount.balance) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Insufficient balance",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Transfer button
            Button(
                onClick = {
                    viewModel.executeTransfer(
                        fromAccountId = fromAccountId!!,
                        toAccountId = toAccountId!!,
                        amount = transferAmount,
                        note = note,
                        date = selectedDate
                    ) {
                        showSuccessDialog = true
                    }
                },
                enabled = canTransfer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Transfer")
            }

            // Same account warning
            if (fromAccountId != null && toAccountId != null && fromAccountId == toAccountId) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "Cannot transfer to the same account",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Success dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onNavigateBack()
            },
            title = { Text("Transfer Successful") },
            text = { Text("Successfully transferred ${CurrencyUtils.getCurrencySymbol(fromAccount?.currency ?: "INR")}${String.format("%.2f", transferAmount)}") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onNavigateBack()
                }) {
                    Text("OK")
                }
            }
        )
    }
}