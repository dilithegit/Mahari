package com.example.mahari.data.recap

import com.example.mahari.data.db.TransactionEntity
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class FinancialScoreResult(
    val financialScore: Double?,
    val confidenceRating: Double?,
    val breakdownJson: String,
    val isInsufficientData: Boolean
)

object FinancialScoreEngine {

    fun computeFinancialScore(
        monthTx: List<TransactionEntity>,
        prior3MonthsTx: List<TransactionEntity>,
        priorMonthsAvailable: Int,
        monthlyBudget: Double
    ): FinancialScoreResult {
        val expenseTx = monthTx.filter { it.isExpense }
        val incomeTx = monthTx.filter { !it.isExpense }

        // Edge Case: Sparse Data (< 3 expense transactions in month)
        if (expenseTx.size < 3) {
            return FinancialScoreResult(
                financialScore = null,
                confidenceRating = null,
                breakdownJson = "{\"status\":\"insufficient_data\"}",
                isInsufficientData = true
            )
        }

        val totalExpense = expenseTx.sumOf { it.amount }
        val totalIncome = incomeTx.sumOf { it.amount }

        // 1. Factor 1: Budget Adherence (Base Weight: 35%)
        val budgetScore = if (totalExpense <= monthlyBudget) {
            100.0
        } else {
            val overPct = ((totalExpense - monthlyBudget) / monthlyBudget) * 100.0
            max(0.0, 100.0 - (overPct * 1.5))
        }

        // 2. Factor 2: Savings Rate (Base Weight: 25%)
        val hasIncome = totalIncome > 0.0
        val savingsRateScore = if (hasIncome) {
            val savingsPct = ((totalIncome - totalExpense) / totalIncome) * 100.0
            max(0.0, min(100.0, 50.0 + savingsPct))
        } else {
            // Edge Case: Zero Income Fallback (Treat as neutral, reweight remaining 3 factors)
            null
        }

        // 3. Factor 3: Spending Consistency / Anomaly Frequency (Base Weight: 20%)
        var consistencyScore = 100.0
        val merchantCounts = expenseTx.groupBy { it.merchantOrParty }.mapValues { it.value.size }
        val topMerchantCount = merchantCounts.maxByOrNull { it.value }?.value ?: 0
        if (topMerchantCount >= 4) {
            consistencyScore -= 15.0 // High visit frequency penalty
        }

        val catSums = expenseTx.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
        val topCatSpend = catSums.maxByOrNull { it.value }?.value ?: 0.0
        if (totalExpense > 0 && (topCatSpend / totalExpense) > 0.65) {
            consistencyScore -= 15.0 // Concentration risk penalty
        }
        consistencyScore = max(0.0, consistencyScore)

        // 4. Factor 4: Debt Reliance / Fuliza Usage (Base Weight: 20%)
        val fulizaTx = expenseTx.filter {
            it.merchantOrParty.contains("Fuliza", ignoreCase = true) ||
                    it.rawText.contains("Fuliza", ignoreCase = true) ||
                    it.fulizaOutstanding > 0.0
        }
        val debtScore = if (fulizaTx.isEmpty()) {
            // Edge Case: Zero Fuliza Usage = 100.0% Full Marks!
            100.0
        } else {
            val fulizaTotal = fulizaTx.sumOf { it.amount }
            val fulizaRatio = if (totalExpense > 0) fulizaTotal / totalExpense else 0.0
            max(0.0, 100.0 - (fulizaRatio * 200.0))
        }

        // Weighted Score Aggregation with Zero-Income Reweighting
        val finalScore = if (savingsRateScore != null) {
            (budgetScore * 0.35) + (savingsRateScore * 0.25) + (consistencyScore * 0.20) + (debtScore * 0.20)
        } else {
            // Reweight remaining 3 factors: Budget 46.7%, Consistency 26.7%, Debt 26.6%
            (budgetScore * 0.467) + (consistencyScore * 0.267) + (debtScore * 0.266)
        }

        // Confidence Rating based on Data Volume / History Depth
        val confidenceRating = when (priorMonthsAvailable) {
            0 -> 35.0 // 1 Month History
            1 -> 60.0 // 2 Months History
            2 -> 80.0 // 3 Months History
            else -> 95.0 // 4+ Months History
        }

        val jsonBreakdown = """
            {
              "budgetAdherence": ${"%.1f".format(budgetScore)},
              "savingsRate": ${if (savingsRateScore != null) "%.1f".format(savingsRateScore) else "\"N/A\""},
              "consistency": ${"%.1f".format(consistencyScore)},
              "debtReliance": ${"%.1f".format(debtScore)}
            }
        """.trimIndent().replace("\n", "")

        return FinancialScoreResult(
            financialScore = max(0.0, min(100.0, finalScore)),
            confidenceRating = confidenceRating,
            breakdownJson = jsonBreakdown,
            isInsufficientData = false
        )
    }
}
