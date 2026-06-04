package com.medha.app.utils

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin wrapper around [TextToSpeech] with Bengali-first locale selection.
 * Used to optionally read out a suggested reply for accessibility.
 */
class TTSManager(context: Context) : TextToSpeech.OnInitListener {

    private val tts = TextToSpeech(context.applicationContext, this)
    private var ready = false
    var speed: Float = 1.0f
    var pitch: Float = 1.0f

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val bengali = Locale("bn", "BD")
            val result = tts.setLanguage(bengali)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                tts.language = Locale.getDefault()
            }
            ready = true
        }
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.setSpeechRate(speed)
        tts.setPitch(pitch)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "medha_tts")
    }

    fun stop() {
        if (ready) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
