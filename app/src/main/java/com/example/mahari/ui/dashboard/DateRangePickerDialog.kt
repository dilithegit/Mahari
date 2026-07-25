package com.example.mahari.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.PrimaryNavy
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangePickerDialog(
    onRangeSelected: (startTimestamp: Long, endTimestamp: Long) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select Custom Date Range", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    title = null,
                    headline = null,
                    showModeToggle = false
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val start = dateRangePickerState.selectedStartDateMillis ?: System.currentTimeMillis()
                    val end = dateRangePickerState.selectedEndDateMillis ?: (start + 86400000L)
                    onRangeSelected(start, end)
                    onDismiss()
                },
                enabled = dateRangePickerState.selectedStartDateMillis != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
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
