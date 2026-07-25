package com.example.mahari.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.mahari.data.budget.BudgetAlertLevel
import com.example.mahari.data.budget.BudgetEvaluation

object NotificationHelper {
    private const val CHANNEL_ID = "mahari_budget_alerts"
    private const val CHANNEL_NAME = "Budget Threshold Alerts"

    fun showBudgetAlertNotification(context: Context, evaluation: BudgetEvaluation, latestMerchant: String, latestAmount: Double) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when M-Pesa spending crosses daily budget thresholds"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val (title, text) = when (evaluation.alertLevel) {
            BudgetAlertLevel.OVER_BUDGET_100 -> Pair(
                "🚨 Daily Budget Exceeded!",
                "Ksh ${"%.2f".format(latestAmount)} at $latestMerchant pushed today's spend to Ksh ${"%.2f".format(evaluation.todaySpend)} (Limit: Ksh ${"%.2f".format(evaluation.dailyLimit)})."
            )
            BudgetAlertLevel.WARNING_95 -> Pair(
                "⚠️ 95% Daily Budget Reached",
                "Ksh ${"%.2f".format(latestAmount)} at $latestMerchant. Remaining budget is only Ksh ${"%.2f".format(evaluation.remainingBudget)}."
            )
            BudgetAlertLevel.WARNING_80 -> Pair(
                "💡 80% Daily Budget Notice",
                "You've spent Ksh ${"%.2f".format(evaluation.todaySpend)} of today's Ksh ${"%.2f".format(evaluation.dailyLimit)} limit."
            )
            BudgetAlertLevel.NORMAL -> return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
