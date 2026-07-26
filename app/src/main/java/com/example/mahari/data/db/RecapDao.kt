package com.example.mahari.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecapDao {
    @Query("SELECT * FROM recap_history ORDER BY monthYear DESC")
    suspend fun getAllRecaps(): List<RecapEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecap(recap: RecapEntity)
}
