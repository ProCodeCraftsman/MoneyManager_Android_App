package com.moneymanager.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.domain.capture.AccountSourceSummary

/**
 * Account Sources (AI-055): each SMS source with waiting drafts is mapped once to an account, or to a new one.
 * The mapping is remembered for later messages and applied to the drafts already waiting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSourcesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AccountSourcesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var picking by remember { mutableStateOf<AccountSourceSummary?>(null) }
    var creating by remember { mutableStateOf<AccountSourceSummary?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account sources") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            state.message?.let {
                Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
            }
            if (state.sources.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Every bank and card message already has an account. Sources appear here when a new one needs one.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                    items(state.sources, key = { it.key }) { source ->
                        SourceCard(
                            source = source,
                            mappedName = state.accounts.firstOrNull { it.id == source.mappedAccountId }?.name,
                            onChange = { picking = source },
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    picking?.let { source ->
        AccountPickerDialog(
            accounts = state.accounts,
            onPick = { account ->
                viewModel.mapTo(source, account)
                picking = null
            },
            onCreate = {
                creating = source
                picking = null
            },
            onDismiss = { picking = null },
        )
    }

    creating?.let { source ->
        CreateAccountDialog(
            suggestedName = source.label.substringBefore(" ·"),
            suggestedType = source.suggestedAccountType,
            onCreate = { name, type ->
                viewModel.createAndMap(source, name, type)
                creating = null
            },
            onDismiss = { creating = null },
        )
    }
}

@Composable
private fun SourceCard(source: AccountSourceSummary, mappedName: String?, onChange: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(source.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                if (mappedName != null) "${source.draftCount} waiting · Filed under $mappedName"
                else "${source.draftCount} waiting · Not mapped yet",
                style = MaterialTheme.typography.bodySmall,
                color = if (mappedName == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onChange) { Text(if (mappedName == null) "Map account" else "Change") }
            }
        }
    }
}

@Composable
private fun AccountPickerDialog(
    accounts: List<AccountEntity>,
    onPick: (AccountEntity) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File this source under") },
        text = {
            Column {
                accounts.forEach { account ->
                    TextButton(onClick = { onPick(account) }, modifier = Modifier.fillMaxWidth()) {
                        Text(account.name, modifier = Modifier.fillMaxWidth())
                    }
                }
                TextButton(onClick = onCreate, modifier = Modifier.fillMaxWidth()) { Text("Create a new account") }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun CreateAccountDialog(
    suggestedName: String,
    suggestedType: String,
    onCreate: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(suggestedName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New account") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Opens at zero. Set the real opening balance on the account afterwards.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, suggestedType) }, enabled = name.isNotBlank()) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
