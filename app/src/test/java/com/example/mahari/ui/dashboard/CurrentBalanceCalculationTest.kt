package com.example.mahari.ui.dashboard

import com.example.mahari.data.db.TransactionEntity
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import kotlin.math.abs

class CurrentBalanceCalculationTest {

    @Test
    fun `most recent transaction by timestamp determines current balance regardless of list order`() {
        val olderTx = TransactionEntity(
            code = "TX_OLDER",
            amount = 500.0,
            merchantOrParty = "KPLC",
            category = "Utilities",
            type = "PAYBILL",
            runningBalance = 3500.0,
            timestamp = 1700000000000L,
            rawText = "Older SMS",
            isExpense = true
        )

        val newestTx = TransactionEntity(
            code = "TX_NEWEST",
            amount = 1200.0,
            merchantOrParty = "Supermarket",
            category = "Groceries",
            type = "TILL",
            runningBalance = 1200.45,
            timestamp = 1700086400000L, // Newer timestamp
            rawText = "Newest SMS",
            isExpense = true
        )

        // Put olderTx second, newestTx first, or vice-versa
        val transactionsOutOfOrder = listOf(olderTx, newestTx)

        val latestTx = transactionsOutOfOrder.maxByOrNull { it.timestamp }

        assertNotNull(latestTx)
        assertEquals("TX_NEWEST", latestTx!!.code)
        assertEquals(1200.45, latestTx.runningBalance, 0.001)
        assertEquals(1700086400000L, latestTx.timestamp)
    }

    @Test
    fun `negative balance correctly preserves negative value without crashing`() {
        val overdraftTx = TransactionEntity(
            code = "FULIZA_01",
            amount = 1500.0,
            merchantOrParty = "Fuliza Overdraft",
            category = "Services",
            type = "FULIZA_DEBT",
            runningBalance = -450.0,
            timestamp = 1700090000000L,
            rawText = "Fuliza SMS",
            isExpense = true
        )

        val transactions = listOf(overdraftTx)
        val latestTx = transactions.maxByOrNull { it.timestamp }

        assertNotNull(latestTx)
        assertEquals(-450.0, latestTx!!.runningBalance, 0.001)

        val formattedBalance = if (latestTx.runningBalance >= 0.0) {
            "Ksh ${"%.2f".format(latestTx.runningBalance)}"
        } else {
            "-Ksh ${"%.2f".format(abs(latestTx.runningBalance))}"
        }

        assertEquals("-Ksh 450.00", formattedBalance)
    }

    @Test
    fun `empty transaction history results in null balance triggering balance unknown state`() {
        val emptyTransactions = emptyList<TransactionEntity>()
        val latestTx = emptyTransactions.maxByOrNull { it.timestamp }

        assertNull(latestTx)
        assertNull(latestTx?.runningBalance)
        assertNull(latestTx?.timestamp)
    }
}
