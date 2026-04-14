package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.domain.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val goals: List<GoalEntity> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
) : ViewModel() {

    val uiState: StateFlow<GoalsUiState> = goalRepository.getAllGoals()
        .map { goals ->
            GoalsUiState(goals = goals, isLoading = false)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = GoalsUiState()
        )

    fun addGoal(name: String, emoji: String, targetAmount: Double, deadline: Long?) {
        viewModelScope.launch {
            goalRepository.insertGoal(
                GoalEntity(name = name, emoji = emoji, targetAmount = targetAmount, deadline = deadline)
            )
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