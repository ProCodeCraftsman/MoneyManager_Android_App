package com.moneymanager.app.ui.emi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.TransactionEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmiDetailScreen(
    viewModel: EmiViewModel,
    emiId: Long,
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val emi = uiState.emis.find { it.id == emiId }
    val installments by viewModel.getInstallments(emiId).collectAsStateWithLifecycle(initialValue = emptyList())
    val currencyFormat = remember(uiState.currencyCode) { CurrencyUtils.getCurrencyFormat(uiState.currencyCode) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val coroutineScope = rememberCoroutineScope()
    var showForecloseDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(emi?.title ?: "EMI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (emi == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("EMI not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        val account = uiState.accounts.find { it.id == emi.accountId }
        val unpostedCount = installments.count { !it.postedToBalance }
        val remainingAmount = installments.filter { !it.postedToBalance }.sumOf { it.amount }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SummaryRow("Status") { StatusChip(status = emi.status) }
                        SummaryRow("Account", account?.name ?: "—")
                        SummaryRow("Total amount", currencyFormat.format(emi.totalAmount))
                        SummaryRow("Monthly installment", currencyFormat.format(emi.monthlyAmount))
                        SummaryRow("Tenure", "${emi.tenureMonths} months")
                        if (emi.annualInterestRate > 0) {
                            SummaryRow("Interest rate", "${emi.annualInterestRate}% p.a.")
                        }
                        if (emi.processingFee > 0) {
                            SummaryRow("Processing fee", currencyFormat.format(emi.processingFee))
                        }
                        SummaryRow("Remaining", "${unpostedCount} installments • ${currencyFormat.format(remainingAmount)}")
                    }
                }
            }

            if (emi.status == "ACTIVE" && unpostedCount > 0) {
                item {
                    OutlinedButton(
                        onClick = { showForecloseDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Foreclose EMI")
                    }
                }
            }

            item {
                Text(
                    text = "Installments",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                )
            }

            items(installments, key = { it.id }) { installment ->
                InstallmentRow(installment = installment, currencyFormat = currencyFormat, dateFormat = dateFormat)
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showForecloseDialog && emi != null) {
        val unpostedCount = installments.count { !it.postedToBalance }
        val remainingAmount = installments.filter { !it.postedToBalance }.sumOf { it.amount }
        AlertDialog(
            onDismissRequest = { showForecloseDialog = false },
            title = { Text("Foreclose EMI?") },
            text = {
                Text(
                    "This cancels the remaining $unpostedCount unpaid installment(s) " +
                        "(${currencyFormat.format(remainingAmount)}). Installments already posted to your " +
                        "account balance are kept as-is in your transaction history. This can't be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        viewModel.forecloseEmi(emi)
                    }
                    showForecloseDialog = false
                }) {
                    Text("Foreclose", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForecloseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SummaryRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
    }
}

@Composable
private fun InstallmentRow(
    installment: TransactionEntity,
    currencyFormat: java.text.NumberFormat,
    dateFormat: SimpleDateFormat,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (installment.postedToBalance) Icons.Filled.CheckCircle else Icons.Filled.Schedule,
            contentDescription = null,
            tint = if (installment.postedToBalance) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Installment ${installment.emiInstallmentNumber ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = dateFormat.format(Date(installment.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = currencyFormat.format(installment.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}
