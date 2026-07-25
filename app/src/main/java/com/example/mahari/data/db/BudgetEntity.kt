package com.example.mahari.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey
    val id: Int = 1,
    val monthlyLimit: Double,
    val monthYear: String, // e.g. "2026-07"
    val dailyLimit: Double = monthlyLimit / 30.0
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long
)
