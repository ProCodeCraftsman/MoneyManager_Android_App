package com.moneymanager.app.ui.aimodels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.ai.ModelEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiModelsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiModelsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHfLoginDialog by remember { mutableStateOf(false) }
    var showHfTokenDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AiModelsEvent.Snackbar -> snackbarHostState.showSnackbar(event.message)
                is AiModelsEvent.NeedsHfLogin -> showHfLoginDialog = true
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("AI Models") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.importModel() },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.secondary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import model")
            }
        }
    ) { padding ->
        if (showHfLoginDialog) {
            HuggingFaceLoginDialog(
                onDismiss = { showHfLoginDialog = false },
                onOpenHf = { viewModel.openHuggingFaceModelAgreement() },
                onOpenTokenPage = { viewModel.openHuggingFaceLogin() },
                onTokenEntered = { token ->
                    showHfLoginDialog = false
                    val model = uiState.selectedLocalModel ?: return@HuggingFaceLoginDialog
                    viewModel.downloadModelWithToken(model, token)
                },
            )
        }
        if (showHfTokenDialog) {
            HuggingFaceTokenDialog(
                currentToken = uiState.hfAccessToken,
                onDismiss = { showHfTokenDialog = false },
                onSave = { token ->
                    viewModel.setHuggingFaceToken(token)
                    showHfTokenDialog = false
                },
                onClear = {
                    viewModel.clearHuggingFaceToken()
                    showHfTokenDialog = false
                },
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                BackendStatusCard(
                    backendTier = uiState.backendTier,
                    aiStatus = uiState.aiStatus,
                    aiDownloadProgress = uiState.aiDownloadProgress,
                    isLocalModelDownloaded = uiState.isLocalModelDownloaded,
                    selectedLocalModel = uiState.selectedLocalModel,
                    isDownloading = uiState.downloadingModelName != null,
                    localModelDownloadProgress = uiState.localModelDownloadProgress,
                    onCheckStatus = { viewModel.checkAiStatus() },
                )
            }

            if (uiState.backendTier == AiBackend.LOCAL_MODEL) {
                item {
                    WifiOnlyRow(
                        checked = uiState.wifiOnlyDownload,
                        onCheckedChange = { viewModel.setWifiOnlyDownload(it) }
                    )
                }
                item {
                    HuggingFaceTokenRow(
                        hasToken = uiState.isHfTokenValid,
                        onLoginClick = { showHfLoginDialog = true },
                        onManageToken = { showHfTokenDialog = true },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AVAILABLE MODELS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                )
            }

            items(uiState.allModels, key = { it.name }) { model ->
                ModelCard(
                    model = model,
                    isDownloaded = uiState.modelDownloadStates[model.name] == true,
                    isSelected = uiState.selectedLocalModel?.name == model.name,
                    isDownloading = uiState.downloadingModelName == model.name,
                    downloadProgress = uiState.modelProgress[model.name]?.progress
                        ?: uiState.localModelDownloadProgress,
                    downloadProgressInfo = uiState.modelProgress[model.name],
                    backendTier = uiState.backendTier,
                    onDownload = { viewModel.downloadModel(model) },
                    onDelete = { viewModel.deleteModel(model) },
                    onSelect = { viewModel.setSelectedModel(model) },
                )
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun BackendStatusCard(
    backendTier: AiBackend,
    aiStatus: String,
    aiDownloadProgress: Float,
    isLocalModelDownloaded: Boolean,
    selectedLocalModel: ModelEntry?,
    isDownloading: Boolean,
    localModelDownloadProgress: Float,
    onCheckStatus: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val (icon, label, subtitle) = when (backendTier) {
                AiBackend.AICORE -> Triple(
                    Icons.Default.AutoAwesome,
                    "AI Backend: Gemini Nano",
                    when (aiStatus) {
                        "READY" -> "Ready to use"
                        "PENDING" -> if (aiDownloadProgress > 0)
                            "Downloading: ${(aiDownloadProgress * 100).toInt()}%"
                            else "Preparing model..."
                        else -> "Unavailable"
                    }
                )
                AiBackend.LOCAL_MODEL -> {
                    val m = selectedLocalModel
                    if (m == null) {
                        Triple(
                            Icons.Default.Storage,
                            "AI Backend: Local Model",
                            "Checking status..."
                        )
                    } else {
                        val ready = if (isLocalModelDownloaded) "ready" else "not downloaded"
                        Triple(
                            Icons.Default.Storage,
                            "AI Backend: ${m.modelFile}",
                            if (isLocalModelDownloaded)
                                "${"%.0f".format(m.sizeBytes / 1_000_000.0)} MB — $ready"
                            else "Tap download below (${"%.0f".format(m.sizeBytes / 1_000_000.0)} MB)"
                        )
                    }
                }
                AiBackend.NONE -> Triple(
                    Icons.Default.Info,
                    "AI Backend: Unavailable",
                    "Your device does not meet requirements (min 6 GB RAM)"
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (backendTier == AiBackend.AICORE && aiStatus != "READY") {
                    IconButton(onClick = onCheckStatus) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    }
                }
            }

            if (backendTier == AiBackend.AICORE &&
                aiStatus == "PENDING" &&
                aiDownloadProgress > 0f && aiDownloadProgress < 1f
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { aiDownloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }

            if (backendTier == AiBackend.LOCAL_MODEL &&
                isDownloading &&
                localModelDownloadProgress > 0f && localModelDownloadProgress < 1f
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { localModelDownloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}

@Composable
private fun WifiOnlyRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Wi-Fi only",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (checked) "Download only on Wi-Fi" else "Download on any network",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ModelCard(
    model: ModelEntry,
    isDownloaded: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    downloadProgressInfo: ModelDownloadProgress?,
    backendTier: AiBackend,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
) {
    val showActions = backendTier == AiBackend.LOCAL_MODEL
    val sizeMb = "%.0f".format(model.sizeBytes / 1_000_000.0)
    val containerColor = if (isSelected)
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    else
        MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = showActions) { onSelect() },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = model.modelFile,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isSelected && showActions) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Active model",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (isDownloaded) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${sizeMb} MB — needs ${model.minRamGb}+ GB RAM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (showActions) {
                    Spacer(modifier = Modifier.width(8.dp))
                    when {
                        isDownloading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        isDownloaded -> {
                            IconButton(onClick = onDelete) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete model",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            IconButton(onClick = onDownload) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download model"
                                )
                            }
                        }
                    }
                }
            }

            if (isDownloading && downloadProgress > 0f && downloadProgress < 1f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                )
                val info = downloadProgressInfo
                if (info != null && info.totalBytes > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = buildDownloadProgressText(info),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun buildDownloadProgressText(info: ModelDownloadProgress): String {
    val receivedMb = "%.1f".format(info.receivedBytes / 1_000_000.0)
    val totalMb = "%.0f".format(info.totalBytes / 1_000_000.0)
    val pct = (info.progress * 100).toInt()
    val speedText = if (info.bytesPerSecond > 0L) {
        if (info.bytesPerSecond > 1_000_000L) {
            "${"%.1f".format(info.bytesPerSecond / 1_000_000.0)} MB/s"
        } else if (info.bytesPerSecond > 1_000L) {
            "${"%.0f".format(info.bytesPerSecond / 1_000.0)} KB/s"
        } else {
            "${info.bytesPerSecond} B/s"
        }
    } else null
    val etaText = if (info.remainingMs > 0L) {
        val secs = info.remainingMs / 1000L
        if (secs > 3600L) "${secs / 3600}h ${(secs % 3600) / 60}m"
        else if (secs > 60L) "${secs / 60}m ${secs % 60}s"
        else "${secs}s"
    } else null

    return buildString {
        append("$receivedMb / $totalMb MB ($pct%)")
        if (speedText != null) append(" — $speedText")
        if (etaText != null) append(" — $etaText remaining")
    }
}

@Composable
private fun HuggingFaceTokenRow(
    hasToken: Boolean,
    onLoginClick: () -> Unit,
    onManageToken: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (hasToken) "HuggingFace Token" else "HuggingFace Login",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (hasToken) "Token configured" else "Required for gated models",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (hasToken) {
            TextButton(onClick = onManageToken) {
                Text("Manage")
            }
        } else {
            Button(onClick = onLoginClick) {
                Text("Login")
            }
        }
    }
}

@Composable
private fun HuggingFaceLoginDialog(
    onDismiss: () -> Unit,
    onOpenHf: () -> Unit,
    onOpenTokenPage: () -> Unit,
    onTokenEntered: (String) -> Unit,
) {
    var tokenInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text("HuggingFace Access") },
        text = {
            Column {
                Text(
                    "This model requires a HuggingFace access token. " +
                        "You need to:",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("1. Accept the license at the model page", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onOpenHf) {
                    Text("Open model page")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("2. Generate a token in HuggingFace settings", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onOpenTokenPage) {
                    Text("Open token settings")
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("HuggingFace Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onTokenEntered(tokenInput.trim()) },
                enabled = tokenInput.isNotBlank(),
            ) {
                Text("Download")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun HuggingFaceTokenDialog(
    currentToken: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
) {
    var tokenInput by remember { mutableStateOf(currentToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text("Manage Token") },
        text = {
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("HuggingFace Token") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(tokenInput.trim()) },
                enabled = tokenInput.isNotBlank(),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (currentToken.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        },
    )
}
