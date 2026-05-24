package com.nbjiragale.notificationpersistent.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["contactName"], unique = true)]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactName: String,
    val packageName: String,
    val isGroup: Boolean = false,
    val isMonitored: Boolean = true,
    val lastMessageAtMs: Long = 0L
)
