package com.example.mahari.data.budget

enum class BudgetAlertLevel {
    NORMAL,
    WARNING_80,
    WARNING_95,
    OVER_BUDGET_100
}

data class BudgetEvaluation(
    val dailyLimit: Double,
    val todaySpend: Double,
    val remainingBudget: Double,
    val percentageUsed: Float,
    val alertLevel: BudgetAlertLevel
)

object BudgetEngine {

    fun evaluate(todaySpend: Double, monthlyLimit: Double): BudgetEvaluation {
        val dailyLimit = if (monthlyLimit > 0) monthlyLimit / 30.0 else 1000.0
        val remaining = dailyLimit - todaySpend
        val percentage = (todaySpend / dailyLimit).toFloat()

        val alertLevel = when {
            percentage >= 1.0f -> BudgetAlertLevel.OVER_BUDGET_100
            percentage >= 0.95f -> BudgetAlertLevel.WARNING_95
            percentage >= 0.80f -> BudgetAlertLevel.WARNING_80
            else -> BudgetAlertLevel.NORMAL
        }

        return BudgetEvaluation(
            dailyLimit = dailyLimit,
            todaySpend = todaySpend,
            remainingBudget = remaining,
            percentageUsed = percentage,
            alertLevel = alertLevel
        )
    }
}
