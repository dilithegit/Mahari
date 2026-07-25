package com.example.mahari.data.categorizer

import com.example.mahari.data.db.MerchantMappingDao
import com.example.mahari.data.parser.MpesaTransactionType

object Categorizer {

    // Dynamic Keyword Mapping Matrix
    private val categoryKeywordMatrix = mapOf(
        "Food & Dining" to listOf(
            "CAFETERIA", "CANTEEN", "RESTAURANT", "CAFE", "BISTRO", "EATERY", "DINER",
            "KITCHEN", "FOOD", "GRILL", "BAKERY", "COFFEE", "PIZZA", "BURGER", "BAR",
            "PUB", "LOUNGE", "ROAST", "DELI", "MESS", "CATERING", "JAVA", "KFC",
            "DOMINOS", "SUBWAY", "CHICKEN", "CHOMA", "CORNER", "HOTEL", "STEAK"
        ),
        "Groceries" to listOf(
            "SUPERMARKET", "MART", "GROCER", "STORE", "WHOLESALE", "RETAIL",
            "MINIMARKET", "PRODUCE", "BUTCHERY", "VEG", "NAIVAS", "CARREFOUR",
            "QUICKMART", "CHANDARANA", "MARKET", "BAZAAR"
        ),
        "Transport" to listOf(
            "BOLT", "UBER", "CAB", "TAXI", "RIDE", "TRANSIT", "MATATU", "SHUTTLE",
            "PETROL", "SHELL", "TOTAL", "RUBIS", "GAS", "PARKING", "TOLL", "EXPRESSWAY",
            "FILLING", "OIL"
        ),
        "Utilities" to listOf(
            "KPLC", "POWER", "WATER", "ELECTRICITY", "INTERNET", "WIFI", "BROADBAND",
            "TV", "ZUKU", "SAFARICOM HOME", "DSTV", "GOTV", "UTILITIES"
        ),
        "Education" to listOf(
            "UNIVERSITY", "COLLEGE", "CAMPUS", "SCHOOL", "TUITION", "ACADEMY",
            "HOSTEL", "LIBRARY", "STUDY", "BOOKSHOP", "STATIONERY"
        ),
        "Health & Pharmacy" to listOf(
            "CHEMIST", "PHARMACY", "HOSPITAL", "CLINIC", "MEDICAL", "LAB",
            "DENTAL", "HEALTH", "PHARMA"
        )
    )

    suspend fun categorize(
        merchant: String,
        type: MpesaTransactionType,
        mappingDao: MerchantMappingDao? = null
    ): String {
        val upperMerchant = merchant.uppercase().trim()

        // 1. Priority Tier: User Learning Memory (Room DB persistent overrides)
        mappingDao?.getMappingForMerchant(upperMerchant)?.let {
            return it.category
        }

        // 2. Priority Tier: Transaction Type Rules
        when (type) {
            MpesaTransactionType.RECEIVE_MONEY, MpesaTransactionType.DEPOSIT -> return "Income"
            MpesaTransactionType.FULIZA_DEBT -> return "Debt & Overdraft"
            MpesaTransactionType.AIRTIME -> return "Airtime & Data"
            else -> {}
        }

        // 3. Priority Tier: Dynamic Keyword & Token Matcher
        // Matches uni cafeterias ("KU Main Cafeteria", "Strathmore Canteen", "Campus Diner", etc.)
        for ((category, keywords) in categoryKeywordMatrix) {
            for (keyword in keywords) {
                if (upperMerchant.contains(keyword)) {
                    return category
                }
            }
        }

        // 4. Default Fallback
        return "Other"
    }
}
