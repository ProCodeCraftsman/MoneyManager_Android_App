package com.moneymanager.app.ui.summary.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.summary.TrendDataPoint
import com.moneymanager.app.ui.summary.TrendStats
import com.moneymanager.app.ui.summary.TrendTimeFilter
import com.moneymanager.app.ui.summary.TrendType
import com.moneymanager.app.ui.util.CurrencyUtils
import kotlin.math.abs

@Composable
fun TrendsView(
    selectedType: TrendType,
    timeFilter: TrendTimeFilter,
    dataPoints: List<TrendDataPoint>,
    stats: TrendStats,
    currency: String,
    onTypeChange: (TrendType) -> Unit,
    onTimeFilterChange: (TrendTimeFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrendTypeToggles(
            selectedType = selectedType,
            onTypeChange = onTypeChange
        )

        TrendCard(
            type = selectedType,
            timeFilter = timeFilter,
            dataPoints = dataPoints,
            stats = stats,
            currency = currency,
            onTimeFilterChange = onTimeFilterChange
        )

        DetailedStatsCard(
            stats = stats,
            currency = currency
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TrendTypeToggles(
    selectedType: TrendType,
    onTypeChange: (TrendType) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TrendType.entries.forEach { type ->
            val isSelected = selectedType == type
            val color = when (type) {
                TrendType.INCOME -> MaterialTheme.colorScheme.primary
                TrendType.EXPENSE -> MaterialTheme.colorScheme.error
                TrendType.LENDING -> MaterialTheme.colorScheme.secondary
                TrendType.SAVINGS -> MaterialTheme.colorScheme.tertiary
            }
            
            FilterChip(
                selected = isSelected,
                onClick = { onTypeChange(type) },
                label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                leadingIcon = {
                    Icon(
                        imageVector = when (type) {
                            TrendType.INCOME -> Icons.Default.TrendingUp
                            TrendType.EXPENSE -> Icons.Default.TrendingDown
                            TrendType.LENDING -> Icons.Default.People
                            TrendType.SAVINGS -> Icons.Default.Savings
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = color.copy(alpha = 0.2f),
                    selectedLabelColor = color,
                    selectedLeadingIconColor = color
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    selectedBorderColor = color
                )
            )
        }
    }
}

@Composable
fun TrendCard(
    type: TrendType,
    timeFilter: TrendTimeFilter,
    dataPoints: List<TrendDataPoint>,
    stats: TrendStats,
    currency: String,
    onTimeFilterChange: (TrendTimeFilter) -> Unit
) {
    val typeColor = when (type) {
        TrendType.INCOME -> MaterialTheme.colorScheme.primary
        TrendType.EXPENSE -> MaterialTheme.colorScheme.error
        TrendType.LENDING -> MaterialTheme.colorScheme.secondary
        TrendType.SAVINGS -> MaterialTheme.colorScheme.tertiary
    }
    
    val currencyFormat = CurrencyUtils.getCurrencyFormat(currency)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(typeColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (type) {
                                TrendType.INCOME -> Icons.Default.TrendingUp
                                TrendType.EXPENSE -> Icons.Default.TrendingDown
                                TrendType.LENDING -> Icons.Default.People
                                TrendType.SAVINGS -> Icons.Default.Savings
                            },
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${type.name.lowercase().replaceFirstChar { it.uppercase() }} Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                TrendTimeFilterDropdown(
                    selectedFilter = timeFilter,
                    onFilterChange = onTimeFilterChange
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TrendStatItem("Current (${stats.currentLabel})", currencyFormat.format(stats.current), typeColor, Modifier.weight(1f))
                TrendStatItem("Highest Month", currencyFormat.format(stats.highest), typeColor, Modifier.weight(1f), stats.highestMonth)
                TrendStatItem("Average / Month", currencyFormat.format(stats.average), typeColor, Modifier.weight(1f))
                TrendStatItem(
                    "Growth (vs last year)", 
                    "${if (stats.growthPercent >= 0) "+" else ""}${String.format("%.0f", stats.growthPercent)}%", 
                    if (stats.growthPercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336), 
                    Modifier.weight(1f),
                    isGrowth = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Graph
            TrendGraph(
                dataPoints = dataPoints,
                color = typeColor,
                currency = currency
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Swipe indicator (Fixed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                Text(
                    text = " Swipe horizontally to view more ",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
                Box(modifier = Modifier.width(40.dp).height(1.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun TrendStatItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    subLabel: String? = null,
    isGrowth: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            if (isGrowth) {
                Icon(
                    imageVector = if (value.contains("-")) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(14.dp).padding(start = 2.dp)
                )
            }
        }
        if (subLabel != null) {
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun TrendTimeFilterDropdown(
    selectedFilter: TrendTimeFilter,
    onFilterChange: (TrendTimeFilter) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedCard(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedFilter.label,
                    style = MaterialTheme.typography.labelMedium
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TrendTimeFilter.entries.forEach { filter ->
                DropdownMenuItem(
                    text = { Text(filter.label) },
                    onClick = {
                        onFilterChange(filter)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun TrendGraph(
    dataPoints: List<TrendDataPoint>,
    color: Color,
    currency: String
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Text("No data available")
        }
        return
    }

    val maxAmount = dataPoints.maxOf { abs(it.amount) }.coerceAtLeast(1.0)
    val scrollState = rememberScrollState(Int.MAX_VALUE)
    val density = LocalDensity.current
    
    val currencyFormat = CurrencyUtils.getCurrencyFormat(currency)
    var selectedPoint by remember { mutableStateOf<TrendDataPoint?>(null) }
    var touchX by remember { mutableStateOf(0f) }

    Row(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        // Fixed Y-Axis Labels
        Column(
            modifier = Modifier
                .width(40.dp)
                .fillMaxHeight()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            val steps = 4
            for (i in steps downTo 0) {
                val valToDisplay = (maxAmount / steps) * i
                Text(
                    text = formatShortValue(valToDisplay),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(24.dp)) // Offset for X-axis labels
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(scrollState)
            ) {
                val pointWidth = 60.dp
                val canvasWidth = pointWidth * dataPoints.size.coerceAtLeast(1)
                
                Column(modifier = Modifier.width(canvasWidth).fillMaxHeight()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 24.dp)
                            .pointerInput(dataPoints) {
                                detectTapGestures { offset ->
                                    val stepX = size.width.toFloat() / dataPoints.size
                                    val index = (offset.x / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                                    selectedPoint = dataPoints[index]
                                    touchX = index * stepX + stepX / 2
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val stepX = width / (dataPoints.size.coerceAtLeast(1))
                        
                        // Draw grid lines (horizontal)
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = height - (height / gridLines * i)
                            drawLine(
                                color = if (i == 0) Color.Gray.copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.1f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = if (i == 0) 2.dp.toPx() else 1.dp.toPx()
                            )
                        }

                        if (dataPoints.size > 1) {
                            val path = Path()
                            val fillPath = Path()
                            
                            dataPoints.forEachIndexed { index, point ->
                                val x = index * stepX + stepX / 2
                                val y = height - (point.amount.toFloat() / maxAmount.toFloat() * height)
                                
                                if (index == 0) {
                                    path.moveTo(x, y)
                                    fillPath.moveTo(x, height)
                                    fillPath.lineTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                    fillPath.lineTo(x, y)
                                }
                                
                                if (index == dataPoints.size - 1) {
                                    fillPath.lineTo(x, height)
                                    fillPath.close()
                                }
                            }

                            // Draw area fill
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.0f))
                                )
                            )

                            // Draw line
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )

                            // Draw points
                            dataPoints.forEachIndexed { index, point ->
                                val x = index * stepX + stepX / 2
                                val y = height - (point.amount.toFloat() / maxAmount.toFloat() * height)
                                
                                drawCircle(
                                    color = Color.White,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = color,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }
                    
                    // Labels (X-axis) - Inside scrollable area to stay aligned with peaks
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    ) {
                        dataPoints.forEach { point ->
                            Text(
                                text = point.date,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(pointWidth),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Tooltip
                selectedPoint?.let { point ->
                    val x = touchX
                    val y = 20.dp // Fixed height for tooltip
                    
                    Surface(
                        modifier = Modifier
                            .offset(x = with(density) { (x.toDp() - 50.dp).coerceAtLeast(0.dp) }, y = y),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 4.dp,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(point.date, style = MaterialTheme.typography.labelSmall)
                            Text(currencyFormat.format(point.amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = color)
                        }
                    }
                    
                    // Dismiss tooltip on scroll or after time
                    LaunchedEffect(scrollState.value) {
                        selectedPoint = null
                    }
                }
            }
        }
    }
}

private fun formatShortValue(value: Double): String {
    return when {
        value >= 1000000 -> "${String.format("%.1f", value / 1000000)}M"
        value >= 1000 -> "${(value / 1000).toInt()}K"
        else -> value.toInt().toString()
    }
}

@Composable
fun DetailedStatsCard(
    stats: TrendStats,
    currency: String
) {
    val currencyFormat = CurrencyUtils.getCurrencyFormat(currency)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Grid of stats
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailedStatItem("Highest Month", currencyFormat.format(stats.highest), stats.highestMonth, Modifier.weight(1f), Color(0xFF4CAF50))
                    DetailedStatItem("Lowest Month", currencyFormat.format(stats.lowest), stats.lowestMonth, Modifier.weight(1f), Color(0xFFF44336))
                    DetailedStatItem("Average / Month", currencyFormat.format(stats.average), null, Modifier.weight(1f))
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    DetailedStatItem("Total", currencyFormat.format(stats.total), null, Modifier.weight(1f))
                    DetailedStatItem("Median / Month", currencyFormat.format(stats.median), null, Modifier.weight(1f))
                    DetailedStatItem(
                        "Growth (vs last year)", 
                        "${if (stats.growthPercent >= 0) "+" else ""}${String.format("%.1f", stats.growthPercent)}%", 
                        null, 
                        Modifier.weight(1f),
                        if (stats.growthPercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        showIcon = true
                    )
                }
            }
        }
    }
}

@Composable
fun DetailedStatItem(
    label: String,
    value: String,
    subValue: String?,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    showIcon: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = valueColor
            )
            if (showIcon) {
                Icon(
                    imageVector = if (value.contains("-")) Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = valueColor,
                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                )
            }
        }
        if (subValue != null) {
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
