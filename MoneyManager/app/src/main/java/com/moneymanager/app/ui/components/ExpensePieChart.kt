package com.moneymanager.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.*

data class PieChartEntry(
    val label: String,
    val value: Double,
    val color: Color
)

@Composable
fun ExpensePieChart(
    entries: List<PieChartEntry>,
    modifier: Modifier = Modifier,
    currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US),
    onCategoryClick: ((PieChartEntry) -> Unit)? = null
) {
    if (entries.isEmpty()) {
        EmptyPieChart(modifier)
        return
    }

    val total = entries.sumOf { it.value }
    if (total <= 0) {
        EmptyPieChart(modifier)
        return
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 40.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    var startAngle = -90f
                    entries.forEach { entry ->
                        val sweepAngle = ((entry.value / total) * 360).toFloat()
                        drawArc(
                            color = entry.color,
                            startAngle = startAngle,
                            sweepAngle = sweepAngle,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth)
                        )
                        startAngle += sweepAngle
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            entries.take(5).forEach { entry ->
                val percentage = ((entry.value / total) * 100).toInt()
                PieChartLegendItem(
                    entry = entry,
                    percentage = percentage,
                    currencyFormat = currencyFormat,
                    onClick = if (onCategoryClick != null) {
                        { onCategoryClick(entry) }
                    } else null
                )
            }
            if (entries.size > 5) {
                val otherTotal = entries.drop(5).sumOf { it.value }
                val otherPercentage = ((otherTotal / total) * 100).toInt()
                PieChartLegendItem(
                    entry = PieChartEntry("Others", otherTotal, Color.Gray),
                    percentage = otherPercentage,
                    currencyFormat = currencyFormat,
                    onClick = null
                )
            }
        }
    }
}

@Composable
private fun PieChartLegendItem(
    entry: PieChartEntry,
    percentage: Int,
    currencyFormat: NumberFormat,
    onClick: (() -> Unit)?
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickModifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(12.dp)) {
                drawCircle(color = entry.color)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row {
            Text(
                text = currencyFormat.format(entry.value),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($percentage%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyPieChart(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 40.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = Color.LightGray,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )
            }
            Text(
                text = "No data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Add transactions to see expense breakdown",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
