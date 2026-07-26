package com.example.mahari.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.mahari.theme.WarmMetalDark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onRangeSelected: (startTimestamp: Long, endTimestamp: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val displayCalendar = remember { Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) } }
    var currentYear by remember { mutableStateOf(displayCalendar.get(Calendar.YEAR)) }
    var currentMonth by remember { mutableStateOf(displayCalendar.get(Calendar.MONTH)) }

    var selectedStartMillis by remember { mutableStateOf<Long?>(null) }
    var selectedEndMillis by remember { mutableStateOf<Long?>(null) }

    val monthHeaderStr = remember(currentYear, currentMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    val calForMonth = remember(currentYear, currentMonth) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    val daysInMonth = calForMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeekJava = calForMonth.get(Calendar.DAY_OF_WEEK)
    val leadingEmptyCells = if (firstDayOfWeekJava == Calendar.SUNDAY) 6 else firstDayOfWeekJava - Calendar.MONDAY

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Custom Date Range", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                TextButton(onClick = onDismiss) {
                    Text("Close", color = WarmMetalDark)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Month Stepper Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (currentMonth == 0) {
                            currentMonth = 11
                            currentYear--
                        } else {
                            currentMonth--
                        }
                    }) {
                        Text("‹", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                    }

                    Text(
                        text = monthHeaderStr,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = {
                        if (currentMonth == 11) {
                            currentMonth = 0
                            currentYear++
                        } else {
                            currentMonth++
                        }
                    }) {
                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                    }
                }

                // Days of Week Header (Mon to Sun)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val daysOfWeek = listOf("M", "T", "W", "T", "F", "S", "S")
                    daysOfWeek.forEach { dayLabel ->
                        Text(
                            text = dayLabel,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmMetalDark
                        )
                    }
                }

                // 7-Column Date Grid
                val totalGridCells = leadingEmptyCells + daysInMonth
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(totalGridCells) { index ->
                        if (index < leadingEmptyCells) {
                            Spacer(modifier = Modifier.size(36.dp))
                        } else {
                            val dayNum = index - leadingEmptyCells + 1
                            val cellCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, currentYear)
                                set(Calendar.MONTH, currentMonth)
                                set(Calendar.DAY_OF_MONTH, dayNum)
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            val cellStartMs = cellCal.timeInMillis
                            val cellEndMs = cellStartMs + 86399999L

                            val isStart = selectedStartMillis != null &&
                                    cellStartMs >= selectedStartMillis!! && cellStartMs < selectedStartMillis!! + 86400000L

                            val isEnd = selectedEndMillis != null &&
                                    cellStartMs >= selectedEndMillis!! && cellStartMs < selectedEndMillis!! + 86400000L

                            val isInRange = selectedStartMillis != null && selectedEndMillis != null &&
                                    cellStartMs > selectedStartMillis!! && cellStartMs < selectedEndMillis!!

                            val isSelected = isStart || isEnd

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(if (isSelected) CircleShape else RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> WarmMetalDark
                                            isInRange -> WarmMetalDark.copy(alpha = 0.25f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        if (selectedStartMillis == null || selectedEndMillis != null) {
                                            selectedStartMillis = cellStartMs
                                            selectedEndMillis = null
                                        } else if (cellStartMs > selectedStartMillis!!) {
                                            selectedEndMillis = cellStartMs
                                        } else {
                                            selectedStartMillis = cellStartMs
                                            selectedEndMillis = null
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNum.toString(),
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Range Selection Label Indicator
                if (selectedStartMillis != null) {
                    val sFmt = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedStartMillis!!))
                    val eFmt = if (selectedEndMillis != null) {
                        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(selectedEndMillis!!))
                    } else "Select End Date"

                    Text(
                        text = "Range: $sFmt → $eFmt",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmMetalDark,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = selectedStartMillis ?: System.currentTimeMillis()
                    val end = (selectedEndMillis ?: start) + 86399999L
                    onRangeSelected(start, end)
                    onDismiss()
                },
                enabled = selectedStartMillis != null,
                colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
            ) {
                Text("Apply Custom Range")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
