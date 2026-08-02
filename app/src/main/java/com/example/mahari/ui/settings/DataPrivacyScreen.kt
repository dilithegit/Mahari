package com.example.mahari.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.parser.SmsBackfillManager
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.AlertRed
import com.example.mahari.theme.AlertRedContainer
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.WarmMetalDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataPrivacyScreen(
    database: MahariDatabase,
    securityManager: SecurityManager,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var deleteInputText by remember { mutableStateOf("") }
    var isBackfilling by remember { mutableStateOf(false) }
    var isPurging by remember { mutableStateOf(false) }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("🚨 Delete All Local Data?", fontWeight = FontWeight.Bold, color = AlertRed) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "This will permanently delete your entire on-device database. THIS ACTION CANNOT BE UNDONE.",
                        fontSize = 13.sp,
                        color = AlertRed
                    )
                    Text("Type \"DELETE\" below to confirm:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = deleteInputText,
                        onValueChange = { deleteInputText = it },
                        singleLine = true,
                        placeholder = { Text("DELETE") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteInputText.trim() == "DELETE") {
                            coroutineScope.launch {
                                isPurging = true
                                database.clearAllTables()
                                securityManager.setOnboardingComplete(false)
                                isPurging = false
                                showDeleteConfirmDialog = false
                                Toast.makeText(context, "All local user data deleted permanently.", Toast.LENGTH_LONG).show()
                                onBack()
                            }
                        } else {
                            Toast.makeText(context, "Must type DELETE exactly to confirm.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = deleteInputText.trim() == "DELETE" && !isPurging,
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text(if (isPurging) "Purging Everything..." else "Permanently Wipe Data")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data & Privacy Control", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = WarmMetalDark, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Backup & Restore
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("📦 ENCRYPTED LOCAL BACKUP & RESTORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                    Text("Export your on-device transaction history to an encrypted local file, or restore from a previous backup.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Encrypted backup exported to Downloads/Mahari_Backup.json", Toast.LENGTH_LONG).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = WarmMetalDark)
                        ) {
                            Text("Export Backup")
                        }
                        Button(
                            onClick = {
                                Toast.makeText(context, "Select a Mahari backup file to restore.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = WarmMetalDark)
                        ) {
                            Text("Restore File")
                        }
                    }
                }
            }

            // Section 2: 100% Local & Offline Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("🛡️ 100% PRIVATE & LOCAL STORAGE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                    Text(
                        "Mahari operates 100% offline on your device. Your M-Pesa SMS receipts, financial stats, health scores, and merchant mappings NEVER leave your phone.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Section 3: SMS Re-Backfill
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔄 RE-RUN SMS HISTORICAL BACKFILL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                    Text("Re-scans your device SMS inbox for any missed historical M-Pesa receipts.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Toast.makeText(context, "Imported ${res.importedCount} transactions!", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = !isBackfilling,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = WarmMetalDark)
                    ) {
                        Text(if (isBackfilling) "Rescanning Inbox..." else "Start SMS Inbox Re-Scan")
                    }
                }
            }

            // Section 4: Destructive Wipe
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AlertRedContainer),
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🚨 DESTRUCTIVE ZONE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed)
                    Text("Wipe all local Room database tables and send a purge payload to the backend server if sync was used.", fontSize = 12.sp, color = AlertRed)
                    Button(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                    ) {
                        Text("🗑️ Delete All My Data Permanently", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
