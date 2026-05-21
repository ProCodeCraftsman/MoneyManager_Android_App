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
import androidx.compose.material.icons.filled.Close
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
import com.moneymanager.app.ui.constants.TimeFilter
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

    val dateInputFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    var startDateText by remember {
        mutableStateOf(initialStartDate?.let { dateInputFormat.format(Date(it)) } ?: "")
    }
    var endDateText by remember {
        mutableStateOf(initialEndDate?.let { dateInputFormat.format(Date(it)) } ?: "")
    }
    var calendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            timeInMillis = startDate ?: System.currentTimeMillis()
        })
    }

    fun parseDateText(text: String): Long? = try {
        dateInputFormat.parse(text.trim())?.time
    } catch (e: Exception) { null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Select Period",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Choose a time range for your summary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Period chips ──────────────────────────────────────────────────
            Text(
                text = "Period",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
            )

            val presets = listOf("Day", "Month", "Year", "All Time", "Custom")
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presets) { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = {
                            selectedPreset = preset
                            if (preset != "Custom") {
                                handlePresetSelection(preset) { s, e ->
                                    startDate = s
                                    endDate = e
                                    calendarMonth = Calendar.getInstance().apply { timeInMillis = s }
                                    startDateText = dateInputFormat.format(Date(s))
                                    endDateText = dateInputFormat.format(Date(e))
                                }
                            }
                        },
                        label = { Text(preset, fontSize = 12.sp) },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Range preview card ────────────────────────────────────────────
            if (startDate != null && endDate != null) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = formatRange(startDate, endDate),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (selectedPreset == "Custom" && startDate != null && endDate != null) {
                                val days = ((endDate!! - startDate!!) / (1000L * 60 * 60 * 24)).toInt() + 1
                                Text(
                                    text = "$days days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Custom: date input fields + calendar ──────────────────────────
            if (selectedPreset == "Custom") {
                Text(
                    text = "Date Range",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { text ->
                            startDateText = text
                            parseDateText(text)?.let { millis ->
                                startDate = millis
                                calendarMonth = Calendar.getInstance().apply { timeInMillis = millis }
                            }
                        },
                        label = { Text("Start Date", fontSize = 11.sp) },
                        placeholder = { Text("dd MMM yyyy", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = endDateText,
                        onValueChange = { text ->
                            endDateText = text
                            parseDateText(text)?.let { millis ->
                                endDate = millis
                            }
                        },
                        label = { Text("End Date", fontSize = 11.sp) },
                        placeholder = { Text("dd MMM yyyy", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CalendarView(
                        currentMonth = calendarMonth,
                        startDate = startDate,
                        endDate = endDate,
                        onDateSelected = { date ->
                            if (startDate == null || (startDate != null && endDate != null)) {
                                startDate = date
                                endDate = null
                                startDateText = dateInputFormat.format(Date(date))
                                endDateText = ""
                            } else if (date < startDate!!) {
                                endDate = startDate
                                startDate = date
                                startDateText = dateInputFormat.format(Date(date))
                                endDateText = dateInputFormat.format(Date(endDate!!))
                            } else {
                                endDate = date
                                endDateText = dateInputFormat.format(Date(date))
                            }
                        },
                        onMonthChange = { calendarMonth = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = startDate != null && endDate != null
                ) {
                    Text("Apply", color = Color.White)
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
    val firstDayOfWeek = (currentMonth.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
    }.get(Calendar.DAY_OF_WEEK)

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
            listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
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
                        dateCal.set(Calendar.HOUR_OF_DAY, 0)
                        dateCal.set(Calendar.MINUTE, 0)
                        dateCal.set(Calendar.SECOND, 0)
                        dateCal.set(Calendar.MILLISECOND, 0)
                        val timeMillis = dateCal.timeInMillis

                        val isSelected = timeMillis == startDate || timeMillis == endDate
                        val isInRange = startDate != null && endDate != null &&
                            timeMillis > startDate && timeMillis < endDate

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
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
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
    return if (end == null) df.format(Date(start))
    else "${df.format(Date(start))} – ${dfYear.format(Date(end))}"
}

private fun handlePresetSelection(preset: String, onRangeCalculated: (Long, Long) -> Unit) {
    val start = Calendar.getInstance()
    val end = Calendar.getInstance()
    start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0)
    start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
    end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59)
    end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)

    when (preset) {
        "Day" -> { /* start/end already today */ }
        "Month" -> {
            start.set(Calendar.DAY_OF_MONTH, 1)
            end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        "Year" -> {
            start.set(Calendar.DAY_OF_YEAR, 1)
            end.set(Calendar.MONTH, Calendar.DECEMBER)
            end.set(Calendar.DAY_OF_MONTH, 31)
        }
        "All Time" -> {
            start.set(2000, Calendar.JANUARY, 1)
            end.set(2100, Calendar.DECEMBER, 31)
        }
    }
    onRangeCalculated(start.timeInMillis, end.timeInMillis)
}

private fun mapPresetToTimeFilter(preset: String?): TimeFilter? {
    return when (preset) {
        "Day" -> TimeFilter.DAY
        "Month" -> TimeFilter.MONTH
        "Year" -> TimeFilter.YEAR
        "All Time" -> TimeFilter.ALL
        else -> TimeFilter.CUSTOM
    }
}
