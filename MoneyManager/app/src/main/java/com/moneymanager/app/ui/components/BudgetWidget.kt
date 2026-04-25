package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymanager.app.ui.screens.BudgetWithProgress
import java.text.NumberFormat
import java.util.*

@Composable
fun BudgetWidget(
    budgetsWithProgress: List<BudgetWithProgress>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier,
    periodName: String = ""
) {
    if (budgetsWithProgress.isEmpty()) {
        return
    }

    val title = if (periodName.isNotEmpty()) "Budgets - $periodName" else "Budgets"

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            budgetsWithProgress.forEach { budgetWithProgress ->
                BudgetProgressItem(
                    budgetWithProgress = budgetWithProgress,
                    currencyFormat = currencyFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun BudgetProgressItem(
    budgetWithProgress: BudgetWithProgress,
    currencyFormat: NumberFormat
) {
    val progressColor = budgetWithProgress.categoryColor

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = budgetWithProgress.categoryName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${currencyFormat.format(budgetWithProgress.spent)} / ${currencyFormat.format(budgetWithProgress.budget.amount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { (budgetWithProgress.percentage / 100f).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.1f),
        )
    }
}
