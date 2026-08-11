package com.moneymanager.app.ui.summary.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.summary.TrendDataPoint
import com.moneymanager.app.ui.summary.TrendStats
import com.moneymanager.app.ui.summary.TrendTimeFilter
import com.moneymanager.app.ui.summary.TrendType
import com.moneymanager.app.ui.util.CurrencyUtils
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@Composable
fun TrendsView(
    selectedType: TrendType,
    timeFilter: TrendTimeFilter,
    dataPoints: List<TrendDataPoint>,
    stats: TrendStats,
    currency: String,
    onTypeChange: (TrendType) -> Unit,
    onTimeFilterChange: (TrendTimeFilter) -> Unit,
    allDataPoints: Map<TrendType, List<TrendDataPoint>> = emptyMap()
) {
    var showMetricSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TrendCard(
            type = selectedType,
            timeFilter = timeFilter,
            dataPoints = dataPoints,
            currency = currency,
            onTimeFilterChange = onTimeFilterChange,
            onMetricClick = { showMetricSelector = true },
            allDataPoints = allDataPoints
        )

        StatisticsSection(
            stats = stats,
            currency = currency
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showMetricSelector) {
        MetricSelectorBottomSheet(
            selectedType = selectedType,
            onTypeChange = {
                onTypeChange(it)
                showMetricSelector = false
            },
            onDismiss = { showMetricSelector = false }
        )
    }
}

@Composable
fun TrendCard(
    type: TrendType,
    timeFilter: TrendTimeFilter,
    dataPoints: List<TrendDataPoint>,
    currency: String,
    onTimeFilterChange: (TrendTimeFilter) -> Unit,
    onMetricClick: () -> Unit,
    allDataPoints: Map<TrendType, List<TrendDataPoint>> = emptyMap()
) {
    val typeColor = when (type) {
        TrendType.INCOME -> Color(0xFF42A5F5)
        TrendType.EXPENSE -> Color(0xFFEF5350)
        TrendType.LENDING -> Color(0xFFAB47BC)
        TrendType.SAVINGS -> Color(0xFF66BB6A)
        TrendType.OVERALL -> MaterialTheme.colorScheme.primary
    }
    
    var visibleTypes by remember(type) { 
        mutableStateOf(if (type == TrendType.OVERALL) TrendType.entries.filter { it != TrendType.OVERALL }.toSet() else setOf(type)) 
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onMetricClick,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TrendTimeFilterDropdown(
                        selectedFilter = timeFilter,
                        onFilterChange = onTimeFilterChange
                    )
                    
                    IconButton(onClick = { /* More options */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Graph
            TrendGraph(
                dataPoints = dataPoints,
                color = typeColor,
                currency = currency,
                type = type,
                allDataPoints = allDataPoints,
                visibleTypes = visibleTypes
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Swipe indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(5) { i ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 2.dp)
                            .size(if (i == 3) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (i == 3) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            )
                    )
                }
            }
            
            Text(
                text = "Swipe horizontally to view more",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textAlign = TextAlign.Center
            )

            if (type == TrendType.OVERALL) {
                Spacer(modifier = Modifier.height(16.dp))
                TrendLegend(
                    visibleTypes = visibleTypes,
                    onToggle = { t ->
                        visibleTypes = if (visibleTypes.contains(t)) visibleTypes - t else visibleTypes + t
                    }
                )
            }
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
    currency: String,
    type: TrendType = TrendType.EXPENSE,
    allDataPoints: Map<TrendType, List<TrendDataPoint>> = emptyMap(),
    visibleTypes: Set<TrendType> = emptySet()
) {
    if (dataPoints.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            Text("No data available")
        }
        return
    }

    val graphHeight = 180.dp
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    
    val currencyFormat = CurrencyUtils.getCurrencyFormat(currency)
    
    val currentMonthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val pointWidth = 70.dp
    val pointWidthPx = with(density) { pointWidth.toPx() }
    
    val textMeasurer = rememberTextMeasurer()
    val textStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 8.sp, 
        fontWeight = FontWeight.Bold
    )

    val currentIndex = dataPoints.indexOfFirst { it.timestamp == currentMonthStart }.coerceAtLeast(0)
    
    LaunchedEffect(Unit) {
        val scrollPos = (currentIndex * pointWidthPx) - (with(density) { 150.dp.toPx() })
        scrollState.scrollTo(scrollPos.toInt())
    }

    val maxAmount = if (type == TrendType.OVERALL) {
        allDataPoints.values.flatten().maxOfOrNull { abs(it.amount) }?.coerceAtLeast(1.0) ?: 1.0
    } else {
        dataPoints.maxOf { abs(it.amount) }.coerceAtLeast(1.0)
    }

    val scrollOffset = scrollState.value.toFloat()
    // Adjusted highlight logic to be more responsive at the start of the scroll
    val highlightedIndex = if (scrollOffset < pointWidthPx) {
        0
    } else {
        (scrollOffset / pointWidthPx + 1.5f).toInt()
    }.coerceIn(0, dataPoints.size - 1)
    val highlightedPoint = dataPoints[highlightedIndex]

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = highlightedPoint.date + " 2026",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(highlightedPoint.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (type == TrendType.OVERALL) MaterialTheme.colorScheme.onSurface else color
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(graphHeight)) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                val steps = 4
                for (i in steps downTo 0) {
                    Text(
                        text = formatShortValue((maxAmount / steps) * i),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 32.dp)
                    .horizontalScroll(scrollState)
            ) {
                val canvasWidth = pointWidth * dataPoints.size
                
                Canvas(
                    modifier = Modifier
                        .width(canvasWidth)
                        .fillMaxHeight()
                        .padding(vertical = 20.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val stepX = width / dataPoints.size
                    
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = height - (height / gridLines * i)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.1f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (type == TrendType.OVERALL) {
                        TrendType.entries.filter { it != TrendType.OVERALL }.forEach { t ->
                            if (visibleTypes.contains(t)) {
                                val tColor = when (t) {
                                    TrendType.INCOME -> Color(0xFF42A5F5)
                                    TrendType.EXPENSE -> Color(0xFFEF5350)
                                    TrendType.LENDING -> Color(0xFFAB47BC)
                                    TrendType.SAVINGS -> Color(0xFF66BB6A)
                                    else -> Color.Gray
                                }
                                val points = allDataPoints[t] ?: emptyList()
                                if (points.size > 1) {
                                    drawTrendLine(
                                        points = points, 
                                        color = tColor, 
                                        maxAmount = maxAmount, 
                                        stepX = stepX, 
                                        height = height,
                                        textMeasurer = textMeasurer,
                                        textStyle = textStyle.copy(color = tColor),
                                        formatValue = { formatShortValue(it) },
                                        drawValues = true
                                    )
                                }
                            }
                        }
                    } else {
                        drawTrendLine(
                            points = dataPoints, 
                            color = color, 
                            maxAmount = maxAmount, 
                            stepX = stepX, 
                            height = height,
                            textMeasurer = textMeasurer,
                            textStyle = textStyle.copy(color = color),
                            formatValue = { formatShortValue(it) },
                            drawValues = true
                        )
                    }
                }
                
                Row(
                    modifier = Modifier
                        .width(canvasWidth)
                        .align(Alignment.BottomStart)
                ) {
                    dataPoints.forEach { point ->
                        Text(
                            text = point.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.width(pointWidth),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTrendLine(
    points: List<TrendDataPoint>,
    color: Color,
    maxAmount: Double,
    stepX: Float,
    height: Float,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    formatValue: (Double) -> String,
    drawValues: Boolean = false
) {
    val path = Path()
    val fillPath = Path()
    
    points.forEachIndexed { index, point ->
        val x = index * stepX + stepX / 2
        val y = height - (abs(point.amount).toFloat() / maxAmount.toFloat() * height)
        
        if (index == 0) {
            path.moveTo(x, y)
            fillPath.moveTo(x, height)
            fillPath.lineTo(x, y)
        } else {
            val prevX = (index - 1) * stepX + stepX / 2
            val prevY = height - (abs(points[index - 1].amount).toFloat() / maxAmount.toFloat() * height)
            path.cubicTo(
                (prevX + x) / 2, prevY,
                (prevX + x) / 2, y,
                x, y
            )
            fillPath.cubicTo(
                (prevX + x) / 2, prevY,
                (prevX + x) / 2, y,
                x, y
            )
        }
        
        if (index == points.size - 1) {
            fillPath.lineTo(x, height)
            fillPath.close()
        }
    }

    drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.2f), color.copy(alpha = 0.0f))
        )
    )

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    points.forEachIndexed { index, point ->
        val x = index * stepX + stepX / 2
        val y = height - (abs(point.amount).toFloat() / maxAmount.toFloat() * height)
        
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(x, y)
        )
        drawCircle(
            color = color,
            radius = 3.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 1.5.dp.toPx())
        )

        if (drawValues && abs(point.amount) > 0) {
            val textLayoutResult = textMeasurer.measure(
                text = formatValue(abs(point.amount)),
                style = textStyle
            )
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(x - textLayoutResult.size.width / 2, y - textLayoutResult.size.height - 4.dp.toPx())
            )
        }
    }
}

