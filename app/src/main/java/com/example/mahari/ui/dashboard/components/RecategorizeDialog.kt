package com.example.mahari.ui.dashboard.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.MahariApplication
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.theme.WarmMetalDark

@Composable
fun RecategorizeDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onConfirmNewCategory: (String) -> Unit
) {
    val context = LocalContext.current
    val database = (context.applicationContext as MahariApplication).container.database

    var customCategories by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val custom = database.categoryDao().getAllCustomCategories().map { it.name }
            customCategories = custom
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val builtInCategories = listOf(
        "Food & Dining",
        "Groceries",
        "Transport",
        "Utilities",
        "Shopping & Services",
        "Education",
        "Health & Pharmacy",
        "Financial & Transfers",
        "Airtime & Data",
        "Income",
        "Debt & Overdraft",
        "Other"
    )

    val allCategories = (builtInCategories + customCategories).distinct()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recategorize Merchant", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Select new category for ${transaction.merchantOrParty}:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                allCategories.forEach { cat ->
                    val isCurrent = cat.equals(transaction.category, ignoreCase = true)
                    TextButton(
                        onClick = {
                            onConfirmNewCategory(cat)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isCurrent) WarmMetalDark else MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = if (isCurrent) "✓ $cat" else cat,
                            fontSize = 15.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
