package com.medha.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One logged message in a conversation. Only the user's own assistant activity
 * is stored: incoming messages the user received and the AI replies the user
 * approved. This local cache feeds conversation context back to Gemini and
 * powers the in-app conversation log.
 */
@Entity(tableName = "messages")
data class MessageEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactId: String,
    val app: String,
    val contactName: String,
    val text: String,
    val isAI: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
