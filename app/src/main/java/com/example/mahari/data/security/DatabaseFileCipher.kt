package com.example.mahari.data.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object DatabaseFileCipher {
    private const val AES_KEY_SIZE = 256
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128

    private fun getSecretKey(securityManager: SecurityManager): SecretKey {
        val rawKeyString = securityManager.getOrCreateDatabasePassphrase()
        val keyBytes = rawKeyString.toByteArray(Charsets.UTF_8).copyOf(32) // 256 bits
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encryptDatabaseOnDisk(context: Context, securityManager: SecurityManager): Boolean {
        val dbFile = context.getDatabasePath("mahari_database.db")
        if (!dbFile.exists() || dbFile.length() == 0L) return false

        val bytes = dbFile.readBytes()
        if (bytes.size >= 16 && String(bytes.copyOfRange(0, 15)) != "SQLite format 3") {
            // Already encrypted on disk
            return true
        }

        try {
            val secretKey = getSecretKey(securityManager)
            val iv = ByteArray(GCM_IV_LENGTH)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val encryptedBytes = cipher.doFinal(bytes)
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            dbFile.writeBytes(combined)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    fun decryptDatabaseOnDisk(context: Context, securityManager: SecurityManager): Boolean {
        val dbFile = context.getDatabasePath("mahari_database.db")
        if (!dbFile.exists() || dbFile.length() == 0L) return false

        val combined = dbFile.readBytes()
        if (combined.size >= 16 && String(combined.copyOfRange(0, 15)) == "SQLite format 3") {
            // Already plain SQLite format
            return true
        }

        try {
            val secretKey = getSecretKey(securityManager)
            if (combined.size <= GCM_IV_LENGTH) return false

            val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encryptedBytes = combined.copyOfRange(GCM_IV_LENGTH, combined.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))

            val decryptedBytes = cipher.doFinal(encryptedBytes)
            dbFile.writeBytes(decryptedBytes)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
}
