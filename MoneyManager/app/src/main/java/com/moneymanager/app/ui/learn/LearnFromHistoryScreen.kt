package com.moneymanager.app.ui.learn

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.Permissions
import com.moneymanager.domain.learn.HistoryLabels
import com.moneymanager.domain.learn.JoinCoverage
import com.moneymanager.domain.learn.MerchantLabel
import com.moneymanager.domain.learn.SourceLabel

/**
 * "Learned from your history" (F08 L2). Nothing is written until the user confirms; rows the app is not sure
 * about start unticked. Built for the personal flavor, where the inbox can be read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnFromHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: LearnFromHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted)
    }
    val onScan = {
        if (Permissions.isGranted(context, Permissions.READ_SMS)) viewModel.scan()
        else permissionLauncher.launch(Permissions.READ_SMS)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Learn from history") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        bottomBar = {
            if (state.phase == LearnPhase.Ready) {
                val picked = state.merchantPicks.size + state.sourcePicks.size
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = viewModel::confirm,
                        enabled = picked > 0,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) {
                        Text(if (picked == 0) "Nothing selected" else "Learn $picked")
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (state.phase) {
                LearnPhase.Idle -> IntroContent(permissionDenied = state.permissionDenied, onScan = onScan)
                LearnPhase.Scanning -> Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Reading the last 6 months of SMS on this phone",
                        modifier = Modifier.padding(top = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                LearnPhase.Ready -> state.labels?.let { labels ->
                    ReadyContent(
                        labels = labels,
                        merchantPicks = state.merchantPicks,
                        sourcePicks = state.sourcePicks,
                        onToggleMerchant = viewModel::toggleMerchant,
                        onToggleSource = viewModel::toggleSource,
                    )
                }
                LearnPhase.Saved -> SavedContent(count = state.savedCount, onDone = onNavigateBack)
            }
        }
    }
}

@Composable
private fun IntroContent(permissionDenied: Boolean, onScan: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Matches the last 6 months of bank and card SMS to the transactions you already saved, so merchants " +
                "and accounts are recognised from the first new message. Nothing is saved until you confirm, " +
                "and the messages themselves are not stored.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text("Scan my SMS") }
        if (permissionDenied) {
            Text(
                "SMS access was refused, so there is nothing to read. Allow it to learn from history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun ReadyContent(
    labels: HistoryLabels,
    merchantPicks: Set<String>,
    sourcePicks: Set<String>,
    onToggleMerchant: (String) -> Unit,
    onToggleSource: (String) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item { CoverageText(labels.coverage) }
        if (labels.merchants.isEmpty() && labels.sources.isEmpty()) {
            item {
                Text(
                    "No pairs were clear enough to learn from. Repeat purchases and spends with two possible matches " +
                        "are skipped on purpose.",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        if (labels.merchants.isNotEmpty()) {
            item { SectionTitle("Merchants") }
            items(labels.merchants, key = { "m:${it.merchantKey}" }) { label ->
                MerchantRow(label, checked = label.merchantKey in merchantPicks) { onToggleMerchant(label.merchantKey) }
                HorizontalDivider()
            }
        }
        if (labels.sources.isNotEmpty()) {
            item { SectionTitle("Accounts") }
            items(labels.sources, key = { "s:${it.sourceKey}" }) { label ->
                SourceRow(label, checked = label.sourceKey in sourcePicks) { onToggleSource(label.sourceKey) }
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun CoverageText(coverage: JoinCoverage) {
    val text = if (coverage.total == 0) {
        "No bank or card transactions in the last 6 months."
    } else {
        val percent = coverage.matched * 100 / coverage.total
        "${coverage.matched} of ${coverage.total} saved bank and card transactions ($percent%) have a matching SMS. " +
            "The rest were entered by hand or came from elsewhere."
    }
    Text(text, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun MerchantRow(label: MerchantLabel, checked: Boolean, onToggle: () -> Unit) {
    LabelRow(
        title = label.merchantHint.ifBlank { label.merchantKey },
        detail = "${label.categoryName} · ${label.count} of ${label.total}",
        checked = checked,
        onToggle = onToggle,
    )
}

@Composable
private fun SourceRow(label: SourceLabel, checked: Boolean, onToggle: () -> Unit) {
    LabelRow(
        title = prettySource(label.sourceKey),
        detail = "${label.accountName} · ${label.count} of ${label.total}",
        checked = checked,
        onToggle = onToggle,
    )
}

@Composable
private fun LabelRow(title: String, detail: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(
                "→ $detail",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedContent(count: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Learned $count ${if (count == 1) "rule" else "rules"}", style = MaterialTheme.typography.titleMedium)
        Text(
            "New SMS drafts will use what you kept.",
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Button(onClick = onDone) { Text("Done") }
    }
}

/** "hdfcbk|BANK_ACCOUNT|1234" -> "HDFCBK account ••1234". The key is built by SmsAccountLinks.keyFor. */
private fun prettySource(sourceKey: String): String {
    val parts = sourceKey.split("|")
    val institution = parts.getOrNull(0).orEmpty().uppercase()
    val instrument = when (parts.getOrNull(1)) {
        "CREDIT_CARD" -> "card"
        "BANK_ACCOUNT" -> "account"
        else -> parts.getOrNull(1).orEmpty().lowercase()
    }
    val last4 = parts.getOrNull(2).orEmpty()
    val digits = if (last4.isEmpty()) "" else "••$last4"
    return listOf(institution, instrument, digits).filter { it.isNotEmpty() }.joinToString(" ")
}
