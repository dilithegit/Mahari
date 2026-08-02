package com.example.mahari.data.security

import android.content.Context
import java.io.File
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

object SqlCipherMigrationHelper {

    fun prepareEncryptedDatabase(context: Context, securityManager: SecurityManager): SupportFactory {
        SQLiteDatabase.loadLibs(context)
        val passphraseStr = securityManager.getOrCreateDatabasePassphrase()
        val passphraseBytes = passphraseStr.toByteArray(Charsets.UTF_8)

        // First, check if legacy whole-file AES decryption is needed
        try {
            DatabaseFileCipher.decryptDatabaseOnDisk(context, securityManager)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val dbFile = context.getDatabasePath("mahari_database.db")
        if (dbFile.exists() && dbFile.length() > 0L) {
            val headerBytes = try {
                val input = dbFile.inputStream()
                val buf = ByteArray(15)
                val read = input.read(buf)
                input.close()
                if (read >= 15) String(buf, 0, 15) else ""
            } catch (e: Exception) {
                ""
            }

            if (headerBytes == "SQLite format 3") {
                // Perform transparent 1-time migration from plaintext SQLite to SQLCipher encrypted SQLite
                try {
                    val encryptedDbFile = context.getDatabasePath("mahari_database_sqlcipher_temp.db")
                    if (encryptedDbFile.exists()) {
                        encryptedDbFile.delete()
                    }

                    val plainDb = SQLiteDatabase.openOrCreateDatabase(dbFile, "", null)
                    val escapedPath = encryptedDbFile.absolutePath.replace("'", "''")
                    val escapedPassphrase = passphraseStr.replace("'", "''")
                    
                    plainDb.rawExecSQL("ATTACH DATABASE '$escapedPath' AS encrypted KEY '$escapedPassphrase';")
                    plainDb.rawExecSQL("SELECT sqlcipher_export('encrypted');")
                    plainDb.rawExecSQL("DETACH DATABASE encrypted;")
                    plainDb.close()

                    val walFile = context.getDatabasePath("mahari_database.db-wal")
                    val shmFile = context.getDatabasePath("mahari_database.db-shm")
                    if (walFile.exists()) walFile.delete()
                    if (shmFile.exists()) shmFile.delete()

                    dbFile.delete()
                    encryptedDbFile.renameTo(dbFile)
                    android.util.Log.d("SqlCipherMigration", "Successfully migrated database to SQLCipher transparent page encryption.")
                } catch (e: Exception) {
                    android.util.Log.e("SqlCipherMigration", "Error migrating database to SQLCipher", e)
                }
            }
        }

        return SupportFactory(passphraseBytes)
    }
}
