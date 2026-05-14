package com.moneymanager.app.ui.summary.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.screens.TimeFilter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerSheet(
    initialStartDate: Long?,
    initialEndDate: Long?,
    onApply: (Long, Long, TimeFilter?) -> Unit,
    onDismiss: () -> Unit
) {
    var startDate by remember { mutableStateOf(initialStartDate) }
    var endDate by remember { mutableStateOf(initialEndDate) }
    var selectedPreset by remember { mutableStateOf<String?>("Month") }
    
    // Calendar state
    var calendarMonth by remember { 
        mutableStateOf(Calendar.getInstance().apply { 
            timeInMillis = startDate ?: System.currentTimeMillis() 
        }) 
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Select Date Range",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            // Combined Preset Chips
            val allPresets = listOf(
                "Day", "Month", "Year", "All Time",
                "This Week", "Last 7 Days", "Last 30 Days",
                "Last Month", "This Quarter", "Last Quarter", "Custom"
            )
            
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allPresets) { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { 
                            selectedPreset = preset
                            handlePresetSelection(preset) { start, end ->
                                startDate = start
                                endDate = end
                                calendarMonth = Calendar.getInstance().apply { timeInMillis = start }
                            }
                        },
                        label = { Text(preset, fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            // Selected Range Box
            Surface(
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Selected Range",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = formatRange(startDate, endDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (startDate != null && endDate != null) {
                        val days = ((endDate!! - startDate!!) / (1000 * 60 * 60 * 24)).toInt() + 1
                        Text(
                            text = "$days days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 30.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendar View
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                CalendarView(
                    currentMonth = calendarMonth,
                    startDate = startDate,
                    endDate = endDate,
                    onDateSelected = { date ->
                        selectedPreset = "Custom"
                        if (startDate == null || (startDate != null && endDate != null)) {
                            startDate = date
                            endDate = null
                        } else if (date < startDate!!) {
                            endDate = startDate
                            startDate = date
                        } else {
                            endDate = date
                        }
                    },
                    onMonthChange = { calendarMonth = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (startDate != null && endDate != null) {
                            onApply(startDate!!, endDate!!, mapPresetToTimeFilter(selectedPreset))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = startDate != null && endDate != null
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun CalendarView(
    currentMonth: Calendar,
    startDate: Long?,
    endDate: Long?,
    onDateSelected: (Long) -> Unit,
    onMonthChange: (Calendar) -> Unit
) {
    val daysInMonth = currentMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = (currentMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.get(Calendar.DAY_OF_WEEK)
    
    val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(currentMonth.time)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { 
                val prev = currentMonth.clone() as Calendar
                prev.add(Calendar.MONTH, -1)
                onMonthChange(prev)
            }) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev")
            }
            Text(text = monthTitle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            IconButton(onClick = { 
                val next = currentMonth.clone() as Calendar
                next.add(Calendar.MONTH, 1)
                onMonthChange(next)
            }) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next")
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            days.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val calendar = currentMonth.clone() as Calendar
        var dayCount = 1
        for (i in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 1..7) {
                    val currentDayIndex = i * 7 + j
                    if (currentDayIndex < firstDayOfWeek || dayCount > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val dateCal = calendar.clone() as Calendar
                        dateCal.set(Calendar.DAY_OF_MONTH, dayCount)
                        
                        // Normalize dateCal to start of day for comparison
                        dateCal.set(Calendar.HOUR_OF_DAY, 0)
                        dateCal.set(Calendar.MINUTE, 0)
                        dateCal.set(Calendar.SECOND, 0)
                        dateCal.set(Calendar.MILLISECOND, 0)
                        val timeMillis = dateCal.timeInMillis

                        val isSelected = timeMillis == startDate || timeMillis == endDate
                        val isInRange = startDate != null && endDate != null && timeMillis > startDate && timeMillis < endDate

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isInRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(timeMillis) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayCount.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        dayCount++
                    }
                }
            }
            if (dayCount > daysInMonth) break
        }
    }
}

private fun formatRange(start: Long?, end: Long?): String {
    if (start == null) return "Select start date"
    val df = SimpleDateFormat("MMM dd", Locale.getDefault())
    val dfYear = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    return if (end == null) {
        df.format(Date(start))
    } else {
        val startStr = df.format(Date(start))
        val endStr = dfYear.format(Date(end))
        "$startStr - $endStr"
    }
}

private fun handlePresetSelection(preset: String, onRangeCalculated: (Long, Long) -> Unit) {
    val start = Calendar.getInstance()
    val end = Calendar.getInstance()
    
    // Clear time
    start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
    end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)

    when (preset) {
        "Day", "Today" -> {
            // Start and end are already today
        }
        "Month", "This Month" -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        "Year", "This Year" -> {
            start.set(Calendar.DAY_OF_YEAR, 1)
            end.set(Calendar.MONTH, Calendar.DECEMBER)
            end.set(Calendar.DAY_OF_MONTH, 31)
        }
        "All Time" -> {
            start.set(2000, Calendar.JANUARY, 1)
            end.set(2100, Calendar.DECEMBER, 31)
        }
        "Last Month" -> {
            start.add(Calendar.MONTH, -1)
            start.set(Calendar.DAY_OF_MONTH, 1)
            end.timeInMillis = start.timeInMillis
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)
        }
        "This Quarter" -> {
            val currentMonth = start.get(Calendar.MONTH)
            val quarterStartMonth = (currentMonth / 3) * 3
            start.set(Calendar.MONTH, quarterStartMonth)
            start.set(Calendar.DAY_OF_MONTH, 1)
            end.set(Calendar.MONTH, quarterStartMonth + 2)
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        "Last Quarter" -> {
            val currentMonth = start.get(Calendar.MONTH)
            val lastQuarterStartMonth = ((currentMonth / 3) - 1) * 3
            // Handle negative if current is Q1
            start.add(Calendar.MONTH, -3)
            val quarterStart = (start.get(Calendar.MONTH) / 3) * 3
            start.set(Calendar.MONTH, quarterStart)
            start.set(Calendar.DAY_OF_MONTH, 1)
            end.timeInMillis = start.timeInMillis
            end.set(Calendar.MONTH, quarterStart + 2)
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)
        }
        "This Week" -> {
            start.set(Calendar.DAY_OF_WEEK, start.firstDayOfWeek)
            end.timeInMillis = start.timeInMillis
            end.add(Calendar.DAY_OF_MONTH, 6)
            end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)
        }
        "Last 7 Days" -> {
            start.add(Calendar.DAY_OF_YEAR, -6)
        }
        "Last 30 Days" -> {
            start.add(Calendar.DAY_OF_YEAR, -29)
        }
    }
    onRangeCalculated(start.timeInMillis, end.timeInMillis)
}

private fun mapPresetToTimeFilter(preset: String?): TimeFilter? {
    return when (preset) {
        "Day", "Today" -> TimeFilter.DAY
        "Month", "This Month" -> TimeFilter.MONTH
        "Year", "This Year" -> TimeFilter.YEAR
        "All Time" -> TimeFilter.ALL
        "Last Month" -> TimeFilter.LAST_MONTH
        "This Quarter" -> TimeFilter.THIS_QUARTER
        "Last Quarter" -> TimeFilter.LAST_QUARTER
        "This Week" -> TimeFilter.WEEK
        "Last 7 Days" -> TimeFilter.LAST_7_DAYS
        "Last 30 Days" -> TimeFilter.LAST_30_DAYS
        else -> TimeFilter.CUSTOM
    }
}
