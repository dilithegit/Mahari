package com.example.mahari.data.repository

import com.example.mahari.data.db.*
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TransactionRepository(
    private val database: MahariDatabase,
    private val transactionDao: TransactionDao,
    private val merchantMappingDao: MerchantMappingDao,
    private val budgetDao: BudgetDao
) {
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactionsFlow()
    val currentBudget: Flow<BudgetEntity?> = budgetDao.getCurrentBudgetFlow()

    fun getTodayStartTimestamp(): Long = com.example.mahari.util.DateUtils.getTodayStartTimestamp()
    fun getMonthStartTimestamp(): Long = com.example.mahari.util.DateUtils.getMonthStartTimestamp()

    fun getTodayExpenses(): Flow<Double?> = transactionDao.getTotalExpenseSince(getTodayStartTimestamp())
    fun getMonthExpenses(): Flow<Double?> = transactionDao.getTotalExpenseSince(getMonthStartTimestamp())
    fun getMonthIncome(): Flow<Double?> = transactionDao.getTotalIncomeSince(getMonthStartTimestamp())

    fun searchTransactions(query: String): Flow<List<TransactionEntity>> = transactionDao.searchTransactions(query)

    suspend fun recategorizeMerchant(merchant: String, newCategory: String) {
        database.withTransaction {
            merchantMappingDao.insertMapping(
                MerchantCategoryMappingEntity(
                    merchantPattern = merchant.uppercase().trim(),
                    category = newCategory
                )
            )
            transactionDao.updateCategoryForMerchant(merchant, newCategory)
        }
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
