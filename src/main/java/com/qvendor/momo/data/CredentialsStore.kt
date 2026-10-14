package com.qvendor.momo.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialsStore(context: Context) {

    private val masterKey: MasterKey = MasterKey.Builder(context.applicationContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context.applicationContext,
        FILE_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    val isRegistered: Boolean
        get() = prefs.contains(KEY_AGENT_CODE) && prefs.contains(KEY_PIN_HASH)

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun register(agentCode: String, pin: String): Boolean {
        val sanitized = sanitizeAgentCode(agentCode)
        if (sanitized.isBlank()) return false
        if (!isValidPin(pin)) return false

        prefs.edit()
            .putString(KEY_AGENT_CODE, sanitized)
            .putString(KEY_PIN_HASH, hashPin(sanitized, pin))
            .putBoolean(KEY_LOGGED_IN, true)
            .apply()
        return true
    }

    fun login(agentCode: String, pin: String): Boolean {
        val sanitized = sanitizeAgentCode(agentCode)
        if (sanitized.isBlank() || !isValidPin(pin)) return false

        val storedCode = prefs.getString(KEY_AGENT_CODE, null) ?: return false
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false

        if (storedCode != sanitized) return false
        if (storedHash != hashPin(sanitized, pin)) return false

        prefs.edit().putBoolean(KEY_LOGGED_IN, true).apply()
        return true
    }

    fun logout() {
        prefs.edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun storedAgentCode(): String? = prefs.getString(KEY_AGENT_CODE, null)

    companion object {
        private const val FILE_NAME = "qvendor_secure_prefs"
        private const val KEY_AGENT_CODE = "agent_code"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_LOGGED_IN = "logged_in"

        fun sanitizeAgentCode(raw: String): String =
            raw.filter { it.isDigit() }.trim()

        fun isValidPhone(agentCode: String): Boolean {
            val digits = sanitizeAgentCode(agentCode)
            return digits.length in 9..15
        }

        fun isValidPin(pin: String): Boolean =
            pin.length == 6 && pin.all { it.isDigit() }

        fun hashPin(agentCode: String, pin: String): String {
            val salted = "$agentCode|qvendor|$pin"
            val digest = java.security.MessageDigest.getInstance("SHA-256")
                .digest(salted.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
