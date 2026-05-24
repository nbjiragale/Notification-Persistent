package com.nbjiragale.notificationpersistent.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY lastMessageAtMs DESC")
    fun observeAll(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE contactName = :name LIMIT 1")
    suspend fun findByName(name: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ConversationEntity): Long

    @Query("UPDATE conversations SET lastMessageAtMs = :ms WHERE id = :id")
    suspend fun updateLastMessage(id: Long, ms: Long)

    @Query("UPDATE conversations SET isMonitored = :monitored WHERE id = :id")
    suspend fun setMonitored(id: Long, monitored: Boolean)
}
