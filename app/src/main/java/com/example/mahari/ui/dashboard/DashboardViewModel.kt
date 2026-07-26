package com.example.mahari.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahari.data.db.BudgetEntity
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.model.DateScopeMode
import com.example.mahari.data.parser.SmsBackfillManager
import com.example.mahari.data.repository.TransactionRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class CategorySpend(
    val category: String,
    val totalAmount: Double,
    val percentageShare: Float
)

data class DashboardUiState(
    val dateScopeMode: DateScopeMode = DateScopeMode.currentMonth(),
    val todaySpend: Double = 0.0,
    val dailyBudgetLimit: Double = 1000.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val topCategories: List<CategorySpend> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val searchQuery: String = "",
    val isSearchAllTime: Boolean = false,
    val selectedCategoryFilter: String? = null,
    val isRefreshing: Boolean = false,
    val currentBalance: Double? = null,
    val latestTransactionTimestamp: Long? = null
)

class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _dateScopeMode = MutableStateFlow<DateScopeMode>(DateScopeMode.currentMonth())
    val dateScopeMode: StateFlow<DateScopeMode> = _dateScopeMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchAllTime = MutableStateFlow(false)
    val isSearchAllTime: StateFlow<Boolean> = _isSearchAllTime.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var lastFullScanTime: Long = 0L

    @Suppress("UNCHECKED_CAST")
    val uiState: StateFlow<DashboardUiState> = combine(
        _dateScopeMode,
        repository.allTransactions,
        repository.currentBudget,
        _searchQuery,
        _isSearchAllTime,
        _selectedCategoryFilter,
        _isRefreshing
    ) { flows: Array<Any?> ->
        val scopeMode = flows[0] as DateScopeMode
        val allTx = flows[1] as List<TransactionEntity>
        val budget = flows[2] as BudgetEntity?
        val query = flows[3] as String
        val searchAllTime = flows[4] as Boolean
        val categoryFilter = flows[5] as String?
        val refreshing = flows[6] as Boolean

        val startTs = scopeMode.startTimestamp
        val endTs = scopeMode.endTimestamp

        val scopedTx = allTx.filter { it.timestamp in startTs..endTs }

        val nowCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = nowCal.timeInMillis
        val todaySpent = allTx.filter { it.timestamp >= startOfToday && it.isExpense }.sumOf { it.amount }

        val periodIncome = scopedTx.filter { !it.isExpense }.sumOf { it.amount }
        val periodExpense = scopedTx.filter { it.isExpense }.sumOf { it.amount }

        val expenseTx = scopedTx.filter { it.isExpense }
        val totalExp = expenseTx.sumOf { it.amount }
        val categorySpends = if (totalExp > 0) {
            expenseTx.groupBy { it.category }
                .map { (cat, txList) ->
                    val catAmount = txList.sumOf { it.amount }
                    CategorySpend(
                        category = cat,
                        totalAmount = catAmount,
                        percentageShare = (catAmount / totalExp).toFloat()
                    )
                }
                .sortedByDescending { it.totalAmount }
        } else emptyList()

        val poolForSearch = if (searchAllTime) allTx else scopedTx
        val filteredTx = poolForSearch.filter { tx ->
            val matchesQuery = query.isEmpty() ||
                    tx.merchantOrParty.contains(query, ignoreCase = true) ||
                    tx.code.contains(query, ignoreCase = true) ||
                    tx.rawText.contains(query, ignoreCase = true)

            val matchesCategory = categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)

            matchesQuery && matchesCategory
        }

        val latestTx = allTx.maxByOrNull { it.timestamp }
        val latestBal = latestTx?.runningBalance
        val latestTxTs = latestTx?.timestamp

        DashboardUiState(
            dateScopeMode = scopeMode,
            todaySpend = todaySpent,
            dailyBudgetLimit = budget?.dailyLimit ?: 1000.0,
            monthIncome = periodIncome,
            monthExpense = periodExpense,
            topCategories = categorySpends,
            transactions = filteredTx,
            searchQuery = query,
            isSearchAllTime = searchAllTime,
            selectedCategoryFilter = categoryFilter,
            isRefreshing = refreshing,
            currentBalance = latestBal,
            latestTransactionTimestamp = latestTxTs
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshDashboard(context: Context, database: MahariDatabase) {
        viewModelScope.launch {
            _isRefreshing.value = true
            // 1. Fast cosmetic re-query
            delay(300)

            // 2. Throttled real SMS re-scan (once every 3 minutes)
            val now = System.currentTimeMillis()
            if (now - lastFullScanTime > 3 * 60 * 1000L) {
                try {
                    SmsBackfillManager.performOneTimeBackfill(
                        context = context,
                        transactionDao = database.transactionDao(),
                        mappingDao = database.merchantMappingDao()
                    )
                    lastFullScanTime = now
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            _isRefreshing.value = false
        }
    }

    fun onPreviousMonth() {
        val current = _dateScopeMode.value
        if (current is DateScopeMode.MonthMode) {
            _dateScopeMode.value = current.stepPrevious()
        } else {
            _dateScopeMode.value = DateScopeMode.currentMonth().stepPrevious()
        }
    }

    fun onNextMonth() {
        val current = _dateScopeMode.value
        if (current is DateScopeMode.MonthMode) {
            _dateScopeMode.value = current.stepNext()
        }
    }

    fun onCustomRangeSelected(start: Long, end: Long) {
        _dateScopeMode.value = DateScopeMode.CustomRangeMode(start, end)
    }

    fun onResetToCurrentMonth() {
        _dateScopeMode.value = DateScopeMode.currentMonth()
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onSearchAllTimeToggled(searchAllTime: Boolean) {
        _isSearchAllTime.value = searchAllTime
    }

    fun onCategoryFilterSelected(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun recategorizeMerchant(merchant: String, newCategory: String) {
        viewModelScope.launch {
            repository.recategorizeMerchant(merchant, newCategory)
        }
    }

    fun updateMonthlyBudget(monthlyLimit: Double) {
        viewModelScope.launch {
            repository.setMonthlyBudget(monthlyLimit)
        }
    }
}
