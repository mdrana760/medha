package com.medha.app.ai

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Minimal Gemini REST client. Uses the user's own API key — Medha never ships
 * or proxies a key of its own.
 */
class GeminiClient(
    private val model: String = DEFAULT_MODEL
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * Sends [systemPrompt] + [userText] to Gemini. Retries up to 3 times on
     * transient failure. Returns the generated text or a [Result.failure].
     */
    suspend fun generate(
        apiKey: String,
        systemPrompt: String,
        userText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Empty API key"))
        }

        val url = "$BASE_URL/$model:generateContent?key=$apiKey"
        val payload = buildPayload(systemPrompt, userText)
        val body = payload.toRequestBody(JSON)

        var lastError: Exception? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val request = Request.Builder().url(url).post(body).build()
                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        val text = parseText(raw)
                        if (!text.isNullOrBlank()) {
                            return@withContext Result.success(text.trim())
                        }
                        lastError = IllegalStateException("Empty response from Gemini")
                    } else {
                        lastError = IllegalStateException(
                            "Gemini error ${response.code}: ${raw.take(200)}"
                        )
                    }
                }
            } catch (e: Exception) {
                lastError = e
            }
            if (attempt < MAX_ATTEMPTS - 1) delay(BACKOFF_MS * (attempt + 1))
        }
        Result.failure(lastError ?: IllegalStateException("Unknown Gemini failure"))
    }

    /** A cheap call used to validate that a key works. */
    suspend fun validateKey(apiKey: String): Boolean {
        val result = generate(apiKey, "You are a test.", "Reply with the single word: ok")
        return result.isSuccess
    }

    private fun buildPayload(systemPrompt: String, userText: String): String {
        val root = JsonObject()

        val systemPart = JsonObject().apply { addProperty("text", systemPrompt) }
        val systemContent = JsonObject().apply {
            add("parts", gson.toJsonTree(listOf(systemPart)))
        }
        root.add("system_instruction", systemContent)

        val userPart = JsonObject().apply { addProperty("text", userText) }
        val userContent = JsonObject().apply {
            addProperty("role", "user")
            add("parts", gson.toJsonTree(listOf(userPart)))
        }
        root.add("contents", gson.toJsonTree(listOf(userContent)))

        val genConfig = JsonObject().apply {
            addProperty("temperature", 0.7)
            addProperty("maxOutputTokens", 256)
        }
        root.add("generationConfig", genConfig)
        return root.toString()
    }

    private fun parseText(raw: String): String? {
        return try {
            val root = JsonParser.parseString(raw).asJsonObject
            val candidates = root.getAsJsonArray("candidates") ?: return null
            if (candidates.size() == 0) return null
            val content = candidates[0].asJsonObject.getAsJsonObject("content") ?: return null
            val parts = content.getAsJsonArray("parts") ?: return null
            if (parts.size() == 0) return null
            parts[0].asJsonObject.get("text")?.asString
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models"
        const val DEFAULT_MODEL = "gemini-1.5-flash-latest"
        private const val MAX_ATTEMPTS = 3
        private const val BACKOFF_MS = 600L
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
