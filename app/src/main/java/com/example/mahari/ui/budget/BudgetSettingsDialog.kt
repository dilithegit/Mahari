package com.example.mahari.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.PrimaryNavy

@Composable
fun BudgetSettingsDialog(
    currentMonthlyBudget: Double,
    userName: String,
    userAge: Int,
    onDismiss: () -> Unit,
    onSaveBudget: (monthlyLimit: Double) -> Unit
) {
    var monthlyInput by remember { mutableStateOf(currentMonthlyBudget.toInt().toString()) }
    val monthlyValue = monthlyInput.toDoubleOrNull() ?: 0.0
    val dailyLimit = monthlyValue / 30.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Budget & Profile Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (userName.isNotEmpty()) {
                    Text(
                        text = "Profile: $userName (${userAge}y/o)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Adjust your monthly target spend. Daily limits will automatically recalculate.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = monthlyInput,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) monthlyInput = it },
                    label = { Text("Monthly Budget (KES)") },
                    prefix = { Text("KES ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AUTO-COMPUTED SAFE DAILY LIMIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "KES ${String.format("%.2f", dailyLimit)} / day",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryNavy
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (monthlyValue > 0) {
                        onSaveBudget(monthlyValue)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Save Budget")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
