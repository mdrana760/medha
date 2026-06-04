package com.medha.app.data

import android.content.Context

/**
 * Non-sensitive local settings store. The Gemini API key is NOT kept here — it
 * lives encrypted via [com.medha.app.ai.ApiKeyManager].
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    var masterEnabled: Boolean
        get() = sp.getBoolean(KEY_MASTER, true)
        set(value) = sp.edit().putBoolean(KEY_MASTER, value).apply()

    /** Seconds to wait before auto-sending an approved reply. */
    var responseDelay: Int
        get() = sp.getInt(KEY_DELAY, 30)
        set(value) = sp.edit().putInt(KEY_DELAY, value.coerceIn(0, 60)).apply()

    /** When true, replies are never sent without explicit user approval. */
    var requireApproval: Boolean
        get() = sp.getBoolean(KEY_APPROVAL, true)
        set(value) = sp.edit().putBoolean(KEY_APPROVAL, value).apply()

    var language: String
        get() = sp.getString(KEY_LANGUAGE, "bengali") ?: "bengali"
        set(value) = sp.edit().putString(KEY_LANGUAGE, value).apply()

    /** One of: always, screen_off, busy, schedule. */
    var autoReplyMode: String
        get() = sp.getString(KEY_MODE, "always") ?: "always"
        set(value) = sp.edit().putString(KEY_MODE, value).apply()

    var ttsEnabled: Boolean
        get() = sp.getBoolean(KEY_TTS, false)
        set(value) = sp.edit().putBoolean(KEY_TTS, value).apply()

    var scheduleEnabled: Boolean
        get() = sp.getBoolean(KEY_SCHED_ON, false)
        set(value) = sp.edit().putBoolean(KEY_SCHED_ON, value).apply()

    var scheduleStartHour: Int
        get() = sp.getInt(KEY_SCHED_START, 21)
        set(value) = sp.edit().putInt(KEY_SCHED_START, value.coerceIn(0, 23)).apply()

    var scheduleEndHour: Int
        get() = sp.getInt(KEY_SCHED_END, 9)
        set(value) = sp.edit().putInt(KEY_SCHED_END, value.coerceIn(0, 23)).apply()

    var setupComplete: Boolean
        get() = sp.getBoolean(KEY_SETUP, false)
        set(value) = sp.edit().putBoolean(KEY_SETUP, value).apply()

    /** Package names of apps the user opted to monitor. */
    var connectedApps: Set<String>
        get() = sp.getStringSet(KEY_APPS, emptySet()) ?: emptySet()
        set(value) = sp.edit().putStringSet(KEY_APPS, value).apply()

    fun isAppConnected(pkg: String): Boolean = connectedApps.contains(pkg)

    fun setAppConnected(pkg: String, connected: Boolean) {
        val current = connectedApps.toMutableSet()
        if (connected) current.add(pkg) else current.remove(pkg)
        connectedApps = current
    }

    companion object {
        private const val NAME = "medha_prefs"
        private const val KEY_MASTER = "master_enabled"
        private const val KEY_DELAY = "response_delay"
        private const val KEY_APPROVAL = "require_approval"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_MODE = "auto_reply_mode"
        private const val KEY_TTS = "tts_enabled"
        private const val KEY_SCHED_ON = "schedule_enabled"
        private const val KEY_SCHED_START = "schedule_start"
        private const val KEY_SCHED_END = "schedule_end"
        private const val KEY_SETUP = "setup_complete"
        private const val KEY_APPS = "connected_apps"
    }
}
