package com.example.mahari.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mahari.data.parser.MpesaParser
import com.example.mahari.data.security.DatabaseFileCipher
import com.example.mahari.data.security.SecurityManager

@Database(
    entities = [
        TransactionEntity::class,
        MerchantCategoryMappingEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        RecapEntity::class,
        CategoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class MahariDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantMappingDao(): MerchantMappingDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recapDao(): RecapDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: MahariDatabase? = null

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recap_history ADD COLUMN financialScore REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE recap_history ADD COLUMN confidenceRating REAL DEFAULT NULL")
                db.execSQL("ALTER TABLE recap_history ADD COLUMN scoreBreakdownJson TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE recap_history ADD COLUMN isCloudEnhanced INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions(timestamp)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_merchantOrParty ON transactions(merchantOrParty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_transactions_category ON transactions(category)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Re-parse every existing transaction's rawText against broadened MpesaParser regex
                val cursor = db.query("SELECT code, rawText FROM transactions")
                val updates = mutableListOf<Pair<String, Double?>>()
                cursor.use { c ->
                    val codeIdx = c.getColumnIndex("code")
                    val rawTextIdx = c.getColumnIndex("rawText")
                    if (codeIdx != -1 && rawTextIdx != -1) {
                        while (c.moveToNext()) {
                            val code = c.getString(codeIdx)
                            val rawText = c.getString(rawTextIdx) ?: ""
                            val parsed = MpesaParser.parse(rawText)
                            val newBalance = parsed?.runningBalance
                            updates.add(Pair(code, newBalance))
                        }
                    }
                }
                var updatedRealCount = 0
                var updatedNullCount = 0
                for ((code, newBal) in updates) {
                    if (newBal != null) {
                        db.execSQL("UPDATE transactions SET runningBalance = ? WHERE code = ?", arrayOf(newBal, code))
                        updatedRealCount++
                    } else {
                        db.execSQL("UPDATE transactions SET runningBalance = NULL WHERE code = ?", arrayOf(code))
                        updatedNullCount++
                    }
                }
                android.util.Log.d("MahariDatabase", "MIGRATION_4_5 complete: $updatedRealCount rows with balance, $updatedNullCount rows set to null.")
            }
        }

        fun getDatabase(context: Context): MahariDatabase {
            return INSTANCE ?: synchronized(this) {
                val sec = SecurityManager(context)
                val factory = com.example.mahari.data.security.SqlCipherMigrationHelper.prepareEncryptedDatabase(context, sec)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahariDatabase::class.java,
                    "mahari_database.db"
                )
                    .openHelperFactory(factory)
                    .addMigrations(MIGRATION_3_4, MIGRATION_4_5)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
