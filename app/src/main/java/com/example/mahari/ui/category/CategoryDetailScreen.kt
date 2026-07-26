package com.example.mahari.ui.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.ui.dashboard.components.TransactionItemRow
import com.example.mahari.theme.HeroEmeraldDark
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.WarmCharcoalElevatedDark
import com.example.mahari.theme.WarmMetalDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    categoryName: String,
    periodLabel: String,
    transactions: List<TransactionEntity>,
    categoryBudgetLimit: Double?,
    onSetCategoryBudget: (Double) -> Unit,
    onSelectMerchant: (String) -> Unit,
    onBack: () -> Unit
) {
    var showBudgetInputDialog by remember { mutableStateOf(false) }
    var budgetInputText by remember { mutableStateOf(categoryBudgetLimit?.toInt()?.toString() ?: "10000") }

    val categoryTx = remember(transactions, categoryName) {
        transactions.filter { it.category.equals(categoryName, ignoreCase = true) }
            .sortedByDescending { it.timestamp }
    }

    val periodSpend = categoryTx.sumOf { it.amount }

    // Compute Top Merchants in Category
    val topMerchants = remember(categoryTx) {
        categoryTx.groupBy { it.merchantOrParty }
            .map { (mName, txList) -> mName to txList.sumOf { it.amount } }
            .sortedByDescending { it.second }
            .take(5)
    }

    if (showBudgetInputDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetInputDialog = false },
            title = { Text("Set $categoryName Budget", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = budgetInputText,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) budgetInputText = it },
                    label = { Text("Target Monthly Limit (KES)") },
                    prefix = { Text("KES ") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val limit = budgetInputText.toDoubleOrNull() ?: 10000.0
                        onSetCategoryBudget(limit)
                        showBudgetInputDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
                ) {
                    Text("Save Budget")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetInputDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$categoryName Overview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = WarmMetalDark, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, WarmMetalDark.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = periodLabel.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Ksh ${"%.2f".format(periodSpend)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = HeroEmeraldDark
                        )

                        // Budget Bar section
                        if (categoryBudgetLimit != null && categoryBudgetLimit > 0) {
                            val progress = (periodSpend / categoryBudgetLimit).coerceIn(0.0, 1.0).toFloat()
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = WarmMetalDark,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Spent: Ksh ${periodSpend.toInt()}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Limit: Ksh ${categoryBudgetLimit.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                                }
                            }
                        } else {
                            Button(
                                onClick = { showBudgetInputDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = WarmCharcoalElevatedDark, contentColor = WarmMetalDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎯 Set $categoryName Budget Target", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Top Merchants Breakdown
            if (topMerchants.isNotEmpty()) {
                item {
                    Text(
                        text = "TOP MERCHANTS IN $categoryName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            topMerchants.forEach { (mName, mSpend) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectMerchant(mName) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(mName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Ksh ${"%.0f".format(mSpend)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = WarmMetalDark)
                                        Text(" →", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Transactions List
            item {
                Text(
                    text = "TRANSACTIONS (${categoryTx.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            items(categoryTx) { tx ->
                TransactionItemRow(
                    transaction = tx,
                    onClick = { onSelectMerchant(tx.merchantOrParty) }
                )
            }
        }
    }
}
