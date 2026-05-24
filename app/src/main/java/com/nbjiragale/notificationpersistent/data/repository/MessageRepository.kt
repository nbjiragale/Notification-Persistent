package com.nbjiragale.notificationpersistent.data.repository

import com.nbjiragale.notificationpersistent.data.db.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class MessageRepository(private val db: AppDatabase) {

    fun observeConversations(): Flow<List<ConversationEntity>> =
        db.conversationDao().observeAll()

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        db.messageDao().observeForConversation(conversationId)

    fun observePendingCount(conversationId: Long): Flow<Int> =
        db.messageDao().observePendingCount(conversationId)

    suspend fun getOrCreateConversation(
        contactName: String,
        packageName: String,
        isGroup: Boolean
    ): Long {
        val existing = db.conversationDao().findByName(contactName)
        if (existing != null) return existing.id
        val newId = db.conversationDao().insert(
            ConversationEntity(
                contactName = contactName,
                packageName = packageName,
                isGroup = isGroup
            )
        )
        // IGNORE conflict means insert returns -1 if duplicate race — re-query
        if (newId == -1L) {
            return db.conversationDao().findByName(contactName)?.id ?: -1L
        }
        return newId
    }

    suspend fun insertMessage(message: MessageEntity): Long {
        val id = db.messageDao().insert(message)
        db.conversationDao().updateLastMessage(message.conversationId, message.timestampMs)
        return id
    }

    suspend fun markReplied(notificationKey: String, status: ReplyStatus, reason: Int) =
        db.messageDao().markReplied(notificationKey, status, reason)

    suspend fun pruneOldRecords() {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        db.messageDao().deleteOlderThan(cutoff)
    }

    suspend fun setConversationMonitored(id: Long, monitored: Boolean) =
        db.conversationDao().setMonitored(id, monitored)
}
