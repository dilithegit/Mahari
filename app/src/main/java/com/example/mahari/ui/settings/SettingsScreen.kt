package com.example.mahari.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.example.mahari.data.db.CategoryEntity
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.PrimaryContainer
import com.example.mahari.theme.PrimaryNavy
import com.example.mahari.theme.WarmCharcoalElevatedDark
import com.example.mahari.theme.WarmMetalDark
import com.example.mahari.util.BatteryOptimizationManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    securityManager: SecurityManager,
    database: MahariDatabase,
    onNavigateToDataPrivacy: () -> Unit,
    onNavigateToRecapHistory: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var userName by remember { mutableStateOf(securityManager.getUserName()) }
    var userAge by remember { mutableStateOf(securityManager.getUserAge().toString()) }
    var monthlyBudgetInput by remember { mutableStateOf(securityManager.getMonthlyBudget().toInt().toString()) }
    var isBiometricEnabled by remember { mutableStateOf(securityManager.isBiometricEnabled()) }

    var customCategories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var newCatEmoji by remember { mutableStateOf("🛍️") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            customCategories = database.categoryDao().getAllCustomCategories()
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("➕ Add Custom Category", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Subscriptions") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newCatEmoji,
                        onValueChange = { newCatEmoji = it },
                        label = { Text("Emoji Icon") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            coroutineScope.launch {
                                val cat = CategoryEntity(
                                    name = newCatName.trim(),
                                    iconEmoji = newCatEmoji.ifBlank { "🏷️" },
                                    colorHex = "#B08D57"
                                )
                                database.categoryDao().insertCategory(cat)
                                customCategories = database.categoryDao().getAllCustomCategories()
                                showAddCategoryDialog = false
                                newCatName = ""
                                Toast.makeText(context, "Custom category created!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
                ) {
                    Text("Create Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings", fontWeight = FontWeight.Bold) },
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
            // Section 1: Local Profile
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("👤 LOCAL USER PROFILE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)

                    OutlinedTextField(
                        value = userName,
                        onValueChange = {
                            userName = it
                            securityManager.saveUserProfile(it, userAge.toIntOrNull() ?: 20)
                        },
                        label = { Text("Name / Nickname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = userAge,
                        onValueChange = {
                            userAge = it
                            securityManager.saveUserProfile(userName, it.toIntOrNull() ?: 20)
                        },
                        label = { Text("Age") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Section 2: Monthly Budget Limits
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🎯 MONTHLY BUDGET CONFIGURATION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)

                    OutlinedTextField(
                        value = monthlyBudgetInput,
                        onValueChange = {
                            monthlyBudgetInput = it
                            val limit = it.toDoubleOrNull() ?: 30000.0
                            securityManager.setMonthlyBudget(limit)
                        },
                        label = { Text("Monthly Budget (KES)") },
                        prefix = { Text("KES ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val safeDaily = (monthlyBudgetInput.toDoubleOrNull() ?: 30000.0) / 30.0
                    Text(
                        text = "Auto-computed safe daily limit: Ksh ${"%.2f".format(safeDaily)} / day",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Section 3: Categories & Custom Categories
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏷️ CATEGORIES & CUSTOM TAGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                        TextButton(onClick = { showAddCategoryDialog = true }) {
                            Text("➕ Add Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                        }
                    }

                    val defaultCats = listOf("Food & Dining", "Groceries", "Transport", "Utilities", "Education", "Health & Pharmacy")
                    val allCatNames = defaultCats + customCategories.map { "${it.iconEmoji} ${it.name}" }

                    allCatNames.chunked(2).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { catName ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = catName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Data & Privacy Sub-Screen Navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🔒 DATA, PRIVACY & BACKUP CONTROL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                    Text("Manage encrypted backups, cloud sync settings, SMS re-scans, and data deletion.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Button(
                        onClick = onNavigateToDataPrivacy,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                    ) {
                        Text("⚙️ Open Data & Privacy Dashboard →", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onNavigateToRecapHistory,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = WarmMetalDark)
                    ) {
                        Text("📊 View Past Monthly Recaps & Insights →", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            val isIgnored = BatteryOptimizationManager.isIgnoringBatteryOptimizations(context)
                            if (!isIgnored) {
                                BatteryOptimizationManager.requestBatteryOptimizationExemption(context)
                                Toast.makeText(context, "Opening battery optimization settings...", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Background battery exemption active!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryContainer, contentColor = WarmMetalDark)
                    ) {
                        Text("⚡ Check Background Reliability")
                    }
                }
            }

            // Section 5: Security & Privacy
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("🛡️ APP SECURITY & LOCK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Enable Biometric App Lock", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = {
                                isBiometricEnabled = it
                                securityManager.setBiometricEnabled(it)
                            }
                        )
                    }
                }
            }

            // Section 6: About App
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🐚 MAHARI FINANCE", fontWeight = FontWeight.Bold, color = PrimaryNavy, fontSize = 16.sp)
                    Text("Version 2.4.0 • Strathmore University", fontSize = 12.sp, color = PrimaryNavy)
                    Text("Developed by Okwudili Ujubuonu & Mark Gitau Irungu", fontSize = 11.sp, color = PrimaryNavy)
                }
            }
        }
    }
}
