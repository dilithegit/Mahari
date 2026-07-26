package com.example.mahari.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recap_history")
data class RecapEntity(
    @PrimaryKey
    val monthYear: String, // e.g. "2026-06"
    val headlineInsight: String,
    val totalSpend: Double,
    val topCategory: String,
    val shapExplanationsJson: String,
    val timestamp: Long
)
