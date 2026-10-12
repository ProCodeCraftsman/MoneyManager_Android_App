package com.moneymanager.app.ui.capture

import android.speech.SpeechRecognizer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.BuildConfig

/**
 * "Add from…" (AI-056): replaces the AI dropdown on the transactions screen. The SMS inbox leads and needs no model;
 * receipt and voice still need the on-device AI. Capture tools (learn from history, account sources, edit stats)
 * only show in builds that can read the inbox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFromScreen(
    onNavigateBack: () -> Unit,
    onOpenSmsInbox: () -> Unit,
    onOpenReceipt: () -> Unit,
    onOpenVoice: () -> Unit,
    onOpenLearnFromHistory: () -> Unit,
    onOpenAccountSources: () -> Unit,
    onOpenEditStats: () -> Unit,
    viewModel: CaptureHubViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val aiTools = state.aiAvailable
    val tiles = buildList {
        if (BuildConfig.SMS_CAPTURE) add(
            Tile("SMS inbox", pendingSubtitle(state.pendingCount), Icons.Default.Sms, onOpenSmsInbox),
        )
        if (aiTools && state.imageAttachments) add(Tile("Receipt", "Photo of a bill, read on this phone", Icons.Default.Receipt, onOpenReceipt))
        if (aiTools && SpeechRecognizer.isRecognitionAvailable(context)) {
            add(Tile("Voice", "Say the expense out loud", Icons.Default.Mic, onOpenVoice))
        }
        if (BuildConfig.SMS_CAPTURE) {
            add(Tile("Learn from history", "Match past SMS to saved transactions", Icons.Default.History, onOpenLearnFromHistory))
            add(Tile("Account sources", "Map each bank or card to an account", Icons.Default.AccountBalance, onOpenAccountSources))
            add(Tile("Draft edit stats", "How often drafts are saved unchanged", Icons.Default.BarChart, onOpenEditStats))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add from") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            items(tiles, key = { it.title }) { tile ->
                TileCard(tile)
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

private data class Tile(val title: String, val subtitle: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun TileCard(tile: Tile) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = tile.onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(tile.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(tile.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(tile.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun pendingSubtitle(count: Int): String = when (count) {
    0 -> "Nothing waiting. Scan for new messages."
    1 -> "1 draft waiting to review"
    else -> "$count drafts waiting to review"
}
