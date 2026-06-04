package com.medha.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert
    suspend fun insert(entry: MessageEntry): Long

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recentForContact(contactId: String, limit: Int): List<MessageEntry>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MessageEntry>

    @Query("SELECT * FROM messages WHERE text LIKE '%' || :query || '%' OR contactName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    suspend fun search(query: String): List<MessageEntry>

    @Query("SELECT COUNT(*) FROM messages WHERE isAI = 1 AND timestamp >= :since")
    suspend fun countAiSince(since: Long): Int

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteForContact(contactId: String)

    @Query("DELETE FROM messages")
    suspend fun clear()
}
