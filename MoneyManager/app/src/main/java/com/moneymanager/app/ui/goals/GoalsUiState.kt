package com.moneymanager.app.ui.goals

import com.moneymanager.data.entity.GoalEntity

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
