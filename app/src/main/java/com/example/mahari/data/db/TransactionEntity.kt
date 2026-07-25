package com.example.mahari.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val code: String,
    val amount: Double,
    val merchantOrParty: String,
    val category: String,
    val type: String,
    val runningBalance: Double,
    val timestamp: Long,
    val rawText: String,
    val isExpense: Boolean,
    val fulizaOutstanding: Double = 0.0,
    val isRecurring: Boolean = false,
    val isAnomaly: Boolean = false
)
