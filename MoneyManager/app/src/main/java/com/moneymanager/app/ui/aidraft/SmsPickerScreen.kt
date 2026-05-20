package com.moneymanager.app.ui.aidraft

import android.provider.Telephony
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.app.ui.util.Permissions
import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val SMS_INBOX_ENABLED = false

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsPickerScreen(
    viewModel: AiDraftViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToConfirm: (TransactionDraft) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAiAvailable by viewModel.isAiAvailable.collectAsState()
    var smsText by rememberSaveable { mutableStateOf("") }
    var saveAsNote by rememberSaveable { mutableStateOf(false) }
    var senderFilter by rememberSaveable { mutableStateOf("HDFCBK, SBIINB, PAYTM, ICICIT, AXISBK, KOTAKB") }
    var selectedSmsSender by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                is NavigationEvent.NavigateToDraft -> {
                    val draft = if (saveAsNote && smsText.isNotBlank()) {
                        event.draft.copy(note = smsText.trim())
                    } else {
                        event.draft
                    }
                    onNavigateToConfirm(draft)
                }
                is NavigationEvent.NavigateToCreated -> {
                    snackbarHostState.showSnackbar(
                        "✓ Transaction added${if (event.message.isNotEmpty()) " (${event.message})" else ""}",
                        duration = SnackbarDuration.Short
                    )
                    delay(1500)
                    onNavigateBack()
                }
                NavigationEvent.NavigateBack -> onNavigateBack()
                is NavigationEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message, duration = SnackbarDuration.Short)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Draft from SMS") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = smsText,
                onValueChange = { smsText = it },
                label = { Text("Paste or type an SMS message") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                enabled = !uiState.isGenerating
            )

            var dotCount by remember { mutableStateOf(0) }
            LaunchedEffect(uiState.isGenerating) {
                if (uiState.isGenerating) {
                    dotCount = 0
                    while (true) {
                        delay(500)
                        dotCount = (dotCount + 1) % 4
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isAiAvailable) {
                    Button(
                        onClick = { viewModel.generateDraft(smsText.trim(), "SMS", selectedSmsSender) },
                        enabled = smsText.isNotBlank() && !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text("AI Fill")
                    }
                }
            }

            if (uiState.isGenerating) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${uiState.generatingStep ?: "Processing"}${".".repeat(dotCount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (SMS_INBOX_ENABLED) {
                val context = LocalContext.current
                var hasReadSmsPermission by remember { mutableStateOf(false) }
                var inboxMessages by remember { mutableStateOf<List<SmsMessage>>(emptyList()) }
                var isInboxLoading by remember { mutableStateOf(false) }
                var selectedMessage by remember { mutableStateOf<SmsMessage?>(null) }

                val readSmsPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { granted ->
                    hasReadSmsPermission = granted
                    if (granted) {
                        isInboxLoading = true
                        loadInboxMessages(context, senderFilter) { messages ->
                            inboxMessages = messages
                            isInboxLoading = false
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    hasReadSmsPermission = Permissions.isGranted(context, Permissions.READ_SMS)
                }

                OutlinedTextField(
                    value = senderFilter,
                    onValueChange = { senderFilter = it },
                    label = { Text("Sender filter") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !uiState.isGenerating
                )

                FilledTonalButton(
                    onClick = {
                        isInboxLoading = true
                        if (hasReadSmsPermission) {
                            loadInboxMessages(context, senderFilter) { messages ->
                                inboxMessages = messages
                                isInboxLoading = false
                            }
                        } else {
                            readSmsPermissionLauncher.launch(Permissions.READ_SMS)
                        }
                    },
                    enabled = !isInboxLoading && !uiState.isGenerating
                ) {
                    Text("Read from SMS Inbox")
                }

                if (isInboxLoading) {
                    CircularProgressIndicator()
                }

                if (!hasReadSmsPermission && !isInboxLoading) {
                    Text(
                        "SMS inbox unavailable. Use the paste field above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (hasReadSmsPermission && !isInboxLoading) {
                    if (inboxMessages.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text("No financial messages found", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(4.dp))
                            Text("Try adjusting the sender filter or paste an SMS message manually.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(inboxMessages) { message ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectedMessage == message) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    onClick = {
                                        selectedMessage = message
                                        smsText = message.body
                                        selectedSmsSender = message.sender
                                    }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(message.sender, style = MaterialTheme.typography.titleMedium)
                                            Text(message.timestamp, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            message.body.take(60) + if (message.body.length > 60) "..." else "",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (smsText.isBlank() && !SMS_INBOX_ENABLED) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("No SMS text entered", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text("Paste or type an SMS message above, then tap AI Fill to generate a draft.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save SMS text as note", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                Switch(
                    checked = saveAsNote,
                    onCheckedChange = { saveAsNote = it },
                    enabled = !uiState.isGenerating
                )
            }
        }
    }
}

private data class SmsMessage(
    val sender: String,
    val body: String,
    val timestamp: String,
    val dateMillis: Long
)

private fun loadInboxMessages(context: android.content.Context, senderFilter: String, onResult: (List<SmsMessage>) -> Unit) {
    val senders = senderFilter.split(",").map { it.trim() }
    val messages = mutableListOf<SmsMessage>()
    try {
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            null, null, null, "date DESC"
        )
        cursor?.use {
            val addressIndex = it.getColumnIndex(Telephony.Sms.Inbox.ADDRESS)
            val bodyIndex = it.getColumnIndex(Telephony.Sms.Inbox.BODY)
            val dateIndex = it.getColumnIndex(Telephony.Sms.Inbox.DATE)

            while (it.moveToNext() && messages.size < 50) {
                val address = if (addressIndex >= 0) it.getString(addressIndex) else ""
                val body = if (bodyIndex >= 0) it.getString(bodyIndex) else ""
                val dateMillis = if (dateIndex >= 0) it.getLong(dateIndex) else 0L

                val matchesFilter = senders.any { sender -> address.contains(sender, ignoreCase = true) }
                if (!matchesFilter) continue

                val dateFormat = java.text.SimpleDateFormat("dd MMM HH:mm", java.util.Locale.getDefault())
                val timestamp = dateFormat.format(java.util.Date(dateMillis))

                messages.add(SmsMessage(sender = address, body = body, timestamp = timestamp, dateMillis = dateMillis))
            }
        }
    } catch (_: Exception) {
    }

    val merged = mergeMultipartMessages(messages)
    onResult(merged.take(50))
}

private fun mergeMultipartMessages(messages: List<SmsMessage>): List<SmsMessage> {
    if (messages.isEmpty()) return messages
    val sorted = messages.sortedBy { it.dateMillis }
    val result = mutableListOf<SmsMessage>()
    var i = 0
    while (i < sorted.size) {
        val current = sorted[i]
        var j = i + 1
        var mergedBody = current.body
        while (j < sorted.size) {
            val next = sorted[j]
            if (next.sender == current.sender && (next.dateMillis - current.dateMillis) < 30 * 60 * 1000L) {
                mergedBody += "\n" + next.body
                j++
            } else {
                break
            }
        }
        result.add(SmsMessage(sender = current.sender, body = mergedBody, timestamp = current.timestamp, dateMillis = current.dateMillis))
        i = j
    }
    return result.sortedByDescending { it.dateMillis }
}
