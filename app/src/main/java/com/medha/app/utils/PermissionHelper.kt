package com.medha.app.utils

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils
import com.medha.app.services.MedhaNotificationService

/**
 * Read-only helpers that report whether each user-granted capability is enabled.
 * Nothing here grants permissions silently — it only checks current state so the
 * UI can guide the user to the appropriate system settings screen.
 */
object PermissionHelper {

    /** Whether the user enabled notification access for Medha's listener. */
    fun isNotificationAccessGranted(context: Context): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        ) ?: return false
        if (TextUtils.isEmpty(flat)) return false
        val expected = ComponentName(context, MedhaNotificationService::class.java)
        return flat.split(":").any {
            ComponentName.unflattenFromString(it) == expected
        }
    }

    /** Whether the user granted "draw over other apps" for the approval overlay. */
    fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)
}
