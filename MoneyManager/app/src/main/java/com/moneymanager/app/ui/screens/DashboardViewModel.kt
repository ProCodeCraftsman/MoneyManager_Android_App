package com.moneymanager.app.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.app.ui.components.PieChartEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class DashboardUiState(
    val netWorth: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val expenseBreakdown: List<PieChartEntry> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val categoryColors = mapOf(
        "food" to Color(0xFFE57373),
        "transport" to Color(0xFF64B5F6),
        "shopping" to Color(0xFFBA68C8),
        "bills" to Color(0xFFFFD54F),
        "entertainment" to Color(0xFF4DB6AC),
        "health" to Color(0xFFFF8A65),
        "other" to Color(0xFF90A4AE)
    )

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
        
        val expensesByCategory = monthTransactions
            .filter { it.type == "expense" }
            .groupBy { it.categoryId?.toString() ?: "other" }
            .map { (category, transactions) ->
                val categoryName = getCategoryName(category)
                val color = categoryColors[category.lowercase()] ?: categoryColors["other"]!!
                PieChartEntry(
                    label = categoryName,
                    value = transactions.sumOf { it.amount },
                    color = color
                )
            }
            .sortedByDescending { it.value }

        DashboardUiState(
            netWorth = totalAssets - totalDebt,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            recentTransactions = recentTx,
            accounts = accounts,
            expenseBreakdown = expensesByCategory,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    private fun getCategoryName(categoryId: String): String {
        return when (categoryId.lowercase()) {
            "1" -> "Food"
            "2" -> "Transport"
            "3" -> "Shopping"
            "4" -> "Bills"
            "5" -> "Entertainment"
            "6" -> "Health"
            else -> "Other"
        }
    }

    fun transferMoney(fromAccountId: Long, toAccountId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val fromAccount = accountRepository.getAccountById(fromAccountId)
            val toAccount = accountRepository.getAccountById(toAccountId)
            
            if (fromAccount != null && toAccount != null) {
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = fromAccountId,
                        type = "expense",
                        amount = amount,
                        note = note.ifEmpty { "Transfer to ${toAccount.name}" },
                        categoryId = 4
                    )
                )
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = toAccountId,
                        type = "income",
                        amount = amount,
                        note = note.ifEmpty { "Transfer from ${fromAccount.name}" },
                        categoryId = 4
                    )
                )
                accountRepository.updateBalance(fromAccountId, fromAccount.balance - amount)
                accountRepository.updateBalance(toAccountId, toAccount.balance + amount)
            }
        }
    }
}