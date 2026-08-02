package com.example.mahari.data.migration

import android.content.Context
import android.util.Log
import com.example.mahari.data.db.TransactionDao
import com.example.mahari.data.parser.MpesaParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TransactionMigrationManager {

    private const val PREFS_NAME = "mahari_migration_prefs"
    private const val KEY_MIGRATION_V2 = "date_migration_v2_recorrect"

    suspend fun runOneTimeDateMigration(context: Context, transactionDao: TransactionDao): Int = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_MIGRATION_V2, false)) {
            return@withContext 0
        }

        try {
            val allTransactions = transactionDao.getAllTransactionsSync()
            var correctedCount = 0

            for (tx in allTransactions) {
                if (tx.rawText.isNotEmpty()) {
                    val extractedTs = MpesaParser.extractTransactionTimestamp(tx.rawText, tx.timestamp)
                    if (extractedTs != tx.timestamp) {
                        val updatedTx = tx.copy(timestamp = extractedTs)
                        transactionDao.insertTransaction(updatedTx)
                        correctedCount++
                    }
                }
            }

            prefs.edit().putBoolean(KEY_MIGRATION_V2, true).apply()
            Log.d("MahariMigration", "One-time date migration V2 complete. Corrected $correctedCount out of ${allTransactions.size} transactions.")
            return@withContext correctedCount
        } catch (e: Exception) {
            Log.e("MahariMigration", "Error executing date migration V2. Marking migration complete to prevent partial re-runs.", e)
            prefs.edit().putBoolean(KEY_MIGRATION_V2, true).apply()
            return@withContext 0
        }
    }
}
