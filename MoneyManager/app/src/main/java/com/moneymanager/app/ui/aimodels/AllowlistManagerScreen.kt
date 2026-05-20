package com.moneymanager.app.ui.aimodels

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.ai.AllowedModel
import com.moneymanager.data.ai.AllowlistValidationResult
import com.moneymanager.data.ai.ModelAllowlistRepository
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class AllowlistUiState(
    val bundledModels: List<AllowedModel> = emptyList(),
    val currentUserJson: String = "",
    val isSaving: Boolean = false,
    val savedSnackbar: String? = null,
)

@HiltViewModel
class AllowlistViewModel @Inject constructor(
    private val allowlistRepository: ModelAllowlistRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(AllowlistUiState())
    val state: StateFlow<AllowlistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val bundled = allowlistRepository.loadBundled()
            val userJson = preferencesManager.getUserAllowlistJson()
            _state.value = AllowlistUiState(bundledModels = bundled, currentUserJson = userJson)
        }
    }

    fun validate(json: String): AllowlistValidationResult =
        allowlistRepository.validateUserJson(json)

    fun saveUserJson(json: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true)
            allowlistRepository.saveUserJson(json)
            _state.value = _state.value.copy(
                currentUserJson = json,
                isSaving = false,
                savedSnackbar = "Custom models saved",
            )
        }
    }

    fun resetToOriginal() {
        viewModelScope.launch {
            allowlistRepository.resetToOriginal()
            _state.value = _state.value.copy(
                currentUserJson = "",
                savedSnackbar = "Reset to original bundled models",
            )
        }
    }

    fun clearSnackbar() {
        _state.value = _state.value.copy(savedSnackbar = null)
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllowlistManagerScreen(
    onNavigateBack: () -> Unit,
    viewModel: AllowlistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedSnackbar) {
        if (state.savedSnackbar != null) {
            snackbarHostState.showSnackbar(state.savedSnackbar!!)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Model Allowlist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                // Custom JSON section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Custom Models", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add models not in the bundled list, or override existing ones. " +
                                "Use the same JSON format as the bundled allowlist.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (state.currentUserJson.isNotBlank()) {
                            val preview = try {
                                com.moneymanager.data.ai.parseAllowlistJson(state.currentUserJson)
                            } catch (_: Exception) { emptyList() }
                            Text(
                                "${preview.size} custom model(s) active",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showEditDialog = true }) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (state.currentUserJson.isBlank()) "Add Custom JSON" else "Edit JSON")
                            }
                            if (state.currentUserJson.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { showResetDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    ),
                                ) {
                                    Text("Reset")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "BUNDLED MODELS (${state.bundledModels.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            items(state.bundledModels, key = { it.name }) { model ->
                BundledModelRow(model)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }

            item {
                // JSON format hint card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Required fields for custom models:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "name, modelId, modelFile, commitHash, sizeInBytes, defaultConfig, taskTypes\n\n" +
                                "Format: JSON array [ {...}, {...} ] or { \"models\": [...] }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(64.dp)) }
        }

        if (showEditDialog) {
            JsonEditDialog(
                initialJson = state.currentUserJson,
                onDismiss = { showEditDialog = false },
                onValidate = viewModel::validate,
                onSave = { json ->
                    viewModel.saveUserJson(json)
                    showEditDialog = false
                },
            )
        }

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Reset to original?") },
                text = { Text("This will remove all custom models and revert to the bundled list.") },
                confirmButton = {
                    Button(
                        onClick = { viewModel.resetToOriginal(); showResetDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text("Reset") }
                },
                dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun BundledModelRow(model: AllowedModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(model.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "${"%.1f".format(model.sizeInBytes / 1_073_741_824f)} GB · ${model.minDeviceMemoryInGb ?: 6}+ GB RAM",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Default.Lock,
                contentDescription = "Read-only",
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun JsonEditDialog(
    initialJson: String,
    onDismiss: () -> Unit,
    onValidate: (String) -> AllowlistValidationResult,
    onSave: (String) -> Unit,
) {
    var json by remember { mutableStateOf(initialJson) }
    var validationResult by remember { mutableStateOf<AllowlistValidationResult?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm && validationResult is AllowlistValidationResult.Valid) {
        val v = validationResult as AllowlistValidationResult.Valid
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Confirm Import") },
            text = {
                Column {
                    if (v.newCount > 0) Text("• ${v.newCount} new model(s) to add")
                    if (v.overrideCount > 0) Text("• ${v.overrideCount} bundled model(s) to override")
                }
            },
            confirmButton = {
                Button(onClick = { onSave(json.trim()) }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancel") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Models JSON") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = json,
                    onValueChange = { json = it; validationResult = null },
                    label = { Text("JSON") },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    textStyle = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(8.dp))
                when (val r = validationResult) {
                    is AllowlistValidationResult.Valid -> {
                        Text(
                            "✓ Valid — ${r.newCount} new, ${r.overrideCount} override(s)",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    is AllowlistValidationResult.Error -> {
                        Text(
                            "✗ ${r.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    null -> {}
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val r = onValidate(json.trim())
                    validationResult = r
                    if (r is AllowlistValidationResult.Valid) showConfirm = true
                },
                enabled = json.isNotBlank(),
            ) { Text("Validate & Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
