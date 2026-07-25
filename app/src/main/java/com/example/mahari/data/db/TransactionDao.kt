package com.example.mahari.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getTransactionsSince(startTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE code = :code LIMIT 1")
    suspend fun getTransactionByCode(code: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTransactionsIgnore(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)


    @Query("UPDATE transactions SET category = :newCategory WHERE merchantOrParty = :merchant")
    suspend fun updateCategoryForMerchant(merchant: String, newCategory: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 1 AND timestamp >= :startTime")
    fun getTotalExpenseSince(startTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE isExpense = 0 AND timestamp >= :startTime")
    fun getTotalIncomeSince(startTime: Long): Flow<Double?>

    @Query("SELECT * FROM transactions WHERE merchantOrParty LIKE '%' || :query || '%' OR rawText LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>
}
