package com.example.mahari.data.recap

import com.example.mahari.data.db.RecapDao
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

        // 1. Plain-Language Category Comparison
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
                val varianceDirection = if (variancePct >= 0) "more than" else "less than"
                explanations.add(
                    "You spent Ksh ${"%.2f".format(topCatSpend)} on $topCategory — about ${"%.0f".format(kotlin.math.abs(variancePct))}% $varianceDirection what you usually spend on $topCategory."
                )
            } else {
                explanations.add(
                    "$topCategory was your main spending category at Ksh ${"%.2f".format(topCatSpend)} (${"%.0f".format((topCatSpend / totalSpend) * 100)}% of your monthly spend)."
                )
            }
        } else {
            explanations.add(
                "$topCategory was your main spending category at Ksh ${"%.2f".format(topCatSpend)} (${"%.0f".format((topCatSpend / totalSpend) * 100)}% of your monthly spend)."
            )
        }

        // 2. Frequent Merchant Plain Statement
        val merchantCounts = expenseTx.groupBy { it.merchantOrParty }.mapValues { it.value.size }
        val topMerchantEntry = merchantCounts.maxByOrNull { it.value }
        if (topMerchantEntry != null && topMerchantEntry.value >= 2) {
            val mName = topMerchantEntry.key
            val mCount = topMerchantEntry.value
            val mTotal = expenseTx.filter { it.merchantOrParty == mName }.sumOf { it.amount }
            explanations.add("Top Merchant: You paid $mName $mCount times this month, totaling Ksh ${"%.2f".format(mTotal)}.")
        }

        // 3. Simple Budget Statement
        val isOver = totalSpend > monthlyBudget
        val diff = kotlin.math.abs(totalSpend - monthlyBudget)
        val budgetText = if (isOver) {
            "Monthly Budget: You spent Ksh ${"%.2f".format(totalSpend)}, which was Ksh ${"%.2f".format(diff)} over your Ksh ${"%.0f".format(monthlyBudget)} limit."
        } else {
            "Monthly Budget: You spent Ksh ${"%.2f".format(totalSpend)}, staying under your Ksh ${"%.0f".format(monthlyBudget)} limit with Ksh ${"%.2f".format(diff)} left over."
        }
        explanations.add(budgetText)

        // 4. Largest Transaction Plain Statement
        val maxTx = expenseTx.maxByOrNull { it.amount }
        if (maxTx != null) {
            val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(maxTx.timestamp))
            explanations.add("Single Largest Payment: Ksh ${"%.2f".format(maxTx.amount)} paid to ${maxTx.merchantOrParty} on $dateStr.")
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

    suspend fun backfillHistoricalRecaps(
        transactionDao: TransactionDao,
        recapDao: RecapDao,
        monthlyBudget: Double
    ) {
        val allTx = transactionDao.getAllTransactionsSync()
        if (allTx.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val distinctMonths = allTx.map { sdf.format(Date(it.timestamp)) }.distinct()

        distinctMonths.forEach { mYear ->
            val recap = generateRecap(mYear, transactionDao, monthlyBudget)
            if (recap.totalSpend > 0) {
                recapDao.insertRecap(recap)
            }
        }
    }
}
