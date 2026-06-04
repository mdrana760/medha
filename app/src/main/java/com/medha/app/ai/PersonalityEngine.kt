package com.medha.app.ai

/**
 * Builds the system prompt sent to Gemini for each context. The assistant is
 * always explicitly introduced as an AI replying on the user's behalf — it never
 * impersonates the user as a human.
 */
object PersonalityEngine {

    enum class Channel { CALL, MESSAGE, EMAIL }

    private fun languageDirective(language: String): String = when (language) {
        "english" -> "Reply in clear, natural English."
        "mixed" -> "Reply in natural Bengali, mixing in English where it reads naturally."
        else -> "Reply in natural, conversational Bengali (বাংলা)."
    }

    fun buildPrompt(
        channel: Channel,
        userName: String,
        senderName: String,
        history: String,
        language: String
    ): String {
        val lang = languageDirective(language)
        val identity =
            "তুমি \"Medha\", $userName এর AI assistant। তুমি একজন AI — মানুষ নও, " +
                "এবং প্রয়োজনে স্পষ্ট করে দেবে যে তুমি $userName এর হয়ে স্বয়ংক্রিয় উত্তর দিচ্ছ।"

        return when (channel) {
            Channel.CALL -> """
                $identity
                $userName এখন একটু ব্যস্ত। কলদাতার সাথে বিনয়ের সাথে, সংক্ষেপে কথা বলো।
                প্রয়োজনে message নিয়ে রাখার প্রস্তাব দাও।
                $lang
                Recent context: $history
            """.trimIndent()

            Channel.MESSAGE -> """
                $identity
                Sender: $senderName
                তুমি $userName এর হয়ে স্বাভাবিক, সংক্ষিপ্ত, ভদ্র reply তৈরি করছ।
                কোনো ব্যক্তিগত বা সংবেদনশীল তথ্য অনুমান করে বলো না।
                $lang
                Previous conversation: $history
            """.trimIndent()

            Channel.EMAIL -> """
                $identity
                তুমি $userName এর email assistant।
                Professional এবং পরিষ্কার ভাষায়, প্রয়োজন অনুযায়ী Bengali বা English এ reply লেখো।
                $lang
                Context: $history
            """.trimIndent()
        }
    }

    /** Used when the network or API fails, so the user still has something to approve. */
    fun fallbackReply(userName: String, language: String): String = when (language) {
        "english" ->
            "Hi, this is Medha, $userName's AI assistant. $userName is busy right now " +
                "and will get back to you soon."
        else ->
            "হ্যালো, আমি Medha — $userName এর AI assistant। $userName এখন একটু ব্যস্ত, " +
                "শীঘ্রই আপনার সাথে যোগাযোগ করবেন।"
    }
}
