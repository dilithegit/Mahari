package com.example.mahari.data.parser

import android.content.Context
import android.net.Uri
import com.example.mahari.data.categorizer.Categorizer
import com.example.mahari.data.db.MerchantMappingDao
import com.example.mahari.data.db.TransactionDao
import com.example.mahari.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsBackfillManager {

    suspend fun performOneTimeBackfill(
        context: Context,
        transactionDao: TransactionDao,
        mappingDao: MerchantMappingDao? = null,
        onProgress: ((processed: Int, total: Int) -> Unit)? = null
    ): Int = withContext(Dispatchers.IO) {
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")
        
        val cursor = context.contentResolver.query(
            uri,
            projection,
            null,
            null,
            "date DESC"
        ) ?: return@withContext 0

        val entitiesToInsert = mutableListOf<TransactionEntity>()
        var totalMessages = 0

        cursor.use { c ->
            val addressIdx = c.getColumnIndex("address")
            val bodyIdx = c.getColumnIndex("body")
            val dateIdx = c.getColumnIndex("date")

            totalMessages = c.count
            var processed = 0

            while (c.moveToNext()) {
                processed++
                onProgress?.invoke(processed, totalMessages)

                val address = if (addressIdx != -1) c.getString(addressIdx) ?: "" else ""
                val body = if (bodyIdx != -1) c.getString(bodyIdx) ?: "" else ""
                val date = if (dateIdx != -1) c.getLong(dateIdx) else System.currentTimeMillis()

                // Filter for M-Pesa SMS notifications
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

        entitiesToInsert.size
    }
}
