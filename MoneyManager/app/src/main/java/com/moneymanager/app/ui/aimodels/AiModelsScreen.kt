package com.moneymanager.app.ui.aimodels

import androidx.compose.animation.animateContentSize
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
    onNavigateToAllowlistManager: () -> Unit = {},
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
                },
                actions = {
                    IconButton(onClick = onNavigateToAllowlistManager) {
                        Icon(Icons.Default.Tune, contentDescription = "Manage allowlist")
                    }
                }
            )
        },
    ) { padding ->
        if (showHfLoginDialog) {
            HuggingFaceLoginDialog(
                currentToken = uiState.hfAccessToken,
                onDismiss = { showHfLoginDialog = false },
                onOpenHf = { viewModel.openHuggingFaceModelAgreement() },
                onOpenTokenPage = { viewModel.openHuggingFaceLogin() },
                onTokenEntered = { token ->
                    showHfLoginDialog = false
                    viewModel.downloadModelWithToken(token)
                },
                onClearToken = { viewModel.clearHuggingFaceToken() },
            )
        }
        if (showHfTokenDialog) {
            HuggingFaceTokenDialog(
                currentToken = uiState.hfAccessToken,
                onDismiss = { showHfTokenDialog = false },
                onSave = { token -> viewModel.setHuggingFaceToken(token); showHfTokenDialog = false },
                onClear = { viewModel.clearHuggingFaceToken(); showHfTokenDialog = false },
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                BackendStatusCard(
                    backendTier = uiState.backendTier,
                    aiStatus = uiState.aiStatus,
                    aiDownloadProgress = uiState.aiDownloadProgress,
                    isLocalModelDownloaded = uiState.isLocalModelDownloaded,
                    selectedLocalModel = uiState.selectedLocalModel,
                    isDownloading = uiState.downloadingModelNames.isNotEmpty(),
                    localModelDownloadProgress = uiState.localModelDownloadProgress,
                    onCheckStatus = { viewModel.checkAiStatus() },
                )
            }

            if (uiState.backendTier == AiBackend.LOCAL_MODEL) {
                item {
                    WifiOnlyRow(
                        checked = uiState.wifiOnlyDownload,
                        onCheckedChange = { viewModel.setWifiOnlyDownload(it) },
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
                Row(
                    modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "AVAILABLE MODELS (${uiState.allModels.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    if (uiState.downloadingModelNames.isNotEmpty()) {
                        Text(
                            "${uiState.downloadingModelNames.size} downloading",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
            }

            items(uiState.allModels, key = { it.name }) { model ->
                ModelCard(
                    model = model,
                    isDownloaded = uiState.modelDownloadStates[model.name] == true,
                    isSelected = uiState.selectedLocalModel?.name == model.name,
                    isDownloading = uiState.downloadingModelNames.contains(model.name),
                    isUpdatable = uiState.updatableModelNames.contains(model.name),
                    downloadProgress = uiState.modelProgress[model.name],
                    backendTier = uiState.backendTier,
                    onDownload = { viewModel.downloadModel(model) },
                    onDelete = { viewModel.deleteModel(model) },
                    onSelect = { viewModel.setSelectedModel(model) },
                    onCancel = { viewModel.cancelDownload(model) },
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun ModelCard(
    model: ModelEntry,
    isDownloaded: Boolean,
    isSelected: Boolean,
    isDownloading: Boolean,
    isUpdatable: Boolean,
    downloadProgress: ModelDownloadProgress?,
    backendTier: AiBackend,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val showActions = backendTier == AiBackend.LOCAL_MODEL

    val containerColor = when {
        isSelected && showActions -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = model.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (isSelected && showActions) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        }
                        if (isDownloaded) {
                            Icon(Icons.Default.CloudDone, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        if (isUpdatable) {
                            Icon(Icons.Default.Update, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${"%.1f".format(model.sizeGb)} GB · ${model.minRamGb}+ GB RAM",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (model.capabilities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            model.capabilities.forEach { cap ->
                                CapabilityChip(cap)
                            }
                            if (model.isMultimodal) CapabilityChip("multimodal")
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (showActions) {
                    when {
                        isDownloading -> {
                            IconButton(onClick = onCancel) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                        isDownloaded -> {
                            Row {
                                if (!isSelected) {
                                    IconButton(onClick = onSelect) {
                                        Icon(Icons.Default.PlayArrow, "Use model", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = onDelete) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        isUpdatable -> {
                            IconButton(onClick = onDownload) {
                                Icon(Icons.Default.Update, "Update model", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        else -> {
                            IconButton(onClick = onDownload) {
                                Icon(Icons.Default.Download, "Download")
                            }
                        }
                    }
                }

                // Expand toggle for description
                if (model.description.isNotBlank()) {
                    IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Download progress
            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                val progress = downloadProgress?.progress ?: 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
                if (downloadProgress != null && downloadProgress.totalBytes > 0L) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        buildDownloadProgressText(downloadProgress),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expanded description
            if (expanded && model.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    model.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isUpdatable && model.updateInfo.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Update: ${model.updateInfo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CapabilityChip(label: String) {
    val display = when (label) {
        "llm_thinking" -> "Thinking"
        "speculative_decoding" -> "Fast decode"
        "multimodal" -> "Multimodal"
        else -> label
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            display,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun buildDownloadProgressText(info: ModelDownloadProgress): String {
    val receivedMb = "%.1f".format(info.receivedBytes / 1_000_000.0)
    val totalMb = "%.0f".format(info.totalBytes / 1_000_000.0)
    val pct = (info.progress * 100).toInt()
    val speedText = when {
        info.bytesPerSecond > 1_000_000L -> "${"%.1f".format(info.bytesPerSecond / 1_000_000.0)} MB/s"
        info.bytesPerSecond > 1_000L -> "${"%.0f".format(info.bytesPerSecond / 1_000.0)} KB/s"
        info.bytesPerSecond > 0L -> "${info.bytesPerSecond} B/s"
        else -> null
    }
    val etaText = if (info.remainingMs > 0L) {
        val s = info.remainingMs / 1000L
        when {
            s > 3600L -> "${s / 3600}h ${(s % 3600) / 60}m"
            s > 60L -> "${s / 60}m ${s % 60}s"
            else -> "${s}s"
        }
    } else null
    return buildString {
        append("$receivedMb / $totalMb MB ($pct%)")
        if (speedText != null) append(" — $speedText")
        if (etaText != null) append(" — $etaText left")
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val (icon, label, subtitle) = when (backendTier) {
                AiBackend.AICORE -> Triple(Icons.Default.AutoAwesome, "AI Backend: Gemini Nano",
                    when (aiStatus) {
                        "READY" -> "Ready to use"
                        "PENDING" -> if (aiDownloadProgress > 0) "Downloading: ${(aiDownloadProgress * 100).toInt()}%" else "Preparing…"
                        else -> "Unavailable"
                    })
                AiBackend.LOCAL_MODEL -> {
                    val m = selectedLocalModel
                    if (m == null) Triple(Icons.Default.Storage, "AI Backend: Local Model", "Checking…")
                    else {
                        val status = if (isLocalModelDownloaded) "ready" else "not downloaded"
                        Triple(Icons.Default.Storage, "Active: ${m.name}", "${"%.1f".format(m.sizeGb)} GB — $status")
                    }
                }
                AiBackend.NONE -> Triple(Icons.Default.Info, "AI Backend: Unavailable", "Device requires min 6 GB RAM")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (backendTier == AiBackend.AICORE && aiStatus != "READY") {
                    IconButton(onClick = onCheckStatus) { Icon(Icons.Default.Refresh, "Retry") }
                }
            }
            if (backendTier == AiBackend.LOCAL_MODEL && isDownloading && localModelDownloadProgress in 0.01f..0.99f) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(progress = { localModelDownloadProgress }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
            }
        }
    }
}

@Composable
private fun WifiOnlyRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Wifi, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Wi-Fi only", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(if (checked) "Download on Wi-Fi only" else "Download on any network", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HuggingFaceTokenRow(hasToken: Boolean, onLoginClick: () -> Unit, onManageToken: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(if (hasToken) "HuggingFace Token" else "HuggingFace Login", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(if (hasToken) "Token configured" else "Required for gated models", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasToken) TextButton(onClick = onManageToken) { Text("Manage") }
        else Button(onClick = onLoginClick) { Text("Login") }
    }
}

@Composable
private fun HuggingFaceLoginDialog(
    currentToken: String,
    onDismiss: () -> Unit,
    onOpenHf: () -> Unit,
    onOpenTokenPage: () -> Unit,
    onTokenEntered: (String) -> Unit,
    onClearToken: () -> Unit,
) {
    var tokenInput by remember { mutableStateOf(currentToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, null) },
        title = { Text("HuggingFace Access") },
        text = {
            Column {
                Text("This model requires a HuggingFace access token.", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("1. Accept the license at the model page", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onOpenHf) { Text("Open model page") }
                Text("2. Generate a token in HuggingFace settings", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onOpenTokenPage) { Text("Open token settings") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("HuggingFace Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (currentToken.isNotEmpty()) {
                    TextButton(
                        onClick = { onClearToken(); tokenInput = "" },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("Clear stored token", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onTokenEntered(tokenInput.trim()) }, enabled = tokenInput.isNotBlank()) { Text("Validate & Download") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun HuggingFaceTokenDialog(currentToken: String, onDismiss: () -> Unit, onSave: (String) -> Unit, onClear: () -> Unit) {
    var tokenInput by remember { mutableStateOf(currentToken) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, null) },
        title = { Text("Manage Token") },
        text = { OutlinedTextField(value = tokenInput, onValueChange = { tokenInput = it }, label = { Text("HuggingFace Token") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onSave(tokenInput.trim()) }, enabled = tokenInput.isNotBlank()) { Text("Save") } },
        dismissButton = {
            Row {
                if (currentToken.isNotEmpty()) TextButton(onClick = onClear) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
