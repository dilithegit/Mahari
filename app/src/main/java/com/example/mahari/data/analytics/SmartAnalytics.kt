package com.example.mahari.data.analytics

import com.example.mahari.data.db.TransactionEntity
import kotlin.math.pow
import kotlin.math.sqrt

data class RecurringPattern(
    val merchant: String,
    val averageAmount: Double,
    val frequencyCount: Int,
    val category: String
)

object RecurringDetector {
    fun detectRecurring(transactions: List<TransactionEntity>): List<RecurringPattern> {
        val expenses = transactions.filter { it.isExpense }
        return expenses.groupBy { it.merchantOrParty }
            .filter { (_, txs) -> txs.size >= 2 }
            .map { (merchant, txs) ->
                val avgAmt = txs.map { it.amount }.average()
                RecurringPattern(
                    merchant = merchant,
                    averageAmount = avgAmt,
                    frequencyCount = txs.size,
                    category = txs.first().category
                )
            }
    }
}

object AnomalyDetector {
    fun isAnomaly(amount: Double, merchantTransactions: List<TransactionEntity>): Boolean {
        if (merchantTransactions.size < 3) return false

        val amounts = merchantTransactions.map { it.amount }
        val mean = amounts.average()
        val stdDev = sqrt(amounts.map { (it - mean).pow(2) }.average())

        return stdDev > 0 && amount > (mean + 2.0 * stdDev)
    }
}
