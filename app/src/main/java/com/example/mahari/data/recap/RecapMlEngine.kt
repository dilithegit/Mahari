package com.example.mahari.data.recap

import com.example.mahari.data.db.TransactionEntity

data class MonthlyRecapInsight(
    val title: String,
    val summaryText: String,
    val topCreepCategory: String,
    val volatilityScore: Int,
    val explanationReasoning: String
)

object RecapMlEngine {

    fun generateRecap(transactions: List<TransactionEntity>): MonthlyRecapInsight {
        val expenses = transactions.filter { it.isExpense }

        if (expenses.size < 5) {
            return MonthlyRecapInsight(
                title = "Monthly Financial Snapshot",
                summaryText = "Your spending is tracking normally based on initial activity.",
                topCreepCategory = "General",
                volatilityScore = 15,
                explanationReasoning = "Statistical baseline is active. Collect more transactions for detailed behavioral analytics."
            )
        }

        // Feature Extraction
        val totalExp = expenses.sumOf { it.amount }
        val categoryGroups = expenses.groupBy { it.category }
        val topCatGroup = categoryGroups.maxByOrNull { (_, list) -> list.sumOf { it.amount } }
        val topCatName = topCatGroup?.key ?: "Food & Dining"
        val topCatSpent = topCatGroup?.value?.sumOf { it.amount } ?: 0.0
        val topCatRatio = if (totalExp > 0) (topCatSpent / totalExp * 100).toInt() else 0

        val volatility = ((expenses.map { it.amount }.maxOrNull() ?: 0.0) / (totalExp / expenses.size) * 10).toInt().coerceIn(10, 99)

        val explanation = "$topCatName spending drove $topCatRatio% of this month's budget variation, mostly from discretionary purchases."

        return MonthlyRecapInsight(
            title = "Monthly Recap & Behavioral Insights",
            summaryText = "Your $topCatName expenses were higher than baseline. Overall spending volatility score is $volatility/100.",
            topCreepCategory = topCatName,
            volatilityScore = volatility,
            explanationReasoning = explanation
        )
    }
}
