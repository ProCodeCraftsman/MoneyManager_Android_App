package com.moneymanager.app.ui.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.domain.account.DefaultAccountSlot

private fun DefaultAccountSlot.title(): String = when (this) {
    DefaultAccountSlot.EXPENSE -> "Expense"
    DefaultAccountSlot.INCOME -> "Income"
    DefaultAccountSlot.TRANSFER_FROM -> "Transfer: from account"
    DefaultAccountSlot.TRANSFER_TO -> "Transfer: to account"
    DefaultAccountSlot.SAVINGS_FROM -> "Savings: from account"
    DefaultAccountSlot.SAVINGS_TO -> "Savings: investment account"
    DefaultAccountSlot.LEND -> "Lending"
    DefaultAccountSlot.BORROW -> "Borrowing"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DefaultAccountsScreen(
    viewModel: DefaultAccountsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<DefaultAccountRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Default accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Choose the account each new transaction starts with. Auto uses the account you use most for that type.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(uiState.rows, key = { it.slot.key }) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { editing = row },
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(row.slot.title(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        val summary = when {
                            row.configured != null -> row.configured.name
                            row.effective != null -> "Auto (${row.effective.name})"
                            else -> "No account available"
                        }
                        Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    editing?.let { row ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text(row.slot.title()) },
            text = {
                LazyColumn {
                    item {
                        ChoiceRow(
                            label = "Auto (most used)",
                            selected = row.configured == null,
                            onClick = { viewModel.setDefault(row.slot, null); editing = null }
                        )
                    }
                    items(row.choices, key = { it.id }) { account ->
                        ChoiceRow(
                            label = account.name,
                            selected = row.configured?.id == account.id,
                            onClick = { viewModel.setDefault(row.slot, account.id); editing = null }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { editing = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
