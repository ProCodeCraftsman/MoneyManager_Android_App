package com.moneymanager.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymanager.app.ui.screens.TimeFilter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeFilterBar(
    selectedFilter: TimeFilter,
    customStartDate: Long?,
    customEndDate: Long?,
    onFilterSelected: (TimeFilter) -> Unit,
    onCustomDateRangeSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeFilter.entries.forEach { filter ->
            val isSelected = selectedFilter == filter
            val label = when (filter) {
                TimeFilter.DAY -> "Day"
                TimeFilter.WEEK -> "Week"
                TimeFilter.MONTH -> "Month"
                TimeFilter.YEAR -> "Year"
                TimeFilter.ALL -> "All"
                TimeFilter.CUSTOM -> {
                    if (customStartDate != null && customEndDate != null) {
                        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                        "${dateFormat.format(Date(customStartDate))} - ${dateFormat.format(Date(customEndDate))}"
                    } else {
                        "Custom"
                    }
                }
            }

            FilterChip(
                selected = isSelected,
                onClick = {
                    if (filter == TimeFilter.CUSTOM) {
                        showDatePicker = true
                    } else {
                        onFilterSelected(filter)
                    }
                },
                label = { Text(label) }
            )
        }
    }

    if (showDatePicker) {
        DateRangePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { start, end ->
                onCustomDateRangeSelected(start, end)
                showDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onDateRangeSelected: (Long, Long) -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val startMillis = dateRangePickerState.selectedStartDateMillis
                    val endMillis = dateRangePickerState.selectedEndDateMillis
                    if (startMillis != null && endMillis != null) {
                        // Add time to start and end of day
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = startMillis
                        calendar[Calendar.HOUR_OF_DAY] = 0
                        calendar[Calendar.MINUTE] = 0
                        calendar[Calendar.SECOND] = 0
                        calendar[Calendar.MILLISECOND] = 0
                        val start = calendar.timeInMillis

                        calendar.timeInMillis = endMillis
                        calendar[Calendar.HOUR_OF_DAY] = 23
                        calendar[Calendar.MINUTE] = 59
                        calendar[Calendar.SECOND] = 59
                        calendar[Calendar.MILLISECOND] = 999
                        val end = calendar.timeInMillis

                        onDateRangeSelected(start, end)
                    }
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null &&
                        dateRangePickerState.selectedEndDateMillis != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            DateRangePicker(state = dateRangePickerState)
        }
    )
}
