package com.example.mahari.data.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.mahari.data.categorizer.Categorizer
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.parser.MpesaParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress ?: ""
                val body = sms.messageBody ?: ""

                // Filter to M-Pesa / Safaricom senders
                if (sender.contains("MPESA", ignoreCase = true) || sender.contains("Safaricom", ignoreCase = true) || body.contains("M-PESA", ignoreCase = true)) {
                    val parsed = MpesaParser.parse(body) ?: continue

                    CoroutineScope(Dispatchers.IO).launch {
                        val db = MahariDatabase.getDatabase(context)

                        // Check de-duplication
                        if (db.transactionDao().getTransactionByCode(parsed.code) != null) {
                            return@launch
                        }

                        val category = Categorizer.categorize(
                            merchant = parsed.merchantOrParty,
                            type = parsed.type,
                            mappingDao = db.merchantMappingDao()
                        )

                        val entity = TransactionEntity(
                            code = parsed.code,
                            amount = parsed.amount,
                            merchantOrParty = parsed.merchantOrParty,
                            category = category,
                            type = parsed.type.name,
                            runningBalance = parsed.runningBalance,
                            timestamp = parsed.timestamp,
                            rawText = parsed.rawText,
                            isExpense = parsed.isExpense,
                            fulizaOutstanding = parsed.fulizaOutstanding
                        )

                        db.transactionDao().insertTransaction(entity)

                        com.example.mahari.data.notification.NotificationHelper.showRealtimeTransactionNotification(
                            context = context,
                            amount = parsed.amount,
                            party = parsed.merchantOrParty,
                            isExpense = parsed.isExpense
                        )

                        if (parsed.isExpense) {
                            try {
                                val budget = db.budgetDao().getCurrentBudget()
                                if (budget != null) {
                                    val nowCal = java.util.Calendar.getInstance().apply {
                                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                                        set(java.util.Calendar.MINUTE, 0)
                                        set(java.util.Calendar.SECOND, 0)
                                        set(java.util.Calendar.MILLISECOND, 0)
                                    }
                                    val startOfToday = nowCal.timeInMillis
                                    val todaySpent = db.transactionDao().getAllTransactionsSync()
                                        .filter { it.timestamp >= startOfToday && it.isExpense }
                                        .sumOf { it.amount }
                                    val eval = com.example.mahari.data.budget.BudgetEngine.evaluate(todaySpent, budget.dailyLimit)
                                    if (eval.alertLevel != com.example.mahari.data.budget.BudgetAlertLevel.NORMAL) {
                                        com.example.mahari.data.notification.NotificationHelper.showBudgetAlertNotification(
                                            context = context,
                                            evaluation = eval,
                                            latestMerchant = parsed.merchantOrParty,
                                            latestAmount = parsed.amount
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
            }
        }
    }
}
