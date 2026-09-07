package com.moneymanager.app.ui.accounts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.components.AccountBarData
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountsViewModel @Inject constructor(
    application: Application,
    private val accountRepository: AccountRepository,
    private val transactionRepository: com.moneymanager.domain.repository.TransactionRepository,
    private val preferencesManager: PreferencesManager,
) : AndroidViewModel(application) {

    private val _selectedChartTypeFilter = MutableStateFlow("All")

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAllAccounts(),
        accountRepository.getTotalAssets(),
        transactionRepository.getAllTransactions(),
        preferencesManager.currency,
        _selectedChartTypeFilter,
    ) { accounts, totalAssets, transactions, currencyCode, chartFilter ->
        val active = accounts.filter { !it.isArchived }
        val archived = accounts.filter { it.isArchived }

        val chartAccounts = active.filter { account ->
            if (chartFilter.equals("All", ignoreCase = true)) true
            else account.type.equals(chartFilter, ignoreCase = true)
        }

        val comparisonData = chartAccounts.map { account ->
            val accountTxns = transactions.filter { it.accountId == account.id && !it.isSplitParent }
            val inflow = accountTxns.filter { it.type == "income" || it.type == "borrow" }.sumOf { it.amount }
            val outflow = accountTxns.filter { it.type == "expense" || it.type == "lend" }.sumOf { it.amount }
            AccountBarData(
                accountName = account.name,
                inflow = inflow,
                outflow = outflow,
                accountType = account.type
            )
        }

        AccountsUiState(
            accounts = accounts,
            activeAccounts = active,
            archivedAccounts = archived,
            totalAssets = totalAssets,
            currencyCode = currencyCode,
            isLoading = false,
            accountComparisonData = comparisonData,
            selectedChartTypeFilter = chartFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    private val _events = MutableSharedFlow<AccountEvent>()
    val events = _events.asSharedFlow()

    fun setChartTypeFilter(filter: String) {
        _selectedChartTypeFilter.value = filter
    }

    fun addAccount(name: String, type: String, emoji: String, iconType: String, balance: Double) {
        viewModelScope.launch {
            try {
                val id = accountRepository.insertAccount(
                    AccountEntity(name = name, type = type, emoji = emoji, iconType = iconType, initialBalance = balance, balance = balance)
                )
                if (id > 0) {
                    _events.emit(AccountEvent.Success("Account added successfully"))
                } else {
                    _events.emit(AccountEvent.Error("Failed to add account"))
                }
            } catch (e: Exception) {
                _events.emit(AccountEvent.Error(e.message ?: "Unknown error"))
            }
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            try {
                accountRepository.updateAccount(account)
                _events.emit(AccountEvent.Success("Account updated successfully"))
            } catch (e: Exception) {
                _events.emit(AccountEvent.Error(e.message ?: "Failed to update account"))
            }
        }
    }

    fun archiveAccount(id: Long) {
        viewModelScope.launch {
            try {
                accountRepository.archiveAccount(id)
                _events.emit(AccountEvent.Success("Account archived successfully"))
            } catch (e: Exception) {
                _events.emit(AccountEvent.Error(e.message ?: "Failed to archive account"))
            }
        }
    }

    fun reactivateAccount(id: Long) {
        viewModelScope.launch {
            try {
                accountRepository.reactivateAccount(id)
                _events.emit(AccountEvent.Success("Account reactivated successfully"))
            } catch (e: Exception) {
                _events.emit(AccountEvent.Error(e.message ?: "Failed to reactivate account"))
            }
        }
    }
}

sealed class AccountEvent {
    data class Success(val message: String) : AccountEvent()
    data class Error(val message: String) : AccountEvent()
}
