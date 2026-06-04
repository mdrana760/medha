package com.medha.app.firebase

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Firestore read/write helpers. All methods degrade gracefully (return
 * null / false) when Firebase isn't configured, so the app remains usable
 * locally without a google-services.json.
 *
 * Document layout:
 *   users/{uid} ............ { profile: {...}, config: {...}, connectedApps: {...} }
 *   users/{uid}/conversations/{contactId} ... { app, contactName, messages[], lastUpdated }
 */
class UserRepository {

    private val db get() = FirebaseManager.firestore
    private val uid: String? get() = FirebaseManager.auth?.currentUser?.uid

    private fun userDoc() = uid?.let { db?.collection("users")?.document(it) }

    suspend fun saveUserProfile(user: FirebaseUser): Boolean {
        val doc = userDoc() ?: return false
        val profile = mapOf(
            "name" to (user.displayName ?: ""),
            "email" to (user.email ?: ""),
            "photoUrl" to (user.photoUrl?.toString() ?: ""),
            "lastActive" to FieldValue.serverTimestamp()
        )
        return runCatching {
            doc.set(
                mapOf(
                    "profile" to profile,
                    "createdAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            true
        }.getOrDefault(false)
    }

    /** [encryptedKey] is AES ciphertext produced by EncryptionUtil. */
    suspend fun saveApiKey(encryptedKey: String): Boolean {
        val doc = userDoc() ?: return false
        return runCatching {
            doc.set(mapOf("config" to mapOf("geminiApiKey" to encryptedKey)), SetOptions.merge())
                .await()
            true
        }.getOrDefault(false)
    }

    /** Returns stored ciphertext (decryption is the caller's responsibility). */
    suspend fun getApiKey(): String? {
        val doc = userDoc() ?: return null
        return runCatching {
            val snapshot = doc.get().await()
            @Suppress("UNCHECKED_CAST")
            val config = snapshot.get("config") as? Map<String, Any?>
            config?.get("geminiApiKey") as? String
        }.getOrNull()
    }

    suspend fun saveConfig(config: Map<String, Any?>): Boolean {
        val doc = userDoc() ?: return false
        return runCatching {
            doc.set(mapOf("config" to config), SetOptions.merge()).await()
            true
        }.getOrDefault(false)
    }

    suspend fun getConfig(): Map<String, Any?>? {
        val doc = userDoc() ?: return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            doc.get().await().get("config") as? Map<String, Any?>
        }.getOrNull()
    }

    suspend fun saveConnectedApps(apps: Map<String, Boolean>): Boolean {
        val doc = userDoc() ?: return false
        return runCatching {
            doc.set(mapOf("connectedApps" to apps), SetOptions.merge()).await()
            true
        }.getOrDefault(false)
    }

    suspend fun getConnectedApps(): Map<String, Boolean>? {
        val doc = userDoc() ?: return null
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            doc.get().await().get("connectedApps") as? Map<String, Boolean>
        }.getOrNull()
    }

    suspend fun saveConversationMessage(
        contactId: String,
        app: String,
        contactName: String,
        text: String,
        isAI: Boolean
    ): Boolean {
        val doc = userDoc()?.collection("conversations")?.document(contactId) ?: return false
        val message = mapOf(
            "text" to text,
            "isAI" to isAI,
            "timestamp" to System.currentTimeMillis()
        )
        return runCatching {
            doc.set(
                mapOf(
                    "app" to app,
                    "contactName" to contactName,
                    "messages" to FieldValue.arrayUnion(message),
                    "lastUpdated" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
            true
        }.getOrDefault(false)
    }

    suspend fun getConversationHistory(contactId: String): List<Map<String, Any?>> {
        val doc = userDoc()?.collection("conversations")?.document(contactId) ?: return emptyList()
        return runCatching {
            @Suppress("UNCHECKED_CAST")
            (doc.get().await().get("messages") as? List<Map<String, Any?>>) ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
