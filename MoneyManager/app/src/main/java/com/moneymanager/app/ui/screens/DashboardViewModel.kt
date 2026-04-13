package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val netWorth: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val calendar = Calendar.getInstance()
    private val monthStart: Long
        get() {
            calendar.time = Date()
            calendar[Calendar.DAY_OF_MONTH] = 1
            calendar[Calendar.HOUR_OF_DAY] = 0
            calendar[Calendar.MINUTE] = 0
            calendar[Calendar.SECOND] = 0
            calendar[Calendar.MILLISECOND] = 0
            return calendar.timeInMillis
        }
    private val monthEnd: Long
        get() {
            calendar.time = Date()
            calendar[Calendar.DAY_OF_MONTH] = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar[Calendar.HOUR_OF_DAY] = 23
            calendar[Calendar.MINUTE] = 59
            calendar[Calendar.SECOND] = 59
            return calendar.timeInMillis
        }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getTotalAssets(),
        accountRepository.getTotalDebt(),
        transactionRepository.getTransactionsByDateRange(monthStart, monthEnd),
        transactionRepository.getRecentTransactions(10),
        accountRepository.getAllAccounts()
    ) { totalAssets, totalDebt, monthTransactions, recentTx, accounts ->
        val totalIncome = monthTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = monthTransactions.filter { it.type == "expense" }.sumOf { it.amount }

        DashboardUiState(
            netWorth = totalAssets - totalDebt,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            recentTransactions = recentTx,
            accounts = accounts,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )
}