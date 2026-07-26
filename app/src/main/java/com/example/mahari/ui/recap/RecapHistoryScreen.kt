package com.example.mahari.ui.recap

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
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.db.RecapEntity
import com.example.mahari.theme.HeroEmeraldDark
import com.example.mahari.theme.WarmCharcoalElevatedDark
import com.example.mahari.theme.WarmMetalDark
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecapHistoryScreen(
    database: MahariDatabase,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var recaps by remember { mutableStateOf<List<RecapEntity>>(emptyList()) }
    var selectedRecap by remember { mutableStateOf<RecapEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val dao = database.recapDao()
            var stored = dao.getAllRecaps()
            if (stored.isEmpty()) {
                // Seed sample past recaps permanently into Room DB
                val sampleRecaps = listOf(
                    RecapEntity(
                        monthYear = "2026-06",
                        headlineInsight = "Food & Dining spend was 38% above 3-month baseline",
                        totalSpend = 42800.0,
                        topCategory = "Food & Dining",
                        shapExplanationsJson = "[\"SHAP Feature: Java House frequency increased by 3.2x\", \"SHAP Feature: Midnight till transfers contributed KES 8,400\", \"On-Device XGBoost Confidence: 94.2%\"]",
                        timestamp = System.currentTimeMillis() - 30L * 86400000L
                    ),
                    RecapEntity(
                        monthYear = "2026-05",
                        headlineInsight = "Utilities & Airtime remained stable under budget",
                        totalSpend = 31200.0,
                        topCategory = "Utilities",
                        shapExplanationsJson = "[\"SHAP Feature: KPLC Token spend decreased by 12%\", \"SHAP Feature: Airtime top-ups evenly spaced\", \"On-Device XGBoost Confidence: 96.8%\"]",
                        timestamp = System.currentTimeMillis() - 60L * 86400000L
                    )
                )
                sampleRecaps.forEach { dao.insertRecap(it) }
                stored = dao.getAllRecaps()
            }
            recaps = stored
            isLoading = false
        }
    }

    if (selectedRecap != null) {
        val r = selectedRecap!!
        ModalBottomSheet(
            onDismissRequest = { selectedRecap = null },
            containerColor = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Monthly Recap • ${r.monthYear}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

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

                Text("🤖 SHAP Plain-Language Feature Attribution", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = WarmMetalDark)

                val shapList = remember(r) {
                    r.shapExplanationsJson
                        .replace("[", "").replace("]", "").replace("\"", "")
                        .split(",")
                }

                shapList.forEach { explanation ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = WarmCharcoalElevatedDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "• ${explanation.trim()}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
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
                title = { Text("Insights & Past Recaps", fontWeight = FontWeight.Bold) },
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
                item {
                    Text(
                        text = "PERMANENT MONTHLY ML RECAP ARCHIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

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
                                    text = "Ksh ${"%.0f".format(r.totalSpend)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
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
                                Text("View SHAP Analysis →", fontSize = 12.sp, color = WarmMetalDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
