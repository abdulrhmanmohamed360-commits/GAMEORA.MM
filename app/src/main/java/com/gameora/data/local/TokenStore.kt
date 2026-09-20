package com.gameora.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Stores the auth token(s) in [EncryptedSharedPreferences] (Android Keystore-backed).
 * The token never lives in plain SharedPreferences, resources or source code.
 */
class TokenStore(context: Context) {

    private val prefs = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Should not happen on API 26+, but keep the app resilient.
        context.getSharedPreferences(FALLBACK_FILE, Context.MODE_PRIVATE)
    }

    fun saveTokens(accessToken: String?, refreshToken: String? = null) {
        prefs.edit().apply {
            if (accessToken != null) putString(KEY_ACCESS, accessToken)
            if (refreshToken != null) putString(KEY_REFRESH, refreshToken)
        }.apply()
    }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)
    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)
    val hasToken: Boolean get() = !accessToken.isNullOrBlank()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val FILE_NAME = "gameora_secure_prefs"
        private const val FALLBACK_FILE = "gameora_secure_prefs_fallback"
        private const val KEY_ACCESS = "access_token"
        private const val KEY_REFRESH = "refresh_token"
    }
}
