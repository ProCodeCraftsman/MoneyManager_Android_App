package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymanager.data.entity.RecurringEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RemindersWidget(
    upcomingRecurring: List<RecurringEntity>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    if (upcomingRecurring.isEmpty()) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5ECD0)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "UPCOMING",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB8860B),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            upcomingRecurring.forEach { recurring ->
                ReminderItem(
                    recurring = recurring,
                    currencyFormat = currencyFormat
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ReminderItem(
    recurring: RecurringEntity,
    currencyFormat: NumberFormat
) {
    val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
    val now = Calendar.getInstance()
    val nextDate = Calendar.getInstance().apply { timeInMillis = recurring.nextDate }
    
    val formattedDate = when {
        isTomorrow(now, nextDate) -> "Tomorrow"
        else -> dateFormat.format(Date(recurring.nextDate))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recurring.note.ifEmpty { recurring.type.replaceFirstChar { it.uppercase() } },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = when (recurring.type) {
                "expense" -> "-${currencyFormat.format(recurring.amount)}"
                "income" -> "+${currencyFormat.format(recurring.amount)}"
                else -> currencyFormat.format(recurring.amount)
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when (recurring.type) {
                "expense" -> MaterialTheme.colorScheme.error
                "income" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun isTomorrow(today: Calendar, date: Calendar): Boolean {
    val tomorrow = today.clone() as Calendar
    tomorrow.add(Calendar.DAY_OF_MONTH, 1)
    return tomorrow.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            tomorrow.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}
