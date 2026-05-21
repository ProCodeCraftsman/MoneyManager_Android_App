package com.moneymanager.app.ui.summary

import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.app.ui.constants.TimeFilter

enum class SummaryTab { EXPENSE, INCOME, LENDING, TRANSFERS, SAVINGS }

data class CategorySpend(
    val categoryId: Long?,
    val name: String,
    val amount: Double,
    val percentOfTotal: Float,   // 0..100
    val color: androidx.compose.ui.graphics.Color,
    val emoji: String = "📁",
    val iconType: String = "emoji",
    val colorIndex: Int = 0
)

data class BudgetUtilizationRow(
    val categoryId: Long,
    val categoryName: String,
    val budgetLimit: Double,
    val spent: Double,
    val utilizationPercent: Float, // 0..>100 allowed (overrun)
    val percentOfTotalExpense: Float = 0f,
    val color: androidx.compose.ui.graphics.Color,
    val emoji: String = "📁",
    val iconType: String = "emoji",
    val colorIndex: Int = 0
)

data class LendingPerson(
    val id: Long,
    val name: String,
    val amount: Double,
    val isOwed: Boolean,
    val avatar: String? = null
)

data class AccountTransferInfo(
    val accountId: Long,
    val accountName: String,
    val accountNumber: String,
    val accountType: String,
    val balance: Double,
    val transferCount: Int,
    val inAmount: Double,
    val outAmount: Double,
    val emoji: String,
    val iconType: String = "emoji",
    val color: String,
    val colorIndex: Int = 0
)

data class SavingsGoalRow(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val linkedAmount: Double = 0.0,
    val progressPercent: Float,
    val emoji: String,
    val iconType: String = "emoji",
    val color: androidx.compose.ui.graphics.Color,
    val colorIndex: Int = 0
)

data class SavingsAccountRow(
    val id: Long,
    val name: String,
    val accountNumber: String,
    val balance: Double,
    val emoji: String,
    val iconType: String = "emoji",
    val color: String,
    val colorIndex: Int = 0
)

data class SummaryUiState(
    val isLoading: Boolean = true,
    val isEmpty: Boolean = false,
    val activeTab: SummaryTab = SummaryTab.EXPENSE,

    // Header / period
    val selectedFilter: TimeFilter = TimeFilter.MONTH,
    val filterDisplayDate: String = "",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,

    // Net balance card
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val prevNetBalance: Double = 0.0,
    val netBalanceTrendPercent: Double = 0.0,
    val prevTotalIncome: Double = 0.0,
    val incomeTrendPercent: Double = 0.0,

    // Expense tab data
    val totalBudget: Double = 0.0,
    val budgetRemaining: Double = 0.0,
    val budgetUtilizationPercent: Float = 0f,
    val expenseByCategory: List<PieChartEntry> = emptyList(),
    val expenseByAccount: List<PieChartEntry> = emptyList(),
    val topBudgetUtilization: List<BudgetUtilizationRow> = emptyList(),

    // Income tab data
    val incomeByCategory: List<CategorySpend> = emptyList(),
    val incomeByAccount: List<PieChartEntry> = emptyList(),
    val incomeByCategoryPie: List<PieChartEntry> = emptyList(),

    // Lending tab data
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val lentPeopleCount: Int = 0,
    val borrowedPeopleCount: Int = 0,
    val lendingNetBalance: Double = 0.0,
    val settledAmount: Double = 0.0,
    val settledCount: Int = 0,
    val lendingPeople: List<LendingPerson> = emptyList(),

    // Transfer tab data
    val totalTransfersCount: Int = 0,
    val totalTransferAmount: Double = 0.0,
    val accountTransfers: List<AccountTransferInfo> = emptyList(),

    // Savings tab data
    val totalSavings: Double = 0.0,
    val savingsGrowthPercent: Double = 0.0,
    val savingsGrowthPeriod: String = "",
    val savingsGoals: List<SavingsGoalRow> = emptyList(),
    val savingsAccounts: List<SavingsAccountRow> = emptyList(),

    val currency: String = "INR"
)
