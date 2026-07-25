package com.example.mahari.di

import android.content.Context
import com.example.mahari.data.db.MahariDatabase
import com.example.mahari.data.repository.TransactionRepository

class AppContainer(private val context: Context) {
    val database: MahariDatabase by lazy {
        MahariDatabase.getDatabase(context)
    }

    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(
            transactionDao = database.transactionDao(),
            merchantMappingDao = database.merchantMappingDao(),
            budgetDao = database.budgetDao()
        )
    }
}
