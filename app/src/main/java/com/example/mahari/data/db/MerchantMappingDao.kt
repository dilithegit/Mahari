package com.example.mahari.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MerchantMappingDao {
    @Query("SELECT * FROM merchant_category_mappings WHERE merchantPattern = :merchant LIMIT 1")
    suspend fun getMappingForMerchant(merchant: String): MerchantCategoryMappingEntity?

    @Query("SELECT * FROM merchant_category_mappings")
    fun getAllMappingsFlow(): Flow<List<MerchantCategoryMappingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: MerchantCategoryMappingEntity)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets ORDER BY id DESC LIMIT 1")
    fun getCurrentBudgetFlow(): Flow<BudgetEntity?>

    @Query("SELECT * FROM budgets ORDER BY id DESC LIMIT 1")
    suspend fun getCurrentBudget(): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setBudget(budget: BudgetEntity)
}
