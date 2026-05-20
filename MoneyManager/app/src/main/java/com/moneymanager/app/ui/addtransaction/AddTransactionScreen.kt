package com.moneymanager.app.ui.addtransaction

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.app.ui.dialogs.AddEditTransactionDialog
import com.moneymanager.domain.ai.TransactionDraft

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
            onDismiss = onDismiss,
            initialDraft = initialDraft,
            onDraftDismiss = onDraftDismiss,
            onConfirm = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children)
                else viewModel.addTransaction(tx)
                // Correction learning: record what the user actually saved so future
                // same-merchant lookups reflect real preferences, not just AI guesses.
                val hint = initialDraft?.merchantHint
                if (hint != null && tx.categoryId != null) {
                    viewModel.recordMerchantCategory(hint, tx.categoryId, tx.type)
                }
                onDismiss()
            },
        )
    }
}
