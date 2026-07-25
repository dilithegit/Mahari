package com.example.mahari.data.export

import com.example.mahari.data.db.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {

    fun exportToCsv(transactions: List<TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("TransactionCode,Date,MerchantOrParty,Category,Type,Amount,RunningBalance,IsExpense\n")

        for (tx in transactions) {
            val dateStr = dateFormat.format(Date(tx.timestamp))
            val merchantSanitized = tx.merchantOrParty.replace(",", " ")
            sb.append("${tx.code},$dateStr,$merchantSanitized,${tx.category},${tx.type},${tx.amount},${tx.runningBalance},${tx.isExpense}\n")
        }

        return sb.toString()
    }
}
