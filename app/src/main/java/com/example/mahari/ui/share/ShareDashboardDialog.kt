package com.example.mahari.ui.share

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.WarmMetalDark
import com.example.mahari.util.ImageCaptureUtils

@Composable
fun ShareDashboardDialog(
    todaySpend: Double,
    dailyBudgetLimit: Double,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var maskBalances by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Share Financial Progress", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Generate a luxury Mahari card image to share via WhatsApp, Messages, or save to your gallery.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Mask sensitive balances", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Hides exact numbers as Ksh ••••••", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = maskBalances,
                            onCheckedChange = { maskBalances = it }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountStr = if (maskBalances) "Ksh ••••••" else "Ksh ${"%.2f".format(todaySpend)}"
                    val sub = if (maskBalances) "Daily Limit: Ksh ••••••" else "Daily Limit: Ksh ${dailyBudgetLimit.toInt()}"
                    ImageCaptureUtils.generateAndShareCardImage(
                        context = context,
                        title = "Today's Financial Spend",
                        amountText = amountStr,
                        subtitle = sub,
                        isExpense = todaySpend > dailyBudgetLimit,
                        isDarkMode = isDarkMode
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
            ) {
                Text("📷 Generate & Share Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
