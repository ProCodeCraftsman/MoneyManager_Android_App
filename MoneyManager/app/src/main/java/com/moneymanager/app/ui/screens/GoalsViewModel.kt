package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.GoalRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalWithProgress(
    val goal: GoalEntity,
    val linkedAmount: Double,
    val totalAmount: Double
)

data class GoalsUiState(
    val goals: List<GoalWithProgress> = emptyList(),
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val transactionRepository: TransactionRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<GoalsUiState> = combine(
        goalRepository.getAllGoals(),
        preferencesManager.currency
    ) { goals, currencyCode ->
        goals to currencyCode
    }.flatMapLatest { (goals, currencyCode) ->
        if (goals.isEmpty()) {
            flowOf(GoalsUiState(currencyCode = currencyCode, isLoading = false))
        } else {
            val goalFlows = goals.map { goal ->
                transactionRepository.getTransactionsByGoal(goal.id)
                    .map { transactions ->
                        // Filter out split parents to avoid double counting
                        val linkedAmount = transactions.filter { !it.isSplitParent }.sumOf { it.amount }
                        GoalWithProgress(
                            goal = goal,
                            linkedAmount = linkedAmount,
                            totalAmount = goal.currentAmount + linkedAmount
                        )
                    }
            }
            combine(goalFlows) { it.toList() }.map { GoalsUiState(goals = it, currencyCode = currencyCode, isLoading = false) }
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalsUiState()
        )

    fun addGoal(name: String, emoji: String, iconType: String, targetAmount: Double, deadline: Long?) {
        viewModelScope.launch {
            goalRepository.insertGoal(
                GoalEntity(name = name, emoji = emoji, iconType = iconType, targetAmount = targetAmount, deadline = deadline)
            )
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }

    fun addContribution(goalId: Long, amount: Double) {
        viewModelScope.launch {
            val goal = goalRepository.getGoalById(goalId) ?: return@launch
            val updatedGoal = goal.copy(currentAmount = goal.currentAmount + amount)
            goalRepository.updateGoal(updatedGoal)
        }
    }
}