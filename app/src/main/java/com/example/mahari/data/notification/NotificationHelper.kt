package com.example.mahari.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.example.mahari.MainActivity
import com.example.mahari.R
import com.example.mahari.data.budget.BudgetAlertLevel
import com.example.mahari.data.budget.BudgetEvaluation

object NotificationHelper {
    private const val CHANNEL_TRANSACTIONS = "mahari_transactions"
    private const val CHANNEL_BUDGET = "mahari_budget"
    private const val CHANNEL_RECAP = "mahari_recap"

    private fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val txChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "M-Pesa Transaction Pings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Real-time alerts for incoming & outgoing payments" }

            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget Alerts & Warnings",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Alerts when spending approaches daily budget limits" }

            val recapChannel = NotificationChannel(
                CHANNEL_RECAP,
                "Monthly Insights & Recaps",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for newly generated monthly financial insights" }

            notificationManager.createNotificationChannels(listOf(txChannel, budgetChannel, recapChannel))
        }
    }

    private fun createPendingIntent(context: Context, route: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("target_route", route)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(intent)
            getPendingIntent(
                route.hashCode(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )!!
        }
    }

    fun showRealtimeTransactionNotification(
        context: Context,
        amount: Double,
        party: String,
        isExpense: Boolean
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = if (!isExpense) "💰 Money Received" else "💸 Payment Sent"
        val text = if (!isExpense) {
            "You received Ksh ${"%.2f".format(amount)} from $party"
        } else {
            "You sent Ksh ${"%.2f".format(amount)} to $party"
        }

        val pendingIntent = createPendingIntent(context, "merchant/$party")

        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showBudgetAlertNotification(
        context: Context,
        evaluation: BudgetEvaluation,
        latestMerchant: String,
        latestAmount: Double
    ) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val cleanMerchant = if (latestMerchant.length > 25) {
            latestMerchant.take(25) + "..."
        } else {
            latestMerchant
        }

        val (title, text) = when (evaluation.alertLevel) {
            BudgetAlertLevel.OVER_BUDGET_100 -> Pair(
                "🚨 Daily Budget Exceeded!",
                "Ksh ${"%.2f".format(latestAmount)} at $cleanMerchant pushed today's spend to Ksh ${"%.2f".format(evaluation.todaySpend)} (Limit: Ksh ${"%.2f".format(evaluation.dailyLimit)})."
            )
            BudgetAlertLevel.WARNING_95 -> Pair(
                "⚠️ 95% Daily Budget Reached",
                "Ksh ${"%.2f".format(latestAmount)} at $cleanMerchant. Remaining budget is only Ksh ${"%.2f".format(evaluation.remainingBudget)}."
            )
            BudgetAlertLevel.WARNING_80 -> Pair(
                "💡 80% Daily Budget Notice",
                "Spent Ksh ${"%.2f".format(evaluation.todaySpend)} of today's Ksh ${"%.2f".format(evaluation.dailyLimit)} limit."
            )
            BudgetAlertLevel.NORMAL -> return
        }

        val pendingIntent = createPendingIntent(context, "dashboard")

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun showRecapReadyNotification(context: Context, monthYear: String) {
        createNotificationChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = "📊 Monthly Insight Ready"
        val text = "Your financial recap for $monthYear has been generated. Tap to view insights."

        val pendingIntent = createPendingIntent(context, "insights")

        val notification = NotificationCompat.Builder(context, CHANNEL_RECAP)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(monthYear.hashCode(), notification)
    }
}
