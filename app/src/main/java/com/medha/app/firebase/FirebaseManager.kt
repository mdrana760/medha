package com.medha.app.firebase

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Central access point for Firebase. Everything is null-safe: if the app was
 * built without a google-services.json, [isAvailable] is false and callers fall
 * back to local-only behaviour instead of crashing.
 *
 * Firestore layout (per the project spec):
 *   users/{uid}/profile/main
 *   users/{uid}/config/main
 *   users/{uid}/connectedApps/main
 *   users/{uid}/conversations/{contactId}
 *   admin/config
 */
object FirebaseManager {

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            initialized = FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            initialized = false
        }
    }

    val isAvailable: Boolean
        get() = initialized && try {
            FirebaseApp.getInstance(); true
        } catch (e: Exception) {
            false
        }

    val auth: FirebaseAuth?
        get() = if (isAvailable) runCatching { FirebaseAuth.getInstance() }.getOrNull() else null

    val firestore: FirebaseFirestore?
        get() = if (isAvailable) runCatching { FirebaseFirestore.getInstance() }.getOrNull() else null
}
