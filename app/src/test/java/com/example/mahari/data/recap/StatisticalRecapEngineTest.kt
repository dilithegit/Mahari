package com.example.mahari.data.recap

import com.example.mahari.data.db.TransactionDao
import com.example.mahari.data.db.TransactionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class StatisticalRecapEngineTest {

    private class FakeTransactionDao(private val transactions: List<TransactionEntity>) : TransactionDao {
        override suspend fun getAllTransactionsSync(): List<TransactionEntity> = transactions
        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = flowOf(transactions)
        override fun getTransactionsSince(startTime: Long): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.timestamp >= startTime })
        override suspend fun getTransactionByCode(code: String): TransactionEntity? =
            transactions.find { it.code == code }
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun insertTransactionsIgnore(transactions: List<TransactionEntity>) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun updateCategoryForMerchant(merchant: String, newCategory: String) {}
        override fun getTotalExpenseSince(startTime: Long): Flow<Double?> =
            flowOf(transactions.filter { it.timestamp >= startTime && it.isExpense }.sumOf { it.amount })
        override fun getTotalIncomeSince(startTime: Long): Flow<Double?> =
            flowOf(transactions.filter { it.timestamp >= startTime && !it.isExpense }.sumOf { it.amount })
        override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
            flowOf(transactions.filter { it.merchantOrParty.contains(query, ignoreCase = true) })
    }

    private fun createTimestamp(year: Int, month0: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month0)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis
    }

    private fun makeTx(
        code: String,
        year: Int,
        month0: Int,
        day: Int,
        merchant: String,
        amount: Double,
        category: String,
        isExpense: Boolean = true
    ): TransactionEntity {
        return TransactionEntity(
            code = code,
            amount = amount,
            merchantOrParty = merchant,
            category = category,
            type = if (isExpense) "PAYMENT" else "RECEIVE",
            runningBalance = 10000.0,
            timestamp = createTimestamp(year, month0, day),
            rawText = "Raw text for $code",
            isExpense = isExpense
        )
    }

    @Test
    fun testEdgeCase1_ZeroTransactionsInTargetMonth() = runBlocking {
        val dao = FakeTransactionDao(emptyList())
        val recap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        assertEquals("2026-07", recap.monthYear)
        assertEquals(0.0, recap.totalSpend, 0.01)
        assertEquals("None", recap.topCategory)
        assertTrue(recap.headlineInsight.contains("No expense activity"))
    }

    @Test
    fun testEdgeCase2_Only1MonthHistoryAvailable() = runBlocking {
        val julyTx = listOf(
            makeTx("TX1", 2026, Calendar.JULY, 10, "JAVA HOUSE", 1200.0, "Food & Dining")
        )
        val dao = FakeTransactionDao(julyTx)
        val recap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        assertEquals(1200.0, recap.totalSpend, 0.01)
        assertEquals("Food & Dining", recap.topCategory)
        assertFalse("Should not throw divide by zero", recap.shapExplanationsJson.contains("NaN"))
        assertTrue(recap.shapExplanationsJson.contains("Food & Dining was your main spending category"))
    }

    @Test
    fun testEdgeCase3_Exactly3MonthsHistory() = runBlocking {
        val txs = listOf(
            makeTx("TX_APR", 2026, Calendar.APRIL, 15, "KPLC", 2000.0, "Utilities"),
            makeTx("TX_MAY", 2026, Calendar.MAY, 15, "KPLC", 2000.0, "Utilities"),
            makeTx("TX_JUN", 2026, Calendar.JUNE, 15, "KPLC", 2000.0, "Utilities"),
            makeTx("TX_JUL", 2026, Calendar.JULY, 15, "KPLC", 3000.0, "Utilities")
        )
        val dao = FakeTransactionDao(txs)
        val recap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        assertEquals(3000.0, recap.totalSpend, 0.01)
        assertEquals("Utilities", recap.topCategory)
        assertTrue(recap.shapExplanationsJson.contains("about 50% more than what you usually spend"))
    }

    @Test
    fun testEdgeCase4_MoreThan3MonthsHistory_UsesOnlyRecent3() = runBlocking {
        val txs = listOf(
            makeTx("TX_MAR", 2026, Calendar.MARCH, 15, "KPLC", 10000.0, "Utilities"),
            makeTx("TX_APR", 2026, Calendar.APRIL, 15, "KPLC", 2000.0, "Utilities"),
            makeTx("TX_MAY", 2026, Calendar.MAY, 15, "KPLC", 2000.0, "Utilities"),
            makeTx("TX_JUN", 2026, Calendar.JUNE, 15, "KPLC", 2000.0, "Utilities"),
            makeTx("TX_JUL", 2026, Calendar.JULY, 15, "KPLC", 3000.0, "Utilities")
        )
        val dao = FakeTransactionDao(txs)
        val recap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        assertTrue(recap.shapExplanationsJson.contains("about 50% more than what you usually spend"))
    }

    @Test
    fun testEdgeCase5_SingleLargePayment_NoNonsensicalFrequencyAnomaly() = runBlocking {
        val txs = listOf(
            makeTx("TX_JUL", 2026, Calendar.JULY, 15, "TUITION", 50000.0, "Education")
        )
        val dao = FakeTransactionDao(txs)
        val recap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        assertEquals(50000.0, recap.totalSpend, 0.01)
        assertFalse("Single payment must not trigger Merchant Anomaly", recap.shapExplanationsJson.contains("Merchant Anomaly"))
    }

    @Test
    fun testEdgeCase6_CategoryZeroSpendInTargetMonth_NoDivideByZero() = runBlocking {
        val txs = listOf(
            makeTx("TX_JUN", 2026, Calendar.JUNE, 15, "KPLC", 5000.0, "Utilities"),
            makeTx("TX_JUL", 2026, Calendar.JULY, 15, "JAVA", 2000.0, "Food & Dining")
        )
        val dao = FakeTransactionDao(txs)
        val recap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        assertEquals(2000.0, recap.totalSpend, 0.01)
        assertEquals("Food & Dining", recap.topCategory)
        assertFalse(recap.shapExplanationsJson.contains("NaN"))
        assertFalse(recap.shapExplanationsJson.contains("Infinity"))
    }
}
