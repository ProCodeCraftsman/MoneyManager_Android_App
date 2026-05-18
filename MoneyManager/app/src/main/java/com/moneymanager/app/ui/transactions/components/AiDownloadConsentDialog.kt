package com.moneymanager.app.ui.transactions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Opt-in consent dialog shown when the device supports local AI model download
 * but the user has not yet opted in (HYBRID-05).
 *
 * - Scrim/back tap is treated identically to "Maybe Later" (onMaybeLater called).
 * - "Download (529 MB)" tap calls onDownload — no download is initiated before this.
 * - "Maybe Later" tap calls onMaybeLater — no DataStore write is made.
 */
@Composable
fun AiDownloadConsentDialog(
    onDownload: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onMaybeLater,
        icon = {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI feature",
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        title = {
            Text("Enable On-Device AI?")
        },
        text = {
            Column {
                Text(
                    text = "All AI processing happens entirely on your device. " +
                        "Your financial data is never sent to any server.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Requires a one-time 529 MB download. Wi-Fi recommended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDownload) {
                Text("Download (529 MB)")
            }
        },
        dismissButton = {
            TextButton(onClick = onMaybeLater) {
                Text("Maybe Later")
            }
        },
    )
}
