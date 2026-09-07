package com.moneymanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class AccountBarData(
    val accountName: String,
    val inflow: Double,
    val outflow: Double,
    val accountType: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountComparisonChart(
    data: List<AccountBarData>,
    currencyCode: String,
    modifier: Modifier = Modifier,
    selectedTypeFilter: String = "All",
    onFilterSelected: (String) -> Unit = {}
) {
    val availableFilters = listOf("All", "bank", "cash", "credit", "savings", "peer")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Inflow vs Outflow by Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            availableFilters.forEach { filter ->
                val label = when (filter) {
                    "All" -> "All Types"
                    "bank" -> "Bank"
                    "cash" -> "Cash"
                    "credit" -> "Credit"
                    "savings" -> "Savings"
                    "peer" -> "Peer"
                    else -> filter
                }
                FilterChip(
                    selected = selectedTypeFilter.equals(filter, ignoreCase = true),
                    onClick = { onFilterSelected(filter) },
                    label = { Text(label, style = MaterialTheme.typography.labelMedium) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No account comparison data for selected filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val maxVal = data.flatMap { listOf(it.inflow, it.outflow) }.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { item ->
                    Column(
                        modifier = Modifier
                            .width(64.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Inflow Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight((item.inflow / maxVal).toFloat().coerceIn(0.01f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(MaterialTheme.colorScheme.secondary)
                            )
                            // Outflow Bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight((item.outflow / maxVal).toFloat().coerceIn(0.01f, 1f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFFFF7043))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = item.accountName,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = MaterialTheme.colorScheme.secondary, label = "Inflow")
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem(color = Color(0xFFFF7043), label = "Outflow")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
