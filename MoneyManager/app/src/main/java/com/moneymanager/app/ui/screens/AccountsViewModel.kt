package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalAssets: Double = 0.0,
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
    val accountComparisonData: List<com.moneymanager.app.ui.components.AccountBarData> = emptyList()
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    application: Application,
    private val accountRepository: AccountRepository,
    private val transactionRepository: com.moneymanager.domain.repository.TransactionRepository,
    private val preferencesManager: PreferencesManager,
) : AndroidViewModel(application) {

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAllAccounts(),
        accountRepository.getTotalAssets(),
        transactionRepository.getAllTransactions(),
        preferencesManager.currency,
    ) { accounts, totalAssets, transactions, currencyCode ->
        val comparisonData = accounts.map { account ->
            val accountTxns = transactions.filter { it.accountId == account.id && !it.isSplitParent }
            val inflow = accountTxns.filter { it.type == "income" || it.type == "receive" || it.type == "borrow" }.sumOf { it.amount }
            val outflow = accountTxns.filter { it.type == "expense" || it.type == "lend" || it.type == "repay" }.sumOf { it.amount }
            com.moneymanager.app.ui.components.AccountBarData(
                accountName = account.name,
                inflow = inflow,
                outflow = outflow
            )
        }

        AccountsUiState(
            accounts = accounts,
            totalAssets = totalAssets,
            currencyCode = currencyCode,
            isLoading = false,
            accountComparisonData = comparisonData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    private val _events = MutableSharedFlow<AccountEvent>()
    val events = _events.asSharedFlow()

    fun addAccount(name: String, type: String, emoji: String, iconType: String, balance: Double) {
        viewModelScope.launch {
            try {
                val id = accountRepository.insertAccount(
                    AccountEntity(name = name, type = type, emoji = emoji, iconType = iconType, balance = balance)
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
}

sealed class AccountEvent {
    data class Success(val message: String) : AccountEvent()
    data class Error(val message: String) : AccountEvent()
}