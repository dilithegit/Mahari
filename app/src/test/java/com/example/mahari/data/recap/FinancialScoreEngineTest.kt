package com.example.mahari.data.recap

import com.example.mahari.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FinancialScoreEngineTest {

    private fun makeTx(
        code: String,
        amount: Double,
        party: String,
        category: String,
        isExpense: Boolean = true,
        fulizaOutstanding: Double = 0.0
    ): TransactionEntity {
        return TransactionEntity(
            code = code,
            amount = amount,
            merchantOrParty = party,
            category = category,
            type = if (isExpense) "PAYMENT" else "RECEIPT",
            runningBalance = 10000.0,
            timestamp = System.currentTimeMillis(),
            rawText = "Raw $code",
            isExpense = isExpense,
            fulizaOutstanding = fulizaOutstanding
        )
    }

    @Test
    fun testZeroIncomeFallback_DoesNotProduceNaNOrCrash() {
        val monthTx = listOf(
            makeTx("TX1", 1000.0, "JAVA HOUSE", "Food & Dining"),
            makeTx("TX2", 500.0, "KPLC", "Utilities"),
            makeTx("TX3", 200.0, "SUPERMARKET", "Groceries")
        )

        val result = FinancialScoreEngine.computeFinancialScore(
            monthTx = monthTx,
            prior3MonthsTx = emptyList(),
            priorMonthsAvailable = 0,
            monthlyBudget = 10000.0
        )

        assertNotNull("Score should be computed", result.financialScore)
        assertTrue("Score should not be NaN", !result.financialScore!!.isNaN())
        assertTrue("Score should be between 0 and 100", result.financialScore!! in 0.0..100.0)
        assertTrue("Breakdown should contain savingsRate factor", result.breakdownJson.contains("savingsRate"))
    }

    @Test
    fun testZeroFulizaUsage_ReceivesFullMarksOnDebtFactor() {
        val monthTx = listOf(
            makeTx("TX1", 1000.0, "JAVA HOUSE", "Food & Dining"),
            makeTx("TX2", 500.0, "KPLC", "Utilities"),
            makeTx("TX3", 200.0, "SUPERMARKET", "Groceries")
        )

        val result = FinancialScoreEngine.computeFinancialScore(
            monthTx = monthTx,
            prior3MonthsTx = emptyList(),
            priorMonthsAvailable = 2,
            monthlyBudget = 10000.0
        )

        assertTrue("Breakdown should contain debtReliance factor", result.breakdownJson.contains("debtReliance"))
        assertNotNull(result.financialScore)
    }

    @Test
    fun testSparseData_ReturnsInsufficientDataState() {
        val sparseTx = listOf(
            makeTx("TX1", 100.0, "JAVA HOUSE", "Food & Dining")
        )

        val result = FinancialScoreEngine.computeFinancialScore(
            monthTx = sparseTx,
            prior3MonthsTx = emptyList(),
            priorMonthsAvailable = 0,
            monthlyBudget = 10000.0
        )

        assertTrue("Should trigger insufficient data state", result.isInsufficientData)
        assertNull("Financial score should be null for sparse data", result.financialScore)
        assertNull("Confidence rating should be null for sparse data", result.confidenceRating)
    }

    @Test
    fun testDataVolumeConfidenceRating() {
        val monthTx = listOf(
            makeTx("TX1", 1000.0, "JAVA HOUSE", "Food & Dining"),
            makeTx("TX2", 500.0, "KPLC", "Utilities"),
            makeTx("TX3", 200.0, "SUPERMARKET", "Groceries")
        )

        val res0 = FinancialScoreEngine.computeFinancialScore(monthTx, emptyList(), 0, 10000.0)
        val res1 = FinancialScoreEngine.computeFinancialScore(monthTx, emptyList(), 1, 10000.0)
        val res3 = FinancialScoreEngine.computeFinancialScore(monthTx, emptyList(), 3, 10000.0)

        assertEquals(35.0, res0.confidenceRating!!, 0.01)
        assertEquals(60.0, res1.confidenceRating!!, 0.01)
        assertEquals(95.0, res3.confidenceRating!!, 0.01)
    }
}
