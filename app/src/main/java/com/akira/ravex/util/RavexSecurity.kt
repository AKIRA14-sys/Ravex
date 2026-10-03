package com.akira.ravex.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object RavexSecurity {
    private const val PREF_FILE = "ravex_secure_keys"

    private fun getSecurePrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREF_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences("ravex_secure_keys_fallback", Context.MODE_PRIVATE)
        }
    }

    fun saveApiKey(context: Context, providerId: String, apiKey: String) {
        getSecurePrefs(context).edit().putString("key_$providerId", apiKey).apply()
    }

    fun getApiKey(context: Context, providerId: String): String {
        return getSecurePrefs(context).getString("key_$providerId", "") ?: ""
    }

    fun clearApiKey(context: Context, providerId: String) {
        getSecurePrefs(context).edit().remove("key_$providerId").apply()
    }

    fun maskApiKey(key: String): String {
        if (key.isBlank()) return "Not Configured"
        if (key.length <= 8) return "••••••••"
        return key.take(4) + "••••••••" + key.takeLast(4)
    }
}
