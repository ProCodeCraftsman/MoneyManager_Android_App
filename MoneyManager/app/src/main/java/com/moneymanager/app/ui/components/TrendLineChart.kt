package com.moneymanager.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TrendLineChart(
    data: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(modifier = modifier.fillMaxWidth().height(200.dp)) {
            Text(
                text = "No data available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.error
    val netColor = MaterialTheme.colorScheme.secondary

    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(color = incomeColor, label = "Income")
            LegendItem(color = expenseColor, label = "Expense")
            LegendItem(color = netColor, label = "Net")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            val maxValue = data.maxOfOrNull { maxOf(it.income, it.expense, kotlin.math.abs(it.net)) } ?: 1.0
            val minValue = data.minOfOrNull { minOf(it.income, it.expense, kotlin.math.abs(it.net), 0.0) } ?: 0.0
            val range = (maxValue - minValue).coerceAtLeast(1.0)

            val stepX = chartWidth / (data.size - 1).coerceAtLeast(1)

            fun getY(value: Double): Float {
                return (padding + chartHeight - ((value - minValue) / range * chartHeight)).toFloat()
            }

            data.forEachIndexed { index, point ->
                val x = padding + index * stepX
                val incomeY = getY(point.income)
                val expenseY = getY(point.expense)

                if (index < data.size - 1) {
                    val next = data[index + 1]
                    val nextX = padding + (index + 1) * stepX
                    val nextIncomeY = getY(next.income)
                    val nextExpenseY = getY(next.expense)

                    drawLine(
                        color = incomeColor,
                        start = Offset(x, incomeY),
                        end = Offset(nextX, nextIncomeY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = expenseColor,
                        start = Offset(x, expenseY),
                        end = Offset(nextX, nextExpenseY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                drawCircle(
                    color = incomeColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, incomeY)
                )

                drawCircle(
                    color = expenseColor,
                    radius = 4.dp.toPx(),
                    center = Offset(x, expenseY)
                )
            }

            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(padding, padding),
                end = Offset(padding, size.height - padding),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(padding, size.height - padding),
                end = Offset(size.width - padding, size.height - padding),
                strokeWidth = 1.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { point ->
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(12.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
