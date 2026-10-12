package com.moneymanager.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.domain.capture.DraftEdits
import com.moneymanager.domain.capture.SourceEditStat

/**
 * Debug view for AI-014: per capture source, how many drafts were saved and how many went through unchanged, and which
 * fields were edited. Counts only; no amounts, names or messages are stored or shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftEditStatsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DraftEditStatsViewModel = hiltViewModel(),
) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Draft edit stats") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (stats.bySource.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No captured drafts saved yet. Counts appear after the first save from the inbox.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(stats.bySource.entries.toList(), key = { it.key }) { (source, stat) ->
                    SourceStatCard(source, stat)
                }
            }
        }
    }
}

@Composable
private fun SourceStatCard(source: String, stat: SourceEditStat) {
    val rate = if (stat.saves == 0) 0 else stat.zeroEditSaves * 100 / stat.saves
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(source, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${stat.saves} saved · ${stat.zeroEditSaves} unchanged ($rate%)",
                style = MaterialTheme.typography.bodyMedium,
            )
            if (stat.fieldEdits.isEmpty()) {
                Text("No field was edited.", style = MaterialTheme.typography.bodySmall)
            } else {
                stat.fieldEdits.entries.sortedByDescending { it.value }.forEach { (field, count) ->
                    Text(
                        "${fieldLabel(field)}: $count",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun fieldLabel(field: String): String = when (field) {
    DraftEdits.TYPE -> "Type"
    DraftEdits.AMOUNT -> "Amount"
    DraftEdits.CATEGORY -> "Category"
    DraftEdits.ACCOUNT -> "Account"
    DraftEdits.TO_ACCOUNT -> "To account"
    DraftEdits.DATE -> "Date"
    DraftEdits.NOTE -> "Note"
    DraftEdits.PEER -> "Person"
    DraftEdits.TAGS -> "Tags"
    else -> field
}
