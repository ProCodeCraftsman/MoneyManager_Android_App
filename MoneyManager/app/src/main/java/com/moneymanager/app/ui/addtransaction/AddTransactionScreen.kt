package com.moneymanager.app.ui.addtransaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.app.ui.dialogs.AddEditTransactionDialog
import com.moneymanager.domain.ai.TransactionDraft
import com.moneymanager.domain.capture.DraftEdits

@Composable
fun AddTransactionScreen(
    type: String?,
    initialDraft: TransactionDraft? = null,
    onDraftDismiss: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading) return

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AddEditTransactionDialog(
            transaction = null,
            currency = uiState.currency,
            categories = uiState.categories,
            tags = uiState.tags,
            accounts = uiState.accounts,
            peers = uiState.peers,
            goals = uiState.goals,
            initialType = type,
            categoryUsageCounts = uiState.categoryUsageCounts,
            imageAttachmentsEnabled = uiState.imageAttachmentsEnabled,
            defaultAccounts = uiState.defaultAccounts,
            accountUsage = uiState.accountUsage,
            onDismiss = onDismiss,
            initialDraft = initialDraft,
            onDraftDismiss = onDraftDismiss,
            onConfirm = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children, initialDraft?.captureId)
                else viewModel.addTransaction(tx, initialDraft?.captureId)
                // Edit tracking (AI-014): only drafts from a capture source are counted.
                if (initialDraft?.sourceType != null) {
                    viewModel.recordDraftSave(initialDraft.sourceType, DraftEdits.changedFields(initialDraft, tx))
                }
                // Correction learning: record what the user actually saved so future
                // same-merchant lookups reflect real preferences, not just AI guesses.
                val hint = initialDraft?.merchantHint
                if (hint != null && tx.categoryId != null) {
                    viewModel.recordMerchantCategory(hint, tx.categoryId, tx.type)
                }
                initialDraft?.accountLink?.let { viewModel.recordSmsAccountLink(it, tx) }
                onDismiss()
            },
            onConfirmEmi = { tx, tenure, rate, isNoCost, fee ->
                viewModel.addEmiExpense(tx, tenure, rate, isNoCost, fee)
                onDismiss()
            },
        )
    }
}
