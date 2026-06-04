package com.medha.app.data

/** A messaging app Medha can draft replies for, via its notification reply action. */
data class SupportedApp(val packageName: String, val label: String)

/**
 * Curated list of apps that expose an inline reply (RemoteInput) action in their
 * notifications. Medha can only draft replies for apps in this category — it
 * never hooks into an app's internals.
 */
object SupportedApps {
    val ALL: List<SupportedApp> = listOf(
        SupportedApp("com.google.android.apps.messaging", "Messages (SMS)"),
        SupportedApp("com.whatsapp", "WhatsApp"),
        SupportedApp("com.whatsapp.w4b", "WhatsApp Business"),
        SupportedApp("com.facebook.orca", "Messenger"),
        SupportedApp("com.instagram.android", "Instagram"),
        SupportedApp("org.telegram.messenger", "Telegram"),
        SupportedApp("com.snapchat.android", "Snapchat"),
        SupportedApp("com.zhiliaoapp.musically", "TikTok"),
        SupportedApp("com.twitter.android", "X (Twitter)"),
        SupportedApp("com.linkedin.android", "LinkedIn"),
        SupportedApp("com.google.android.gm", "Gmail"),
        SupportedApp("org.thoughtcrime.securesms", "Signal")
    )
}
