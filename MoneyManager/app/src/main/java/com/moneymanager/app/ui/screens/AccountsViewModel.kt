package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalAssets: Double = 0.0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
) : ViewModel() {

    val uiState: StateFlow<AccountsUiState> = combine(
        accountRepository.getAllAccounts(),
        accountRepository.getTotalAssets(),
    ) { accounts, totalAssets ->
        android.util.Log.d("AccountsViewModel", "Emitting state with ${accounts.size} accounts and totalAssets: $totalAssets")
        AccountsUiState(
            accounts = accounts,
            totalAssets = totalAssets,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AccountsUiState()
    )

    private val _events = MutableSharedFlow<AccountEvent>()
    val events = _events.asSharedFlow()

    fun addAccount(name: String, type: String, balance: Double) {
        viewModelScope.launch {
            try {
                val id = accountRepository.insertAccount(
                    AccountEntity(name = name, type = type, balance = balance)
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
}

sealed class AccountEvent {
    data class Success(val message: String) : AccountEvent()
    data class Error(val message: String) : AccountEvent()
}