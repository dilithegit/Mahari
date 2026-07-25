package com.example.mahari.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.db.BudgetEntity
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.export.CsvExporter
import com.example.mahari.data.parser.SmsBackfillManager
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.PrimaryNavy
import kotlinx.coroutines.launch

@Composable
fun SettingsDialog(
    database: MahariDatabase,
    securityManager: SecurityManager,
    onDismiss: () -> Unit,
    onBudgetUpdated: (Double) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var nameInput by remember { mutableStateOf(securityManager.getUserName()) }
    var ageInput by remember { mutableStateOf(securityManager.getUserAge().toString()) }
    var monthlyBudgetInput by remember { mutableStateOf(securityManager.getMonthlyBudget().toInt().toString()) }
    var isBiometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }

    var isBackfilling by remember { mutableStateOf(false) }
    var backfillMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Mahari Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = "v1.0", fontSize = 12.sp, color = PrimaryNavy, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Profile Settings
                Text("LOCAL PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Name / Nickname") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageInput,
                    onValueChange = { if (it.all { c -> c.isDigit() }) ageInput = it },
                    label = { Text("Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                // Section 2: Budget Settings
                Text("BUDGET LIMITS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                OutlinedTextField(
                    value = monthlyBudgetInput,
                    onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) monthlyBudgetInput = it },
                    label = { Text("Monthly Budget (KES)") },
                    prefix = { Text("KES ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val mVal = monthlyBudgetInput.toDoubleOrNull() ?: 0.0
                Text(
                    text = "Safe Daily Limit: KES ${String.format("%.2f", mVal / 30.0)} / day",
                    fontSize = 12.sp,
                    color = PrimaryNavy,
                    fontWeight = FontWeight.SemiBold
                )

                Divider()

                // Section 3: SMS Re-Backfill
                Text("DATA & SMS RE-SYNC", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isBackfilling = true
                            val res = SmsBackfillManager.performOneTimeBackfill(
                                context = context,
                                transactionDao = database.transactionDao(),
                                mappingDao = database.merchantMappingDao()
                            )
                            isBackfilling = false
                            backfillMessage = "Re-scan complete! Imported ${res.importedCount} transactions."
                            Toast.makeText(context, backfillMessage, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isBackfilling,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = PrimaryNavy)
                ) {
                    Text(if (isBackfilling) "Rescanning Inbox..." else "🔄 Re-run SMS Backfill")
                }

                Divider()

                // Section 4: Security
                Text("SECURITY & PRIVACY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryNavy)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Biometric App Lock", fontSize = 13.sp)
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = {
                            isBiometricEnabled = it
                            securityManager.setBiometricEnabled(it)
                        }
                    )
                }

                Divider()

                // Section 5: About App
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Mahari — M-Pesa Finance Tracker", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryNavy)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created by Strathmore University students Okwudili Ujubuonu and Mark Gitau Irungu. 100% offline, zero network calls.",
                            fontSize = 11.sp,
                            color = PrimaryNavy
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ageInt = ageInput.toIntOrNull() ?: 20
                    securityManager.saveUserProfile(nameInput, ageInt)
                    val newLimit = monthlyBudgetInput.toDoubleOrNull() ?: 30000.0
                    securityManager.setMonthlyBudget(newLimit)

                    coroutineScope.launch {
                        database.budgetDao().setBudget(
                            BudgetEntity(
                                id = 1,
                                monthlyLimit = newLimit,
                                monthYear = "2026-07",
                                dailyLimit = newLimit / 30.0
                            )
                        )
                        onBudgetUpdated(newLimit)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
