package com.example.mahari.data.nlp

data class StructuredQuery(
    val merchant: String? = null,
    val category: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null,
    val isExpenseOnly: Boolean = false
)

object NlpSearchEngine {
    fun parseQuery(userText: String): StructuredQuery {
        val clean = userText.trim().lowercase()

        var merchant: String? = null
        var category: String? = null

        val merchants = listOf("java house", "naivas", "kplc", "bolt", "carrefour", "zuku", "safaricom", "artcafe")
        for (m in merchants) {
            if (clean.contains(m)) {
                merchant = m
                break
            }
        }

        val categories = listOf("food", "groceries", "utilities", "transport", "airtime", "income")
        for (c in categories) {
            if (clean.contains(c)) {
                category = c
                break
            }
        }

        return StructuredQuery(
            merchant = merchant,
            category = category,
            isExpenseOnly = clean.contains("spend") || clean.contains("spent") || clean.contains("paid")
        )
    }
}
