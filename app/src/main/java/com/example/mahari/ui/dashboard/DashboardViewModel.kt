package com.example.mahari.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mahari.data.db.BudgetEntity
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.repository.TransactionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategorySpend(
    val category: String,
    val totalAmount: Double,
    val percentageShare: Float
)

data class SummaryData(
    val todaySpend: Double = 0.0,
    val budget: BudgetEntity? = null,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0
)

data class DashboardUiState(
    val todaySpend: Double = 0.0,
    val dailyBudgetLimit: Double = 1000.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0,
    val topCategories: List<CategorySpend> = emptyList(),
    val transactions: List<TransactionEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null
)

class DashboardViewModel(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val summaryFlow: Flow<SummaryData> = combine(
        repository.getTodayExpenses(),
        repository.currentBudget,
        repository.getMonthIncome(),
        repository.getMonthExpenses()
    ) { todayExp, budget, monthInc, monthExp ->
        SummaryData(
            todaySpend = todayExp ?: 0.0,
            budget = budget,
            monthIncome = monthInc ?: 0.0,
            monthExpense = monthExp ?: 0.0
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        summaryFlow,
        repository.allTransactions,
        _searchQuery,
        _selectedCategoryFilter
    ) { summary, allTx: List<TransactionEntity>, query: String, categoryFilter: String? ->

        val todaySpent = summary.todaySpend
        val dailyBudget = summary.budget?.dailyLimit ?: 1000.0
        val income = summary.monthIncome
        val expense = summary.monthExpense

        // Filter transactions
        val filteredTx = allTx.filter { tx ->
            val matchesQuery = query.isEmpty() ||
                    tx.merchantOrParty.contains(query, ignoreCase = true) ||
                    tx.code.contains(query, ignoreCase = true) ||
                    tx.rawText.contains(query, ignoreCase = true)

            val matchesCategory = categoryFilter == null || tx.category.equals(categoryFilter, ignoreCase = true)

            matchesQuery && matchesCategory
        }

        // Calculate Category Breakdown
        val expenseTx = allTx.filter { it.isExpense }
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

        DashboardUiState(
            todaySpend = todaySpent,
            dailyBudgetLimit = dailyBudget,
            monthIncome = income,
            monthExpense = expense,
            topCategories = categorySpends,
            transactions = filteredTx,
            searchQuery = query,
            selectedCategoryFilter = categoryFilter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onCategoryFilterSelected(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun recategorizeMerchant(merchant: String, newCategory: String) {
        viewModelScope.launch {
            repository.recategorizeMerchant(merchant, newCategory)
        }
    }
}
