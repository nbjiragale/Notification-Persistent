package com.nbjiragale.notificationpersistent.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :convId ORDER BY timestampMs ASC")
    fun observeForConversation(convId: Long): Flow<List<MessageEntity>>

    @Query("""
        SELECT m.* FROM messages m
        INNER JOIN conversations c ON m.conversationId = c.id
        WHERE c.contactName = :contactName AND m.timestampMs >= :sinceMs
        ORDER BY m.timestampMs ASC
    """)
    suspend fun getForContactSince(contactName: String, sinceMs: Long): List<MessageEntity>

    @Query("SELECT id FROM messages WHERE dedupKey = :dedupKey LIMIT 1")
    suspend fun findByDedupKey(dedupKey: String): Long?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: MessageEntity): Long

    // Marks every still-pending incoming message of a conversation slot as resolved
    // (REPLIED when WhatsApp cleared the notification, DISMISSED when swiped away).
    @Query("UPDATE messages SET replyStatus = :status, removalReason = :reason WHERE notificationKey = :key AND direction = 'INCOMING' AND replyStatus = 'PENDING'")
    suspend fun markReplied(key: String, status: ReplyStatus, reason: Int): Int

    @Query("DELETE FROM messages WHERE timestampMs < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long): Int

    @Query("SELECT COUNT(*) FROM messages WHERE conversationId = :convId AND direction = 'INCOMING' AND replyStatus = 'PENDING'")
    fun observePendingCount(convId: Long): Flow<Int>
}
