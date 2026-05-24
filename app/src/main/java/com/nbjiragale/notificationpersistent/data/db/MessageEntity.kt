package com.nbjiragale.notificationpersistent.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ConversationEntity::class,
        parentColumns = ["id"],
        childColumns = ["conversationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("conversationId"), Index("notificationKey")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val notificationKey: String?,
    val direction: Direction,
    val content: String,
    val mediaType: MediaType = MediaType.TEXT,
    val timestampMs: Long,
    val replyStatus: ReplyStatus = ReplyStatus.NONE,
    val removalReason: Int? = null
)

enum class Direction { INCOMING, SENT }
enum class MediaType { TEXT, IMAGE, VIDEO, FILE, AUDIO }
enum class ReplyStatus { NONE, PENDING, REPLIED, DISMISSED, AUTO_CANCELLED, UNKNOWN }