private fun formatShortValue(value: Double): String {
    return when {
        value >= 1000000 -> String.format(Locale.getDefault(), "%.1fM", value / 1000000)
        value >= 1000 -> String.format(Locale.getDefault(), "%dK", (value / 1000).toInt())
        else -> value.toInt().toString()
    }
}

@Composable
fun StatisticsSection(
    stats: TrendStats,
    currency: String
) {
    val currencyFormat = CurrencyUtils.getCurrencyFormat(currency)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Statistics (2026)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatGridItem("Total", currencyFormat.format(stats.total), Modifier.weight(1f))
                StatGridItem("Highest Month", currencyFormat.format(stats.highest), Modifier.weight(1f), stats.highestMonth)
                StatGridItem("Lowest Month", currencyFormat.format(stats.lowest), Modifier.weight(1f), stats.lowestMonth)
                StatGridItem("Average / Month", currencyFormat.format(stats.average), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatGridItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (subValue != null) {
            Text(
                text = subValue,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TrendLegend(
    visibleTypes: Set<TrendType>,
    onToggle: (TrendType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val types = listOf(
            TrendType.INCOME to Color(0xFF42A5F5),
            TrendType.EXPENSE to Color(0xFFEF5350),
            TrendType.LENDING to Color(0xFFAB47BC),
            TrendType.SAVINGS to Color(0xFF66BB6A)
        )

        types.forEach { (type, color) ->
            val isVisible = visibleTypes.contains(type)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onToggle(type) }
            ) {
                Checkbox(
                    checked = isVisible,
                    onCheckedChange = { onToggle(type) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = color,
                        uncheckedColor = color.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricSelectorBottomSheet(
    selectedType: TrendType,
    onTypeChange: (TrendType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Select Metric",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            TrendType.entries.forEach { type ->
                val isSelected = selectedType == type
                Surface(
                    onClick = { onTypeChange(type) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (type) {
                                TrendType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
                                TrendType.EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
                                TrendType.LENDING -> Icons.Default.People
                                TrendType.SAVINGS -> Icons.Default.Savings
                                TrendType.OVERALL -> Icons.Default.BarChart
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
