package com.medha.app.ai

import android.content.Context
import com.medha.app.data.MedhaDatabase
import com.medha.app.data.MessageEntry

/**
 * Stores and retrieves the last few messages per contact so Gemini has context.
 * Backed by the local Room cache; the Firebase layer can mirror entries.
 */
class ConversationMemory(context: Context) {

    private val dao = MedhaDatabase.get(context).messageDao()

    suspend fun record(
        contactId: String,
        app: String,
        contactName: String,
        text: String,
        isAI: Boolean
    ): MessageEntry {
        val entry = MessageEntry(
            contactId = contactId,
            app = app,
            contactName = contactName,
            text = text,
            isAI = isAI
        )
        val id = dao.insert(entry)
        return entry.copy(id = id)
    }

    /** Builds a compact, chronological context string for the prompt. */
    suspend fun contextFor(contactId: String, limit: Int = MAX_CONTEXT): String {
        val recent = dao.recentForContact(contactId, limit).reversed()
        if (recent.isEmpty()) return "(no previous messages)"
        return recent.joinToString("\n") { entry ->
            val who = if (entry.isAI) "Medha" else entry.contactName.ifBlank { "Sender" }
            "$who: ${entry.text}"
        }
    }

    companion object {
        const val MAX_CONTEXT = 10
    }
}
