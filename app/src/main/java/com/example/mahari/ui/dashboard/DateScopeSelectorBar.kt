package com.example.mahari.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.model.DateScopeMode
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.PrimaryNavy

@Composable
fun DateScopeSelectorBar(
    currentScope: DateScopeMode,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDatePicker: () -> Unit,
    onResetToCurrentMonth: () -> Unit
) {
    val isCustomMode = currentScope is DateScopeMode.CustomRangeMode
    val isMonthMode = currentScope is DateScopeMode.MonthMode
    val canStepNext = isMonthMode && (currentScope as DateScopeMode.MonthMode).canStepForward()

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stepper Left
                IconButton(
                    onClick = onPreviousMonth,
                    enabled = isMonthMode,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("◀", fontSize = 14.sp, color = if (isMonthMode) PrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                }

                // Center Label & Custom Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenDatePicker() }
                ) {
                    if (isCustomMode) {
                        Text("📅 ", fontSize = 14.sp)
                    }
                    Text(
                        text = currentScope.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Stepper Right
                IconButton(
                    onClick = onNextMonth,
                    enabled = canStepNext,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "▶",
                        fontSize = 14.sp,
                        color = if (canStepNext) PrimaryNavy else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }

                // Calendar Picker Trigger
                IconButton(
                    onClick = onOpenDatePicker,
                    modifier = Modifier.size(36.dp)
                ) {
                    Text("📆", fontSize = 18.sp)
                }
            }

            // Custom Range Active Banner & Snap Back Action
            if (isCustomMode) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM RANGE ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryNavy
                    )
                    Button(
                        onClick = onResetToCurrentMonth,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                    ) {
                        Text("Back to Current Month", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
