package com.medha.app.ai

import android.content.Context
import com.medha.app.utils.EncryptionUtil

/**
 * Owns the lifecycle of the user's Gemini API key:
 *  - validates the key format,
 *  - tests it against the live API,
 *  - stores it AES-256 encrypted locally (and exposes the ciphertext so the
 *    Firebase layer can mirror it as a backup).
 */
class ApiKeyManager(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("medha_secure", Context.MODE_PRIVATE)
    private val gemini = GeminiClient()

    /** Basic shape check for Google AI Studio keys (begin with "AIza"). */
    fun isValidFormat(key: String): Boolean {
        val trimmed = key.trim()
        return trimmed.length in 30..80 && trimmed.startsWith("AIza")
    }

    /** Confirms the key actually works by issuing a tiny request. */
    suspend fun testKey(key: String): Boolean = gemini.validateKey(key.trim())

    /** Encrypts and persists the plaintext key locally. */
    fun saveKey(plainKey: String) {
        val encrypted = EncryptionUtil.encrypt(plainKey.trim())
        sp.edit().putString(KEY, encrypted).apply()
    }

    /** Returns the decrypted key, or null if none is stored. */
    fun getKey(): String? {
        val encrypted = sp.getString(KEY, null) ?: return null
        return EncryptionUtil.decrypt(encrypted)
    }

    fun hasKey(): Boolean = sp.contains(KEY)

    /** Raw ciphertext, used to back up to Firestore. */
    fun getEncryptedBlob(): String? = sp.getString(KEY, null)

    /** Restores ciphertext fetched from Firestore (device-bound; may fail to decrypt elsewhere). */
    fun restoreEncryptedBlob(blob: String) {
        sp.edit().putString(KEY, blob).apply()
    }

    fun clear() {
        sp.edit().remove(KEY).apply()
    }

    /** Masks the key for display, e.g. "AIza••••••wXyz". */
    fun maskedKey(): String {
        val key = getKey() ?: return "—"
        if (key.length <= 8) return "••••"
        return key.take(4) + "••••••" + key.takeLast(4)
    }

    companion object {
        private const val KEY = "gemini_api_key"
    }
}
