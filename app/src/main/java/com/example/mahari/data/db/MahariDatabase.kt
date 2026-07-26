package com.example.mahari.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
    version = 3,
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

        fun getDatabase(context: Context): MahariDatabase {
            return INSTANCE ?: synchronized(this) {
                val sec = SecurityManager(context)
                DatabaseFileCipher.decryptDatabaseOnDisk(context, sec)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahariDatabase::class.java,
                    "mahari_database.db"
                )
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
