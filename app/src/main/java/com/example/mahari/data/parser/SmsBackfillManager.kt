package com.example.mahari.data.parser

import android.content.Context
import android.net.Uri
import com.example.mahari.data.categorizer.Categorizer
import com.example.mahari.data.db.MerchantMappingDao
import com.example.mahari.data.db.TransactionDao
import com.example.mahari.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackfillResult(
    val importedCount: Int,
    val suggestedMonthlyBudget: Double
)

object SmsBackfillManager {

    suspend fun performOneTimeBackfill(
        context: Context,
        transactionDao: TransactionDao,
        mappingDao: MerchantMappingDao? = null,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): BackfillResult = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        val entitiesToInsert = mutableListOf<TransactionEntity>()

        try {
            val cursor = appContext.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            ) ?: return@withContext BackfillResult(0, 30000.0)

            cursor.use { c ->
                val addressIdx = c.getColumnIndex("address")
                val bodyIdx = c.getColumnIndex("body")
                val dateIdx = c.getColumnIndex("date")

                val totalMessages = c.count
                var processed = 0

                while (c.moveToNext()) {
                    processed++
                    onProgress?.invoke(processed, totalMessages)

                    val address = if (addressIdx != -1) c.getString(addressIdx) ?: "" else ""
                    val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                    val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                    val isMpesaAddress = address.contains("MPESA", ignoreCase = true) ||
                            address.contains("M-PESA", ignoreCase = true) ||
                            address.contains("Safaricom", ignoreCase = true)

                    val isMpesaContent = body.contains("Confirmed.", ignoreCase = true) ||
                            body.contains("M-PESA balance", ignoreCase = true) ||
                            body.contains("Ksh", ignoreCase = true)

                    if (isMpesaAddress || isMpesaContent) {
                        val parsed = MpesaParser.parse(body)
                        if (parsed != null) {
                            val category = Categorizer.categorize(parsed.merchantOrParty, parsed.type, mappingDao)
                            val timestamp = if (parsed.timestamp > 0) parsed.timestamp else date
                            val entity = TransactionEntity(
                                code = parsed.code,
                                amount = parsed.amount,
                                merchantOrParty = parsed.merchantOrParty,
                                type = parsed.type.name,
                                category = category,
                                timestamp = timestamp,
                                runningBalance = parsed.runningBalance,
                                isExpense = parsed.isExpense,
                                rawText = parsed.rawText
                            )
                            entitiesToInsert.add(entity)
                        }
                    }
                }
            }

            if (entitiesToInsert.isNotEmpty()) {
                transactionDao.insertTransactionsIgnore(entitiesToInsert)
            }

            val expenseTx = entitiesToInsert.filter { it.isExpense }
            val totalExpenseSum = expenseTx.sumOf { it.amount }
            val suggestedBudget = if (expenseTx.isNotEmpty()) {
                val minDate = expenseTx.minOf { it.timestamp }
                val maxDate = expenseTx.maxOf { it.timestamp }
                val daysDiff = ((maxDate - minDate) / (1000 * 60 * 60 * 24)).coerceAtLeast(1)
                val monthsDiff = (daysDiff / 30.0).coerceAtLeast(1.0)
                val avgMonthly = totalExpenseSum / monthsDiff
                (kotlin.math.round(avgMonthly / 500.0) * 500.0).coerceAtLeast(5000.0)
            } else {
                30000.0
            }

            BackfillResult(entitiesToInsert.size, suggestedBudget)
        } catch (e: Exception) {
            e.printStackTrace()
            BackfillResult(0, 30000.0)
        }
    }
}
