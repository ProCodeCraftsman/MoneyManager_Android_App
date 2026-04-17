package com.moneymanager.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.moneymanager.data.entity.AccountEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferDialog(
    accounts: List<AccountEntity>,
    currency: String = "USD",
    onDismiss: () -> Unit,
    onTransfer: (fromAccountId: Long, toAccountId: Long, amount: Double, note: String, date: Long) -> Unit
) {
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expandedFrom by remember { mutableStateOf(false) }
    var expandedTo by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateDisplayFmt = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val fromAccount = accounts.find { it.id == fromAccountId }
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    val isValid = fromAccountId != null &&
        toAccountId != null &&
        fromAccountId != toAccountId &&
        amountValue > 0 &&
        (fromAccount?.balance ?: 0.0) >= amountValue

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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Money", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // From
                ExposedDropdownMenuBox(expandedFrom, { expandedFrom = it }) {
                    OutlinedTextField(
                        value = fromAccount?.name ?: "Select source account",
                        onValueChange = {}, readOnly = true, label = { Text("From Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedFrom) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expandedFrom, { expandedFrom = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Column {
                                    Text(acc.name)
                                    Text("${acc.type} · ${acc.currency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }},
                                onClick = { fromAccountId = acc.id; expandedFrom = false }
                            )
                        }
                    }
                }

                // To
                ExposedDropdownMenuBox(expandedTo, { expandedTo = it }) {
                    OutlinedTextField(
                        value = accounts.find { it.id == toAccountId }?.name ?: "Select destination account",
                        onValueChange = {}, readOnly = true, label = { Text("To Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedTo) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expandedTo, { expandedTo = false }) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Column {
                                    Text(acc.name)
                                    Text("${acc.type} · ${acc.currency}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }},
                                onClick = { toAccountId = acc.id; expandedTo = false }
                            )
                        }
                    }
                }

                // Amount
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) amount = it },
                    label = { Text("Amount") }, prefix = { Text(currency) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                if (fromAccount != null) {
                    Text(
                        "Available: $currency ${String.format("%.2f", fromAccount.balance)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (amountValue > fromAccount.balance) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Date & Time
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateDisplayFmt.format(Date(selectedDate)),
                        onValueChange = {}, readOnly = true, label = { Text("Date") },
                        trailingIcon = { Icon(Icons.Default.DateRange, null) },
                        modifier = Modifier.weight(1f).clickable { showDatePicker = true }
                    )
                    OutlinedTextField(
                        value = timeFmt.format(Date(selectedDate)),
                        onValueChange = {}, readOnly = true, label = { Text("Time") },
                        trailingIcon = { Icon(Icons.Default.Schedule, null) },
                        modifier = Modifier.weight(1f).clickable { showTimePicker = true }
                    )
                }

                // Note
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                if (fromAccountId == toAccountId && fromAccountId != null) {
                    Text("Source and destination must be different", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val from = fromAccountId ?: return@TextButton
                    val to = toAccountId ?: return@TextButton
                    val amt = amount.toDoubleOrNull() ?: return@TextButton
                    onTransfer(from, to, amt, note, selectedDate)
                },
                enabled = isValid
            ) { Text("Transfer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
