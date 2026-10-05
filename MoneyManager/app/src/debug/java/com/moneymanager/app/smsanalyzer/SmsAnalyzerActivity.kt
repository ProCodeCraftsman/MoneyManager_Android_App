package com.moneymanager.app.smsanalyzer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.moneymanager.app.ui.theme.MoneyManagerTheme
import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.repository.SmsAccountLinkRepository
import com.moneymanager.domain.ai.AccountEntry
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SmsAnalyzerActivity : ComponentActivity() {
    @Inject lateinit var accountDao: AccountDao
    @Inject lateinit var merchantMemory: MerchantCategoryMemoryRepository
    @Inject lateinit var accountLinks: SmsAccountLinkRepository

    /** The user's real accounts and merchant memory, so the scan can measure how editable drafts would be. */
    private suspend fun draftContext(): DraftContext = withContext(Dispatchers.IO) {
        val accounts = accountDao.getActiveAccounts().first().map { AccountEntry(it.id, it.name, it.type) }
        val cache = HashMap<Pair<String, String>, MemoryHit?>()
        val links = accountLinks.load()
        DraftContext(accounts, links) { hint, typeId ->
            cache.getOrPut(hint to typeId) {
                runBlocking { merchantMemory.lookupWithHistory(hint, typeId) }?.let { MemoryHit(it.typeId) }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MoneyManagerTheme { Scaffold { p -> AnalyzerScreen(Modifier.padding(p), ::draftContext) } } }
    }
}

@Composable
private fun AnalyzerScreen(modifier: Modifier, draftContext: suspend () -> DraftContext) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var report by remember { mutableStateOf<AnalysisReport?>(null) }
    var running by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0) }
    var onlyGaps by remember { mutableStateOf(true) }
    var onlyDraftIssues by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }

    fun run() {
        running = true
        scope.launch {
            try {
                report = SmsInboxReader.analyze(context, nameInput.split(' ', ',').filter { it.isNotBlank() }, draftContext()) { progress = it }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                running = false
            }
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) run() else Toast.makeText(context, "READ_SMS denied", Toast.LENGTH_SHORT).show()
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val r = report ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(r.toJson().toByteArray()) }
            Toast.makeText(context, "Exported", Toast.LENGTH_SHORT).show()
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("SMS Pattern Analyzer", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Runs on this device only. Raw messages are never stored or exported — only masked templates.",
            style = MaterialTheme.typography.bodySmall,
        )
        OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("Your name words (masked in output)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = !running, onClick = {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) run() else permission.launch(Manifest.permission.READ_SMS)
            }) { Text(if (running) "Scanning… $progress" else "Scan inbox") }
            OutlinedButton(enabled = report != null, onClick = { export.launch("sms_insights.json") }) {
                Text("Export JSON")
            }
        }

        val r = report
        if (r != null) {
            val pct = if (r.transactionMessages == 0) 0 else r.fullyParsedMessages * 100 / r.transactionMessages
            Text(
                "Scanned ${r.scanned} (skipped ${r.skippedPersonal} personal) • ${r.templates.size} templates\n" +
                    "Transaction SMS: ${r.transactionMessages} • fully parsed by baseline: $pct%",
                style = MaterialTheme.typography.bodyMedium,
            )
            r.draftQuality?.let { q ->
                fun pc(n: Int, of: Int) = if (of == 0) "-" else "${n * 100 / of}%"
                Text(
                    "Drafts from rules: ${q.drafts} • ready to post: ${pc(q.ready, q.drafts)}\n" +
                        "account matched: ${pc(q.accountMatched, q.drafts)} • category from memory: " +
                        "${pc(q.categoryFilled, q.categoryNeeded)} • card repayments: ${q.transfers}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            r.verifyIssues.takeIf { it.isNotEmpty() }?.let { issues ->
                Text(
                    "Not backed by the text: " + issues.entries.joinToString { "${it.key.lowercase()} ${it.value}" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = onlyGaps, onClick = { onlyGaps = !onlyGaps; if (onlyGaps) onlyDraftIssues = false }, label = { Text("Parser gaps") })
                FilterChip(selected = onlyDraftIssues, onClick = { onlyDraftIssues = !onlyDraftIssues; if (onlyDraftIssues) onlyGaps = false }, label = { Text("Draft needs edits") })
            }
            val shown = when {
                onlyDraftIssues -> r.templates.filter { (it.quality?.notReady ?: 0) > 0 }.sortedByDescending { it.quality?.notReady }
                onlyGaps -> r.gapTemplates
                else -> r.templates
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown) { t -> TemplateCard(t) }
            }
        }
    }
}

@Composable
private fun TemplateCard(t: TemplateInsight) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "${t.sender} • ${t.count}× • ${t.category} • ${t.status}",
                style = MaterialTheme.typography.labelLarge,
            )
            Text(t.template, style = MaterialTheme.typography.bodySmall)
            Text(
                "fields: " + t.fieldHits.entries.joinToString { "${it.key} ${it.value}" },
                style = MaterialTheme.typography.labelSmall,
            )
            t.quality?.let { q ->
                Text(
                    "drafts ${q.drafts}: ready ${q.ready} • account ${q.accountMatched}/${q.drafts}" +
                        (if (q.accountHintedButUnmatched > 0) " (${q.accountHintedButUnmatched} hinted, no match)" else "") +
                        " • category ${q.categoryFilled}/${q.categoryNeeded}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}
