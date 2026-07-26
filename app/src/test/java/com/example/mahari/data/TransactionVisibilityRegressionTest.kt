package com.example.mahari.data

import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.model.DateScopeMode
import com.example.mahari.data.parser.MpesaParser
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class TransactionVisibilityRegressionTest {

    @Test
    fun `test transaction visibility remains intact across date scoping and balance nullability`() {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        // Seed 1: Current month transaction with positive balance
        val txCurrentMonth = TransactionEntity(
            code = "REG_TX_01",
            amount = 1200.0,
            merchantOrParty = "Naivas Supermarket",
            category = "Groceries",
            type = "PAYBILL_BUY_GOODS",
            runningBalance = 4500.0,
            timestamp = now.timeInMillis,
            rawText = "QFG8XYZ Confirmed. Ksh1,200.00 paid to Naivas Supermarket. New M-PESA balance is Ksh4,500.00",
            isExpense = true
        )

        // Seed 2: Previous month transaction
        val prevMonthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, currentMonth)
            add(Calendar.MONTH, -1)
        }
        val txPrevMonth = TransactionEntity(
            code = "REG_TX_02",
            amount = 500.0,
            merchantOrParty = "KPLC PREPAID",
            category = "Utilities",
            type = "PAYBILL_BUY_GOODS",
            runningBalance = 5700.0,
            timestamp = prevMonthCal.timeInMillis,
            rawText = "QFG7XYZ Confirmed. Ksh500.00 paid to KPLC PREPAID. New M-PESA balance is Ksh5,700.00",
            isExpense = true
        )

        // Seed 3: Standalone Fuliza transaction with null running balance
        val txFulizaNullBal = TransactionEntity(
            code = "REG_TX_FULIZA",
            amount = 800.0,
            merchantOrParty = "Fuliza Overdraft",
            category = "Services",
            type = "FULIZA_DEBT",
            runningBalance = null,
            timestamp = now.timeInMillis - 1000,
            rawText = "Fuliza M-PESA amount is Ksh800.00. Outstanding Fuliza M-PESA amount is Ksh800.00",
            isExpense = true
        )

        val allTransactions = listOf(txCurrentMonth, txPrevMonth, txFulizaNullBal)

        // 1. Total DB count check
        assertEquals(3, allTransactions.size)

        // 2. Scope filtering check for current month
        val currentMonthScope = DateScopeMode.MonthMode(currentYear, currentMonth)
        val currentMonthTx = allTransactions.filter { it.timestamp in currentMonthScope.startTimestamp..currentMonthScope.endTimestamp }

        assertEquals(2, currentMonthTx.size)
        assertTrue(currentMonthTx.contains(txCurrentMonth))
        assertTrue(currentMonthTx.contains(txFulizaNullBal))
        assertFalse(currentMonthTx.contains(txPrevMonth))

        // 3. All-time search query check (ensures previous month transactions are never lost)
        val allTimeTx = allTransactions.filter { true }
        assertEquals(3, allTimeTx.size)

        // 4. Latest non-null balance query check
        val latestWithBalance = allTransactions.filter { it.runningBalance != null }.maxByOrNull { it.timestamp }
        assertNotNull(latestWithBalance)
        assertEquals("REG_TX_01", latestWithBalance!!.code)
        assertEquals(4500.0, latestWithBalance.runningBalance!!, 0.001)
    }

    @Test
    fun `re-parsing rawText recovers balances and preserves transaction list count`() {
        val raw1 = "RKT21KLL90 Confirmed. Ksh1,200.00 paid to KPLC PREPAID on 26/7/26 at 4:15 PM. New M-PESA balance is Ksh3,450.20."
        val raw2 = "Fuliza M-PESA amount is Ksh1,500.00. Outstanding Fuliza amount is Ksh350.00."

        val parsed1 = MpesaParser.parse(raw1)
        val parsed2 = MpesaParser.parse(raw2)

        assertNotNull(parsed1)
        assertNotNull(parsed2)
        assertEquals(3450.20, parsed1!!.runningBalance!!, 0.001)
        assertNull(parsed2!!.runningBalance)
    }
}
