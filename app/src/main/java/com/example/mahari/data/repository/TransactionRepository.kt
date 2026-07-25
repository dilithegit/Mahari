package com.example.mahari.data.repository

import com.example.mahari.data.db.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val currentBudget: Flow<BudgetEntity?> = budgetDao.getCurrentBudgetFlow()

    fun getTodayStartTimestamp(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getMonthStartTimestamp(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getTodayExpenses(): Flow<Double?> = transactionDao.getTotalExpenseSince(getTodayStartTimestamp())
    fun getMonthExpenses(): Flow<Double?> = transactionDao.getTotalExpenseSince(getMonthStartTimestamp())
    fun getMonthIncome(): Flow<Double?> = transactionDao.getTotalIncomeSince(getMonthStartTimestamp())

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> = transactionDao.searchTransactions(query)

    suspend fun recategorizeMerchant(merchant: String, newCategory: String) {
        // Save user preference mapping
        merchantMappingDao.insertMapping(
            MerchantCategoryMappingEntity(
                merchantPattern = merchant.uppercase(),
                category = newCategory
            )
        )
        // Update all historical transactions for this merchant
        transactionDao.updateCategoryForMerchant(merchant, newCategory)
    }

    suspend fun setMonthlyBudget(monthlyLimit: Double) {
        val cal = Calendar.getInstance()
        val monthYear = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"
        budgetDao.setBudget(
            BudgetEntity(
                monthlyLimit = monthlyLimit,
                monthYear = monthYear,
                dailyLimit = monthlyLimit / 30.0
            )
        )
    }
}
