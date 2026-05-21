package com.moneymanager.app.ui.summary.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.app.ui.util.CurrencyUtils
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeBreakdownCard(
    categoryEntries: List<PieChartEntry>,
    accountEntries: List<PieChartEntry>,
    totalIncome: Double,
    currency: String,
    modifier: Modifier = Modifier
) {
    val currencyFormat = CurrencyUtils.getCurrencyFormat(currency)
    var selectedType by remember { mutableIntStateOf(0) } // 0 for Category, 1 for Account

    val entries = if (selectedType == 0) categoryEntries else accountEntries

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Income Breakdown",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Small Toggle for Category/Account
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = selectedType == 0,
                        onClick = { selectedType = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        label = { Text("Cat", fontSize = 10.sp) }
                    )
                    SegmentedButton(
                        selected = selectedType == 1,
                        onClick = { selectedType = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        label = { Text("Acc", fontSize = 10.sp) }
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Pie Chart
                    Box(
                        modifier = Modifier.size(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val total = entries.sumOf { it.value }.toFloat()
                        
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            entries.forEach { entry ->
                                val sweepAngle = (entry.value / total * 360f).toFloat()
                                drawArc(
                                    color = entry.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 20.dp.toPx())
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = currencyFormat.format(totalIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Legend
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        entries.take(5).forEach { entry ->
                            LegendItem(entry = entry, currencyFormat = currencyFormat)
                        }
                        if (entries.size > 5) {
                            val otherValue = entries.drop(5).sumOf { it.value }
                            val otherPercent = (otherValue / totalIncome * 100).toFloat()
                            LegendItem(
                                entry = PieChartEntry(
                                    value = otherValue,
                                    color = Color.LightGray,
                                    label = "Others",
                                    percentage = otherPercent.toDouble()
                                ),
                                currencyFormat = currencyFormat
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    entry: PieChartEntry,
    currencyFormat: java.text.NumberFormat
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = entry.color,
            modifier = Modifier.size(8.dp)
        ) {}
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
        
        Text(
            text = "${currencyFormat.format(entry.value)} (${String.format(Locale.getDefault(), "%.0f%%", entry.percentage)})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
