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
            "DOMINOS", "SUBWAY", "CHICKEN", "CHOMA", "CORNER", "HOTEL", "STEAK",
            "SWEET", "ICECREAM", "CREAMERY", "GALITOS", "ARTCAFFE", "BAKEHOUSE"
        ),
        "Groceries" to listOf(
            "SUPERMARKET", "MART", "GROCER", "STORE", "WHOLESALE", "RETAIL",
            "MINIMARKET", "PRODUCE", "BUTCHERY", "VEG", "NAIVAS", "CARREFOUR",
            "QUICKMART", "CHANDARANA", "MARKET", "BAZAAR", "SHOP", "DUKA", "ENTERPRISES"
        ),
        "Transport" to listOf(
            "BOLT", "UBER", "CAB", "TAXI", "RIDE", "TRANSIT", "MATATU", "SHUTTLE",
            "PETROL", "SHELL", "TOTAL", "RUBIS", "GAS", "PARKING", "TOLL", "EXPRESSWAY",
            "FILLING", "OIL", "STATION", "SERVICE STATION", "OLA", "ENGEN"
        ),
        "Utilities" to listOf(
            "KPLC", "POWER", "WATER", "ELECTRICITY", "INTERNET", "WIFI", "BROADBAND",
            "TV", "ZUKU", "SAFARICOM HOME", "DSTV", "GOTV", "UTILITIES", "PAYBILL",
            "TOKEN", "NCWSC", "KRA", "GOVERNMENT", "COUNTY", "REVENUE"
        ),
        "Shopping & Services" to listOf(
            "CLOTHING", "SHIRT", "SHOES", "BOUTIQUE", "APPAREL", "ELECTRONICS", "PHONE",
            "ACCESSORIES", "MALL", "SALON", "BARBER", "SPA", "LAUNDRY", "CLEANING",
            "CAR WASH", "GARAGE", "HARDWARE", "FURNITURE", "TAILOR", "FASHION"
        ),
        "Education" to listOf(
            "UNIVERSITY", "COLLEGE", "CAMPUS", "SCHOOL", "TUITION", "ACADEMY",
            "HOSTEL", "LIBRARY", "STUDY", "BOOKSHOP", "STATIONERY", "STRATHMORE"
        ),
        "Health & Pharmacy" to listOf(
            "CHEMIST", "PHARMACY", "HOSPITAL", "CLINIC", "MEDICAL", "LAB",
            "DENTAL", "HEALTH", "PHARMA", "DRUGSTORE", "OPTICAL"
        ),
        "Financial & Transfers" to listOf(
            "EQUITY", "KCB", "COOP", "CO-OP", "ABSA", "NCBA", "STANBIC", "DTB",
            "FAMILY BANK", "BANK", "SACCO", "M-SHWARI", "KCB M-PESA", "PAYROLL"
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
