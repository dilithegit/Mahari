package com.example.mahari.data.parser

enum class MpesaTransactionType {
    PAYBILL_BUY_GOODS,
    SEND_MONEY,
    RECEIVE_MONEY,
    WITHDRAW,
    DEPOSIT,
    AIRTIME,
    FULIZA_DEBT,
    UNKNOWN
}

data class ParsedMpesaTransaction(
    val code: String,
    val amount: Double,
    val merchantOrParty: String,
    val type: MpesaTransactionType,
    val runningBalance: Double?,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String,
    val isExpense: Boolean,
    val fulizaOutstanding: Double = 0.0
)
