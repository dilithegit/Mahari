package com.example.mahari.data.sync

import android.content.Context
import com.example.mahari.data.db.TransactionEntity
import com.example.mahari.data.security.SecurityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object CloudSyncManager {

    // Production HTTPS endpoint hosted on Render free tier
    private const val BASE_URL = "https://mahari-backend.onrender.com"

    suspend fun syncStructuredTransactions(
        context: Context,
        securityManager: SecurityManager,
        transactions: List<TransactionEntity>
    ): StructuredInsightResponse? = withContext(Dispatchers.IO) {
        if (!securityManager.isCloudSyncEnabled()) {
            return@withContext null
        }

        val deviceId = securityManager.getOrCreateDeviceId()
        val userAge = securityManager.getUserAge()

        val jsonArray = JSONArray()
        for (tx in transactions) {
            val txObj = JSONObject().apply {
                put("code", tx.code)
                put("amount", tx.amount)
                put("category", tx.category)
                put("merchant", tx.merchantOrParty)
                put("timestamp", tx.timestamp)
                put("isExpense", tx.isExpense)
                // RAW SMS TEXT IS EXPLICITLY OMITTED FOR PRIVACY MINIMIZATION
            }
            jsonArray.put(txObj)
        }

        val requestBody = JSONObject().apply {
            put("deviceId", deviceId)
            put("userAge", userAge)
            put("transactions", jsonArray)
        }

        try {
            val url = URL("$BASE_URL/api/v1/sync")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(requestBody.toString())
                writer.flush()
            }

            if (conn.responseCode == 200) {
                val responseStr = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonObj = JSONObject(responseStr)
                return@withContext StructuredInsightResponse(
                    topSpendingCategory = jsonObj.optString("topSpendingCategory", "General"),
                    primaryDriver = jsonObj.optString("primaryDriver", ""),
                    shapSummary = jsonObj.optString("shapSummary", ""),
                    textInsight = jsonObj.optString("textInsight", "")
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        null
    }

    suspend fun purgeCloudData(
        securityManager: SecurityManager
    ): Boolean = withContext(Dispatchers.IO) {
        val deviceId = securityManager.getOrCreateDeviceId()
        try {
            val url = URL("$BASE_URL/api/v1/user-data/$deviceId")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                setRequestProperty("Accept", "application/json")
                connectTimeout = 5000
                readTimeout = 5000
            }

            return@withContext conn.responseCode == 200
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
}
