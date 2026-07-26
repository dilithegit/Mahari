package com.example.mahari.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.theme.WarmMetalDark
import com.example.mahari.util.ImageCaptureUtils

@Composable
fun RecategorizeDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onConfirmNewCategory: (String) -> Unit
) {
    val categories = listOf("Food & Dining", "Groceries", "Utilities", "Transport", "Airtime & Data", "Income", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recategorize Merchant") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Select new category for ${transaction.merchantOrParty}:")
                categories.forEach { cat ->
                    TextButton(
                        onClick = { onConfirmNewCategory(cat) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(cat, fontSize = 16.sp)
                    }
                }
            }
        },
        confirmButton = {
            val ctx = LocalContext.current
            Button(
                onClick = {
                    ImageCaptureUtils.generateAndShareCardImage(
                        context = ctx,
                        title = "M-Pesa Transaction Receipt",
                        amountText = "${if (!transaction.isExpense) "+" else ""}Ksh ${"%.2f".format(transaction.amount)}",
                        subtitle = "${transaction.merchantOrParty} • ${transaction.code}",
                        isExpense = transaction.isExpense,
                        isDarkMode = false
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
            ) {
                Text("📷 Share Receipt")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
