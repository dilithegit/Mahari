package com.example.mahari.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
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

        fun getDatabase(context: Context): MahariDatabase {
            return INSTANCE ?: synchronized(this) {
                val sec = SecurityManager(context)
                DatabaseFileCipher.decryptDatabaseOnDisk(context, sec)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahariDatabase::class.java,
                    "mahari_database.db"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }

        fun lockAndEncryptDatabase(context: Context) {
            synchronized(this) {
                val inst = INSTANCE
                if (inst != null && inst.isOpen) {
                    inst.close()
                }
                INSTANCE = null
                val sec = SecurityManager(context)
                DatabaseFileCipher.encryptDatabaseOnDisk(context, sec)
            }
        }
    }
}
