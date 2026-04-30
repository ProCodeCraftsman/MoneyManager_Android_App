package com.moneymanager.app.ui.insights.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class StatusFigure(
    val label: String,
    val value: Double
)

@Composable
fun StatusPaneFigureGrid(
    figures: List<StatusFigure>,
    currency: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        figures.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { figure ->
                    StatusPaneFigure(
                        label = figure.label,
                        value = figure.value,
                        currency = currency,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if row has only 1 item
                if (rowItems.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
