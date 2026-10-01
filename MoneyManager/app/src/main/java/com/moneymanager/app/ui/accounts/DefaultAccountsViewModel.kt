package com.moneymanager.app.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.dao.AccountUsage
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.account.DefaultAccountResolver
import com.moneymanager.domain.account.DefaultAccountSlot
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DefaultAccountRow(
    val slot: DefaultAccountSlot,
    /** Account explicitly chosen by the user, or null when the slot is on Auto (or the saved one is no longer valid). */
    val configured: AccountEntity?,
    /** What the add-transaction forms will actually preselect right now. */
    val effective: AccountEntity?,
    val choices: List<AccountEntity>
)

data class DefaultAccountsUiState(val rows: List<DefaultAccountRow> = emptyList())

@HiltViewModel
class DefaultAccountsViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val usage = MutableStateFlow<List<AccountUsage>>(emptyList())

    init {
        viewModelScope.launch { usage.value = transactionRepository.getAccountUsage() }
    }

    val uiState: StateFlow<DefaultAccountsUiState> = combine(
        accountRepository.getActiveAccounts(),
        preferencesManager.defaultAccounts,
        usage
    ) { accounts, configured, usageRows ->
        DefaultAccountsUiState(
            rows = DefaultAccountSlot.entries.map { slot ->
                val choices = DefaultAccountResolver.eligibleAccounts(slot, accounts)
                val effectiveId = DefaultAccountResolver.resolve(slot, accounts, configured, usageRows)
                val configuredAccount = configured[slot.key]?.let { id -> choices.firstOrNull { it.id == id } }
                DefaultAccountRow(slot, configuredAccount, accounts.firstOrNull { it.id == effectiveId }, choices)
            }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefaultAccountsUiState())

    /** Null resets the slot to Auto. */
    fun setDefault(slot: DefaultAccountSlot, accountId: Long?) {
        viewModelScope.launch { preferencesManager.setDefaultAccount(slot.key, accountId) }
    }
}
