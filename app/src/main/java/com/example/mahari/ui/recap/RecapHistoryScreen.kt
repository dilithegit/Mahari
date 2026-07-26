package com.example.mahari.ui.recap

import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.db.RecapEntity
import com.example.mahari.data.recap.StatisticalRecapEngine
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.HeroEmeraldDark
import com.example.mahari.theme.WarmCharcoalElevatedDark
import com.example.mahari.theme.WarmMetalDark
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapHistoryScreen(
    database: MahariDatabase,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var recaps by remember { mutableStateOf<List<RecapEntity>>(emptyList()) }
    var selectedRecap by remember { mutableStateOf<RecapEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isGenerating by remember { mutableStateOf(false) }

    val currentMonthYear = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    val securityManager = remember { SecurityManager(context) }

    fun loadRecaps() {
        coroutineScope.launch {
            val dao = database.recapDao()
            val budget = securityManager.getMonthlyBudget()
            StatisticalRecapEngine.backfillHistoricalRecaps(
                transactionDao = database.transactionDao(),
                recapDao = dao,
                monthlyBudget = budget
            )
            recaps = dao.getAllRecaps()
            isLoading = false
        }
    }

    fun generateCurrentRecap() {
        coroutineScope.launch {
            isGenerating = true
            try {
                val budget = securityManager.getMonthlyBudget()
                val newRecap = StatisticalRecapEngine.generateRecap(
                    monthYear = currentMonthYear,
                    transactionDao = database.transactionDao(),
                    monthlyBudget = budget
                )
                database.recapDao().insertRecap(newRecap)
                recaps = database.recapDao().getAllRecaps()
                Toast.makeText(context, "Monthly recap & score generated!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to generate recap.", Toast.LENGTH_SHORT).show()
            } finally {
                isGenerating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadRecaps()
    }

    if (selectedRecap != null) {
        val r = selectedRecap!!
        ModalBottomSheet(
            onDismissRequest = { selectedRecap = null },
            containerColor = MaterialTheme.colorScheme.surface,
            scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.6f),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Recap • ${r.monthYear}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (r.isCloudEnhanced) "Enhanced Insights" else "Local Statistical Engine",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmMetalDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Financial Score Section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("FINANCIAL HEALTH SCORE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (r.financialScore != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${"%.1f".format(r.financialScore)}%", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = HeroEmeraldDark)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = WarmMetalDark.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Data Confidence: ${"%.0f".format(r.confidenceRating ?: 0.0)}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmMetalDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        } else {
                            Text("Score not tracked for this month", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("TOTAL MONTHLY SPEND", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Ksh ${"%.2f".format(r.totalSpend)}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = HeroEmeraldDark)
                        Text("Top Category: ${r.topCategory}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = WarmMetalDark)
                    }
                }

                Text("📊 Plain-Language Financial Insights", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)

                val shapList = remember(r) {
                    r.shapExplanationsJson
                        .replace("[", "").replace("]", "").replace("\"", "")
                        .split(",")
                }

                shapList.forEach { explanation ->
                    val cleanText = explanation.replace("\\", "").trim()
                    if (cleanText.isNotBlank()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "• $cleanText",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { selectedRecap = null },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
                ) {
                    Text("Close Recap")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights & Recaps v3.0", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← Back", color = WarmMetalDark, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarmMetalDark)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Action Header
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, WarmMetalDark.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("MONTHLY FINANCIAL HEALTH ENGINE v3.0", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)
                            Text("4-Factor Health Score (Budget, Savings, Consistency, Debt Reliance) & Data Volume Confidence Rating.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(
                                onClick = { generateCurrentRecap() },
                                enabled = !isGenerating,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = WarmMetalDark)
                            ) {
                                Text(if (isGenerating) "Computing Score & Insights..." else "⚡ Generate $currentMonthYear Recap")
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "PERMANENT MONTHLY RECAP ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                if (recaps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No recaps generated yet.\nTap 'Generate $currentMonthYear Recap' above to compute insights from your real transactions.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(recaps) { r ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRecap = r },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, WarmMetalDark.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = r.monthYear,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = WarmMetalDark
                                    )
                                    Text(
                                        text = if (r.financialScore != null) "Score: ${"%.1f".format(r.financialScore)}%" else "Score: N/A",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (r.financialScore != null) HeroEmeraldDark else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = r.headlineInsight,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text("View Detailed Insights →", fontSize = 12.sp, color = WarmMetalDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
