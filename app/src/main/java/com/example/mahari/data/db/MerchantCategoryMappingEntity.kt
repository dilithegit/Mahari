package com.example.mahari.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_mappings")
data class MerchantCategoryMappingEntity(
    @PrimaryKey
    val merchantPattern: String,
    val category: String,
    val updatedAt: Long = System.currentTimeMillis()
)
