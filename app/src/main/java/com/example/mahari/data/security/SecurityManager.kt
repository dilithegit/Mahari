package com.example.mahari.data.security

import android.content.Context
import android.content.SharedPreferences

class SecurityManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mahari_security_prefs", Context.MODE_PRIVATE)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean("biometric_enabled", false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
    }

    fun getPin(): String? = prefs.getString("user_pin", null)

    fun setPin(pin: String) {
        prefs.edit().putString("user_pin", pin).apply()
    }
}
