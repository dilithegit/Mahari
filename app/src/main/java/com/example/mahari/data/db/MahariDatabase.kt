package com.example.mahari.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

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
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MahariDatabase::class.java,
                    "mahari_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
