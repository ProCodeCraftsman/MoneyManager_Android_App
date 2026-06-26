package com.moneymanager.app.ui.settings.backup

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.moneymanager.app.ui.settings.DriveBackupUiState
import com.moneymanager.app.ui.settings.DriveOpStatus
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DriveBackupSection(
    uiState: DriveBackupUiState,
    onBackup: (passphrase: String) -> Unit,
    onCheckRestore: () -> Unit,
    onRestore: (passphrase: String) -> Unit,
    onClearFoundBackup: () -> Unit,
    onAutoBackupToggle: (Boolean) -> Unit,
    onFrequencyChange: (Boolean) -> Unit,
    onClearDriveError: () -> Unit,
    onClearDriveOp: () -> Unit,
) {
    var showBackupPassphraseDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    DriveSettingsSectionHeader()

    if (!uiState.isSignedIn) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Sign in with Google above to enable backup",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    val rowsEnabled = uiState.isSignedIn && !uiState.isLoading

    DriveActionRow(
        icon = Icons.Default.CloudUpload,
        title = "Backup to Drive",
        subtitle = uiState.lastBackupTime?.let { "Last: ${dateFormat.format(Date(it))}" }
            ?: "No backup yet",
        enabled = rowsEnabled,
        isLoading = uiState.backupOpStatus is DriveOpStatus.InProgress,
        onClick = { showBackupPassphraseDialog = true }
    )

    DriveActionRow(
        icon = Icons.Default.CloudDownload,
        title = "Restore from Drive",
        subtitle = "Decrypt and import a saved backup",
        enabled = rowsEnabled,
        isLoading = uiState.restoreOpStatus is DriveOpStatus.InProgress,
        onClick = onCheckRestore
    )

    DriveToggleRow(
        icon = Icons.Default.Autorenew,
        title = "Auto-backup",
        subtitle = "Automatically back up in the background",
        checked = uiState.autoBackupEnabled,
        enabled = rowsEnabled,
        onCheckedChange = onAutoBackupToggle
    )

    if (uiState.autoBackupEnabled && rowsEnabled) {
        DriveFrequencyRow(
            isWeekly = uiState.backupWeekly,
            onFrequencyChange = onFrequencyChange
        )
    }

    if (showBackupPassphraseDialog) {
        PassphraseDialog(
            title = "Set Backup Passphrase",
            subtitle = "This passphrase encrypts your backup. You will need it to restore on any device.",
            confirmLabel = "Backup",
            onConfirm = { passphrase ->
                showBackupPassphraseDialog = false
                onBackup(passphrase)
            },
            onDismiss = { showBackupPassphraseDialog = false }
        )
    }

    if (uiState.foundBackupFile != null && uiState.restoreOpStatus == DriveOpStatus.Idle) {
        RestoreConfirmDialog(
            backupDate = runCatching {
                val iso = uiState.foundBackupFile.createdTime
                dateFormat.format(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).parse(iso)!!)
            }.getOrNull() ?: uiState.foundBackupFile.createdTime,
            onConfirm = { passphrase -> onRestore(passphrase) },
            onDismiss = onClearFoundBackup
        )
    }

    if (uiState.restoreOpStatus == DriveOpStatus.NoBackupFound) {
        AlertDialog(
            onDismissRequest = onClearDriveOp,
            title = { Text("No Backup Found") },
            text = { Text("No backup file was found in your Google Drive app folder. Run a manual backup first.") },
            confirmButton = { TextButton(onClick = onClearDriveOp) { Text("OK") } }
        )
    }

    val opStatus = when {
        uiState.backupOpStatus is DriveOpStatus.Success ||
            uiState.backupOpStatus is DriveOpStatus.Error -> uiState.backupOpStatus
        uiState.restoreOpStatus is DriveOpStatus.Success ||
            uiState.restoreOpStatus is DriveOpStatus.Error -> uiState.restoreOpStatus
        else -> null
    }
    opStatus?.let { status ->
        val isSuccess = status is DriveOpStatus.Success
        AlertDialog(
            onDismissRequest = onClearDriveOp,
            icon = {
                Icon(
                    if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            },
            title = { Text(if (isSuccess) "Success" else "Error") },
            text = {
                Text(
                    when (status) {
                        is DriveOpStatus.Success -> status.message
                        is DriveOpStatus.Error -> status.message
                        else -> ""
                    }
                )
            },
            confirmButton = { TextButton(onClick = onClearDriveOp) { Text("OK") } }
        )
    }

    uiState.error?.let { error ->
        AlertDialog(
            onDismissRequest = onClearDriveError,
            icon = {
                Icon(Icons.Default.Error, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error)
            },
            title = { Text("Sign-in Error") },
            text = { Text(error) },
            confirmButton = { TextButton(onClick = onClearDriveError) { Text("OK") } }
        )
    }
}

@Composable
private fun DriveSettingsSectionHeader() {
    Text(
        text = "Backup & Restore",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun DriveActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled && !isLoading,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconTint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            Icon(
                icon, contentDescription = null,
                modifier = Modifier.padding(end = 16.dp).size(24.dp),
                tint = iconTint
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.let {
                        if (enabled) it else it.copy(alpha = 0.38f)
                    }
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            }
        }
    }
}

@Composable
private fun DriveToggleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon, contentDescription = null,
            modifier = Modifier.padding(end = 16.dp).size(24.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.let {
                    if (enabled) it else it.copy(alpha = 0.38f)
                }
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun DriveFrequencyRow(isWeekly: Boolean, onFrequencyChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Frequency", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isWeekly, onClick = { onFrequencyChange(true) }, label = { Text("Weekly") })
            FilterChip(selected = !isWeekly, onClick = { onFrequencyChange(false) }, label = { Text("Daily") })
        }
    }
}

@Composable
private fun PassphraseDialog(
    title: String,
    subtitle: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; error = null },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                when {
                    passphrase.isBlank() -> error = "Passphrase cannot be empty"
                    passphrase.length < 6 -> error = "Passphrase must be at least 6 characters"
                    else -> onConfirm(passphrase)
                }
            }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RestoreConfirmDialog(
    backupDate: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(Icons.Default.CloudDownload, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        },
        title = { Text("Restore Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp))
                        Column {
                            Text("Backup found", style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(backupDate, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(
                    "This will replace your current data. Enter your backup passphrase to decrypt.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; error = null },
                    label = { Text("Passphrase") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (passphrase.isBlank()) error = "Passphrase cannot be empty"
                    else onConfirm(passphrase)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Restore") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
