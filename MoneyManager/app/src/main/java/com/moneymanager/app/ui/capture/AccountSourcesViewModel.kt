package com.moneymanager.app.ui.capture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.repository.SmsAccountLinkRepository
import com.moneymanager.domain.capture.AccountSourceSummary
import com.moneymanager.domain.capture.AccountSources
import com.moneymanager.domain.capture.CaptureReview
import com.moneymanager.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountSourcesUiState(
    val sources: List<AccountSourceSummary> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val message: String? = null,
)

/**
 * One-time mapping of each SMS account source to an account (AI-055). Mapping a source teaches the link for future
 * messages and moves every pending draft from that source to the account. Balance cross-check is not built yet.
 */
@HiltViewModel
class AccountSourcesViewModel @Inject constructor(
    private val review: CaptureReview,
    private val accountRepository: AccountRepository,
    private val preferences: PreferencesManager,
    private val smsAccountLinks: SmsAccountLinkRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AccountSourcesUiState> = combine(
        review.observePending(),
        accountRepository.getActiveAccounts(),
        message,
    ) { pending, accounts, note ->
        AccountSourcesUiState(
            sources = AccountSources.summarise(pending, preferences.getSmsAccountLinks()),
            accounts = accounts,
            message = note,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccountSourcesUiState())

    fun mapTo(source: AccountSourceSummary, account: AccountEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            link(source.key, account)
            message.value = "Mapped ${source.draftCount} drafts to ${account.name}"
        }
    }

    fun createAndMap(source: AccountSourceSummary, name: String, type: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val account = AccountEntity(name = trimmed, type = type)
            val id = accountRepository.insertAccount(account)
            link(source.key, account.copy(id = id))
            message.value = "Created $trimmed and mapped ${source.draftCount} drafts"
        }
    }

    private suspend fun link(key: String, account: AccountEntity) {
        smsAccountLinks.learn(key, account.id)
        review.pendingDrafts()
            .filter { it.draft.accountLink == key }
            .forEach { pending ->
                review.updateDraft(pending.ledgerId, pending.draft.copy(accountId = account.id, accountName = account.name))
            }
    }
}
