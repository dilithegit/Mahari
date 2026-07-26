package com.example.mahari.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,
    val iconEmoji: String,
    val colorHex: String
)
