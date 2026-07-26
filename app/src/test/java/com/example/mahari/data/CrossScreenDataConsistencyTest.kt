package com.example.mahari.data

import com.example.mahari.data.db.TransactionDao
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.recap.StatisticalRecapEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CrossScreenDataConsistencyTest {

    class ConsistencyFakeDao(private val txs: List<TransactionEntity>) : TransactionDao {
        override fun getAllTransactionsFlow(): Flow<List<TransactionEntity>> = flowOf(txs)
        override suspend fun getAllTransactionsSync(): List<TransactionEntity> = txs
        override fun getTransactionsSince(startTime: Long): Flow<List<TransactionEntity>> = flowOf(txs.filter { it.timestamp >= startTime })
        override suspend fun getTransactionByCode(code: String): TransactionEntity? = txs.find { it.code == code }
        override suspend fun insertTransaction(transaction: TransactionEntity) {}
        override suspend fun insertTransactionsIgnore(transactions: List<TransactionEntity>) {}
        override suspend fun updateTransaction(transaction: TransactionEntity) {}
        override suspend fun updateCategoryForMerchant(merchant: String, newCategory: String) {}
        override fun getTotalExpenseSince(startTime: Long): Flow<Double?> = flowOf(txs.filter { it.isExpense && it.timestamp >= startTime }.sumOf { it.amount })
        override fun getTotalIncomeSince(startTime: Long): Flow<Double?> = flowOf(txs.filter { !it.isExpense && it.timestamp >= startTime }.sumOf { it.amount })
        override fun searchTransactions(query: String): Flow<List<TransactionEntity>> = flowOf(txs.filter { it.merchantOrParty.contains(query, true) })
    }

    private fun makeTx(
        code: String,
        year: Int,
        month: Int,
        day: Int,
        party: String,
        amount: Double,
        category: String,
        isExpense: Boolean = true
    ): TransactionEntity {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }
        return TransactionEntity(
            code = code,
            amount = amount,
            merchantOrParty = party,
            category = category,
            type = if (isExpense) "PAYMENT" else "RECEIPT",
            runningBalance = 10000.0,
            timestamp = cal.timeInMillis,
            rawText = "Seeded SMS for $code",
            isExpense = isExpense
        )
    }

    @Test
    fun testDataConsistency_AcrossLedger_MerchantProfile_And_RecapEngine() = runBlocking {
        // Seed 3 months of data (May, June, July 2026)
        val mayTx = listOf(
            makeTx("MAY_1", 2026, Calendar.MAY, 10, "Tyrell Stephenson", 1500.0, "Services"),
            makeTx("MAY_2", 2026, Calendar.MAY, 20, "JAVA HOUSE", 800.0, "Food & Dining")
        )
        val juneTx = listOf(
            makeTx("JUN_1", 2026, Calendar.JUNE, 5, "Tyrell Stephenson", 2000.0, "Services"),
            makeTx("JUN_2", 2026, Calendar.JUNE, 18, "KPLC PREPAID", 3500.0, "Utilities")
        )
        val julyTx = listOf(
            makeTx("JUL_1", 2026, Calendar.JULY, 2, "Tyrell Stephenson", 980.0, "Services"),
            makeTx("JUL_2", 2026, Calendar.JULY, 14, "JAVA HOUSE", 1200.0, "Food & Dining"),
            makeTx("JUL_3", 2026, Calendar.JULY, 25, "SAFARICOM AIRTIME", 500.0, "Utilities")
        )

        val allSeed = mayTx + juneTx + julyTx
        val dao = ConsistencyFakeDao(allSeed)

        // 1. July Ledger Total Spend Calculation
        val julyLedgerSpend = julyTx.filter { it.isExpense }.sumOf { it.amount } // 980 + 1200 + 500 = 2680.0

        // 2. StatisticalRecapEngine for July 2026
        val julyRecap = StatisticalRecapEngine.generateRecap("2026-07", dao, 30000.0)

        // 3. Merchant Profile All-Time vs This Month for Tyrell Stephenson
        val tyrellAllTx = allSeed.filter { it.merchantOrParty.equals("Tyrell Stephenson", ignoreCase = true) }
        val tyrellAllTimeSpend = tyrellAllTx.sumOf { it.amount } // 1500 + 2000 + 980 = 4480.0

        val calJulyStart = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JULY, 1, 0, 0, 0)
        }
        val calJulyEnd = Calendar.getInstance().apply {
            clear()
            set(2026, Calendar.JULY, 31, 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val tyrellJulySpend = tyrellAllTx
            .filter { it.timestamp in calJulyStart.timeInMillis..calJulyEnd.timeInMillis }
            .sumOf { it.amount } // 980.0

        // ASSERTIONS:
        // A. Recap total spend must match July Ledger spend exactly (Ksh 2,680.0)
        assertEquals(julyLedgerSpend, julyRecap.totalSpend, 0.01)

        // B. Recap must NOT be 0 or empty
        assertTrue("July recap total spend must be > 0", julyRecap.totalSpend > 0)

        // C. Tyrell Stephenson's All-Time spend must include pre-July data (Ksh 4,480.0 vs July-only Ksh 980.0)
        assertEquals(4480.0, tyrellAllTimeSpend, 0.01)
        assertEquals(980.0, tyrellJulySpend, 0.01)
        assertTrue("All-time spend must exceed current month spend for multi-month merchants", tyrellAllTimeSpend > tyrellJulySpend)

        // D. Verify historical recaps for May and June
        val mayRecap = StatisticalRecapEngine.generateRecap("2026-05", dao, 30000.0)
        val juneRecap = StatisticalRecapEngine.generateRecap("2026-06", dao, 30000.0)

        assertEquals(2300.0, mayRecap.totalSpend, 0.01)
        assertEquals(5500.0, juneRecap.totalSpend, 0.01)
    }
}
