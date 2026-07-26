package com.example.mahari.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.security.SecurityManager
import com.example.mahari.theme.*
import com.example.mahari.ui.budget.BudgetSettingsDialog
import com.example.mahari.ui.dashboard.components.HeroDailyBudgetCard
import com.example.mahari.ui.dashboard.components.MonthlyCashflowCard
import com.example.mahari.ui.dashboard.components.RecategorizeDialog
import com.example.mahari.ui.dashboard.components.RecentTransactionsCard
import com.example.mahari.ui.settings.SettingsDialog
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    database: MahariDatabase,
    securityManager: SecurityManager,
    isDarkMode: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onNavigateToMerchant: (String) -> Unit = {},
    onNavigateToCategory: (String) -> Unit = {},
    onNavigateToRecapHistory: () -> Unit = {},
    onNavigateToDataPrivacy: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTxForRecategorize by remember { mutableStateOf<TransactionEntity?>(null) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showFullListSheet by remember { mutableStateOf(false) }

    if (showShareDialog) {
        com.example.mahari.ui.share.ShareDashboardDialog(
            todaySpend = uiState.todaySpend,
            dailyBudgetLimit = uiState.dailyBudgetLimit,
            isDarkMode = isDarkMode,
            onDismiss = { showShareDialog = false }
        )
    }

    if (showDatePickerDialog) {
        DateRangePickerDialog(
            onRangeSelected = { start, end ->
                viewModel.onCustomRangeSelected(start, end)
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            database = database,
            securityManager = securityManager,
            onDismiss = { showSettingsDialog = false },
            onBudgetUpdated = { newLimit ->
                viewModel.updateMonthlyBudget(newLimit)
            },
            onNavigateToDataPrivacy = onNavigateToDataPrivacy,
            onNavigateToRecapHistory = onNavigateToRecapHistory
        )
    }

    if (showBudgetDialog) {
        BudgetSettingsDialog(
            currentMonthlyBudget = uiState.dailyBudgetLimit * 30.0,
            userName = securityManager.getUserName(),
            userAge = securityManager.getUserAge(),
            onDismiss = { showBudgetDialog = false },
            onSaveBudget = { newLimit ->
                securityManager.setMonthlyBudget(newLimit)
                viewModel.updateMonthlyBudget(newLimit)
            }
        )
    }

    // Time-of-day greeting
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greetingTime = when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    val name = securityManager.getUserName()
    val greetingText = if (name.isNotEmpty()) "$greetingTime, $name" else "M-Pesa Financial Intelligence"

    val context = androidx.compose.ui.platform.LocalContext.current
    val pullToRefreshState = rememberPullToRefreshState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshDashboard(context, database) },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    color = WarmMetalDark,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mahari",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { showShareDialog = true }) {
                                Text("📤", fontSize = 20.sp)
                            }
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Text("⚙️", fontSize = 20.sp)
                            }
                            FilterChip(
                                selected = isDarkMode,
                                onClick = { onToggleTheme(!isDarkMode) },
                                label = { Text(if (isDarkMode) "🌙" else "☀️") }
                            )
                        }
                    }
                }

                // Global Date Scope Stepper Bar
                item {
                    DateScopeSelectorBar(
                        currentScope = uiState.dateScopeMode,
                        onPreviousMonth = { viewModel.onPreviousMonth() },
                        onNextMonth = { viewModel.onNextMonth() },
                        onOpenDatePicker = { showDatePickerDialog = true },
                        onResetToCurrentMonth = { viewModel.onResetToCurrentMonth() }
                    )
                }

                // 0. Empty Budget Banner (Visible if no budget is configured)
                if (uiState.dailyBudgetLimit <= 0.0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PrimaryContainer),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Set a budget to see daily tracking",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = PrimaryNavy
                                    )
                                    Text(
                                        text = "Track your daily spending limits & get real-time alerts.",
                                        fontSize = 12.sp,
                                        color = PrimaryNavy
                                    )
                                }
                                Button(
                                    onClick = { showBudgetDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryNavy)
                                ) {
                                    Text("Set Budget", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // 1. Hero Daily Budget Card
                item {
                    HeroDailyBudgetCard(
                        todaySpend = uiState.todaySpend,
                        dailyBudgetLimit = uiState.dailyBudgetLimit,
                        onEditBudget = { showBudgetDialog = true }
                    )
                }

                // 2. Monthly Cashflow Summary Card
                item {
                    MonthlyCashflowCard(
                        monthIncome = uiState.monthIncome,
                        monthExpense = uiState.monthExpense
                    )
                }

                // 3. Category Breakdown Section
                if (uiState.topCategories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Spending Breakdown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    item {
                        CategoryBreakdownCard(
                            categories = uiState.topCategories,
                            onCategoryClick = onNavigateToCategory
                        )
                    }
                }

                // 4. Search Bar & Filter Chips (with Search All Time Toggle)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Transactions & Search",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text(if (uiState.isSearchAllTime) "Search all history..." else "Search ${uiState.dateScopeMode.label}...") },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )

                        // Filter Chips (Including Search All Time Override)
                        val categories = listOf("Food & Dining", "Groceries", "Utilities", "Transport", "Airtime & Data", "Income")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = uiState.isSearchAllTime,
                                    onClick = { viewModel.onSearchAllTimeToggled(!uiState.isSearchAllTime) },
                                    label = { Text(if (uiState.isSearchAllTime) "🌐 Search All Time (ON)" else "🌐 Search All Time") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryNavy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                            items(categories) { category ->
                                FilterChip(
                                    selected = uiState.selectedCategoryFilter == category,
                                    onClick = { viewModel.onCategoryFilterSelected(category) },
                                    label = { Text(category) },
                                    shape = RoundedCornerShape(20.dp)
                                )
                            }
                        }
                    }
                }

                // 5. Recent Transactions Preview Card (Top 5)
                item {
                    RecentTransactionsCard(
                        transactions = uiState.transactions.take(5),
                        totalCount = uiState.transactions.size,
                        onSeeAll = { showFullListSheet = true },
                        onTransactionClick = { selectedTxForRecategorize = it }
                    )
                }
            }
        }
    }

    if (showFullListSheet) {
        FullTransactionListSheet(
            transactions = uiState.transactions,
            periodLabel = uiState.dateScopeMode.label,
            onTransactionClick = { selectedTxForRecategorize = it },
            onDismiss = { showFullListSheet = false }
        )
    }

    // Recategorize Dialog
    selectedTxForRecategorize?.let { tx ->
        RecategorizeDialog(
            transaction = tx,
            onDismiss = { selectedTxForRecategorize = null },
            onConfirmNewCategory = { newCat ->
                viewModel.recategorizeMerchant(tx.merchantOrParty, newCat)
                selectedTxForRecategorize = null
            }
        )
    }
}

@Composable
fun CategoryBreakdownCard(
    categories: List<CategorySpend>,
    onCategoryClick: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            categories.take(4).forEach { cat ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(cat.category) }
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = cat.category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Ksh ${"%.2f".format(cat.totalAmount)} →",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { cat.percentageShare },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = BluePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}
