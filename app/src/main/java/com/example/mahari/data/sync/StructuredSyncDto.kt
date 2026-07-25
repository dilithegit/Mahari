package com.example.mahari.data.sync

data class StructuredTransactionDto(
    val code: String,
    val amount: Double,
    val category: String,
    val merchant: String,
    val timestamp: Long,
    val isExpense: Boolean
)

data class StructuredSyncRequest(
    val deviceId: String,
    val userAge: Int,
    val transactions: List<StructuredTransactionDto>
)

data class StructuredInsightResponse(
    val topSpendingCategory: String,
    val primaryDriver: String,
    val shapSummary: String,
    val textInsight: String
)
