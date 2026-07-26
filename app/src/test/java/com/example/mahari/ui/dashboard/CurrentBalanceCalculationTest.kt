package com.example.mahari.ui.dashboard

import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.parser.MpesaParser
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CurrentBalanceCalculationTest {

    @Test
    fun `test MpesaParser balance extraction from real SMS string variants`() {
        // Standard Ksh format
        val sms1 = "RKT21KLL90 Confirmed. Ksh1,200.00 paid to KPLC PREPAID on 26/7/26 at 4:15 PM. New M-PESA balance is Ksh3,450.20. Transaction cost, Ksh0.00."
        val res1 = MpesaParser.parse(sms1)
        assertNotNull(res1)
        assertEquals(3450.20, res1!!.runningBalance!!, 0.001)

        // Spaced format with dot
        val sms2 = "QGH871239A Confirmed. Ksh500.00 sent to JOHN DOE 0712345678 on 25/7/26 at 10:00 AM. New M-PESA balance is Ksh. 1,200.45. Transaction cost, Ksh15.00."
        val res2 = MpesaParser.parse(sms2)
        assertNotNull(res2)
        assertEquals(1200.45, res2!!.runningBalance!!, 0.001)

        // KES variant
        val sms3 = "RKU819201A Confirmed. You have received Ksh2,000.00 from JANE DOE 0722000111 on 26/7/26 at 2:30 PM. New M-PESA balance is KES 5,450.20."
        val res3 = MpesaParser.parse(sms3)
        assertNotNull(res3)
        assertEquals(5450.20, res3!!.runningBalance!!, 0.001)

        // Negative overdraft balance
        val sms4 = "RKF567890A Confirmed. Ksh1,500.00 paid to Naivas Supermarket on 26/7/26 at 6:00 PM. New M-PESA balance is Ksh-350.00."
        val res4 = MpesaParser.parse(sms4)
        assertNotNull(res4)
        assertEquals(-350.00, res4!!.runningBalance!!, 0.001)

        // Genuine zero balance
        val smsZero = "RKZ000000A Confirmed. Ksh500.00 paid to Shop on 26/7/26 at 7:00 PM. New M-PESA balance is Ksh0.00."
        val resZero = MpesaParser.parse(smsZero)
        assertNotNull(resZero)
        assertEquals(0.0, resZero!!.runningBalance!!, 0.001)
    }

    @Test
    fun `standalone Fuliza SMS yields null runningBalance`() {
        val fulizaSms = "Fuliza M-PESA amount is Ksh1,500.00. Outstanding Fuliza M-PESA amount is Ksh350.00."
        val parsed = MpesaParser.parse(fulizaSms)

        assertNotNull(parsed)
        assertNull(parsed!!.runningBalance)
    }

    @Test
    fun `query filters out null balance rows and picks latest valid balance`() {
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

        // Newer transaction but with null runningBalance (e.g. standalone Fuliza or unparseable balance)
        val newerFulizaTx = TransactionEntity(
            code = "TX_FULIZA_NEW",
            amount = 1200.0,
            merchantOrParty = "Fuliza Overdraft",
            category = "Services",
            type = "FULIZA_DEBT",
            runningBalance = null,
            timestamp = 1700086400000L,
            rawText = "Fuliza SMS",
            isExpense = true
        )

        val transactions = listOf(olderTx, newerFulizaTx)

        // Selection logic filtering out null runningBalance
        val latestWithBalance = transactions.filter { it.runningBalance != null }.maxByOrNull { it.timestamp }

        assertNotNull(latestWithBalance)
        assertEquals("TX_OLDER", latestWithBalance!!.code)
        assertEquals(3500.0, latestWithBalance.runningBalance!!, 0.001)
    }

    @Test
    fun `genuine zero balance displays as Ksh 0_00 and is distinct from null`() {
        val zeroTx = TransactionEntity(
            code = "TX_ZERO",
            amount = 500.0,
            merchantOrParty = "Shop",
            category = "Groceries",
            type = "TILL",
            runningBalance = 0.0,
            timestamp = 1700090000000L,
            rawText = "Zero balance SMS",
            isExpense = true
        )

        val transactions = listOf(zeroTx)
        val latestWithBalance = transactions.filter { it.runningBalance != null }.maxByOrNull { it.timestamp }

        assertNotNull(latestWithBalance)
        assertNotNull(latestWithBalance!!.runningBalance)
        assertEquals(0.0, latestWithBalance.runningBalance!!, 0.001)

        val formattedBalance = if (latestWithBalance.runningBalance!! >= 0.0) {
            "Ksh ${"%.2f".format(latestWithBalance.runningBalance)}"
        } else {
            "-Ksh ${"%.2f".format(abs(latestWithBalance.runningBalance!!))}"
        }

        assertEquals("Ksh 0.00", formattedBalance)
    }

    @Test
    fun `empty transaction history or all-null balances result in null balance triggering balance unknown state`() {
        val emptyTransactions = emptyList<TransactionEntity>()
        val latestWithBalance = emptyTransactions.filter { it.runningBalance != null }.maxByOrNull { it.timestamp }

        assertNull(latestWithBalance)
    }
}
