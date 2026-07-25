package com.example.mahari.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "mahari_encrypted_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback to private preferences if MasterKey is unsupported
        context.getSharedPreferences("mahari_security_prefs", Context.MODE_PRIVATE)
    }

    fun isOnboardingComplete(): Boolean = prefs.getBoolean("onboarding_complete", false)

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit().putBoolean("onboarding_complete", complete).apply()
    }

    fun getUserName(): String = prefs.getString("user_name", "") ?: ""

    fun getUserAge(): Int = prefs.getInt("user_age", 0)

    fun saveUserProfile(name: String, age: Int) {
        prefs.edit()
            .putString("user_name", name.trim())
            .putInt("user_age", age)
            .apply()
    }

    fun getMonthlyBudget(): Double = prefs.getFloat("monthly_budget", 30000.0f).toDouble()

    fun setMonthlyBudget(limit: Double) {
        prefs.edit().putFloat("monthly_budget", limit.toFloat()).apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun getPin(): String? = prefs.getString("user_pin", null)

    fun setPin(pin: String?) {
        if (pin == null) {
            prefs.edit().remove("user_pin").apply()
        } else {
            prefs.edit().putString("user_pin", pin).apply()
        }
    }
}
