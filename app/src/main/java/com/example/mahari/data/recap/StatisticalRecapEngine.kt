package com.example.mahari.data.recap

import com.example.mahari.data.db.RecapEntity
import com.example.mahari.data.db.TransactionDao
import com.example.mahari.data.db.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object StatisticalRecapEngine {

    suspend fun generateRecap(
        monthYear: String, // e.g. "2026-07"
        transactionDao: TransactionDao,
        monthlyBudget: Double
    ): RecapEntity {
        val allTx = transactionDao.getAllTransactionsSync()

        val dateParts = monthYear.split("-")
        val year = dateParts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month0 = (dateParts.getOrNull(1)?.toIntOrNull() ?: 7) - 1

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month0)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startOfMonth = cal.timeInMillis
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val endCal = Calendar.getInstance().apply {
            timeInMillis = startOfMonth
            set(Calendar.DAY_OF_MONTH, maxDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        val endOfMonth = endCal.timeInMillis

        val monthTx = allTx.filter { it.timestamp in startOfMonth..endOfMonth }
        val expenseTx = monthTx.filter { it.isExpense }

        if (expenseTx.isEmpty()) {
            return RecapEntity(
                monthYear = monthYear,
                headlineInsight = "No expense activity recorded for $monthYear",
                totalSpend = 0.0,
                topCategory = "None",
                shapExplanationsJson = "[\"No M-Pesa expense receipts logged during this month.\"]",
                timestamp = System.currentTimeMillis()
            )
        }

        val totalSpend = expenseTx.sumOf { it.amount }
        val catSums = expenseTx.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
        val topCategory = catSums.maxByOrNull { it.value }?.key ?: "General"
        val topCatSpend = catSums[topCategory] ?: 0.0

        val explanations = mutableListOf<String>()

        // 1. Category Delta vs 3-Month Rolling Average
        val prior3MonthsTx = mutableListOf<TransactionEntity>()
        var priorMonthsAvailable = 0
        for (i in 1..3) {
            val pCal = Calendar.getInstance().apply {
                timeInMillis = startOfMonth
                add(Calendar.MONTH, -i)
            }
            val pStart = pCal.timeInMillis
            val pMax = pCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val pEndCal = Calendar.getInstance().apply {
                timeInMillis = pStart
                set(Calendar.DAY_OF_MONTH, pMax)
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
            }
            val pEnd = pEndCal.timeInMillis
            val pTx = allTx.filter { it.timestamp in pStart..pEnd && it.isExpense }
            if (pTx.isNotEmpty()) {
                prior3MonthsTx.addAll(pTx)
                priorMonthsAvailable++
            }
        }

        if (priorMonthsAvailable > 0) {
            val priorCatSums = prior3MonthsTx.groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } / priorMonthsAvailable }
            val baselineAvg = priorCatSums[topCategory] ?: 0.0

            if (baselineAvg > 0) {
                val variancePct = ((topCatSpend - baselineAvg) / baselineAvg) * 100.0
                val varianceDirection = if (variancePct >= 0) "above" else "below"
                explanations.add(
                    "$topCategory spend was ${"%.0f".format(kotlin.math.abs(variancePct))}% $varianceDirection your $priorMonthsAvailable-month rolling baseline average of Ksh ${"%.0f".format(baselineAvg)}."
                )
            } else {
                explanations.add(
                    "$topCategory was your primary spending driver at Ksh ${"%.2f".format(topCatSpend)} (${"%.0f".format((topCatSpend / totalSpend) * 100)}% of total monthly spend)."
                )
            }
        } else {
            explanations.add(
                "$topCategory was your primary spending driver at Ksh ${"%.2f".format(topCatSpend)} (${"%.0f".format((topCatSpend / totalSpend) * 100)}% of total monthly spend)."
            )
        }

        // 2. Merchant Frequency Anomaly Detection
        val merchantCounts = expenseTx.groupBy { it.merchantOrParty }.mapValues { it.value.size }
        val topMerchantEntry = merchantCounts.maxByOrNull { it.value }
        if (topMerchantEntry != null && topMerchantEntry.value >= 2) {
            val mName = topMerchantEntry.key
            val mCount = topMerchantEntry.value
            val priorMCountAvg = if (priorMonthsAvailable > 0) {
                prior3MonthsTx.count { it.merchantOrParty.equals(mName, ignoreCase = true) } / priorMonthsAvailable.toDouble()
            } else 0.0

            if (priorMCountAvg > 0) {
                val ratio = mCount / priorMCountAvg
                if (ratio >= 1.5) {
                    explanations.add(
                        "Merchant Anomaly: $mName visit frequency increased ${"%.1f".format(ratio)}x ($mCount payments vs. prior avg of ${"%.1f".format(priorMCountAvg)})."
                    )
                } else {
                    explanations.add("Top Merchant: $mName recorded $mCount payments totaling Ksh ${"%.2f".format(expenseTx.filter { it.merchantOrParty == mName }.sumOf { it.amount })}.")
                }
            } else {
                explanations.add("Top Merchant: $mName recorded $mCount payments totaling Ksh ${"%.2f".format(expenseTx.filter { it.merchantOrParty == mName }.sumOf { it.amount })}.")
            }
        }

        // 3. Budget Variance
        val isOver = totalSpend > monthlyBudget
        val diff = kotlin.math.abs(totalSpend - monthlyBudget)
        val budgetText = if (isOver) {
            "Budget Variance: Total spend of Ksh ${"%.2f".format(totalSpend)} exceeded your Ksh ${"%.0f".format(monthlyBudget)} limit by Ksh ${"%.2f".format(diff)}."
        } else {
            "Budget Variance: Total spend of Ksh ${"%.2f".format(totalSpend)} stayed under your Ksh ${"%.0f".format(monthlyBudget)} limit with Ksh ${"%.2f".format(diff)} remaining."
        }
        explanations.add(budgetText)

        // 4. Largest Transaction
        val maxTx = expenseTx.maxByOrNull { it.amount }
        if (maxTx != null) {
            val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(maxTx.timestamp))
            explanations.add("Largest Payment: Ksh ${"%.2f".format(maxTx.amount)} paid to ${maxTx.merchantOrParty} on $dateStr.")
        }

        val jsonExplanations = explanations.joinToString(prefix = "[\"", postfix = "\"]", separator = "\", \"") {
            it.replace("\"", "\\\"")
        }

        val headline = if (isOver) {
            "$topCategory spend drove total outflow Ksh ${"%.0f".format(diff)} over monthly budget"
        } else {
            "$topCategory spend remained controlled; under budget by Ksh ${"%.0f".format(diff)}"
        }

        return RecapEntity(
            monthYear = monthYear,
            headlineInsight = headline,
            totalSpend = totalSpend,
            topCategory = topCategory,
            shapExplanationsJson = jsonExplanations,
            timestamp = System.currentTimeMillis()
        )
    }
}
