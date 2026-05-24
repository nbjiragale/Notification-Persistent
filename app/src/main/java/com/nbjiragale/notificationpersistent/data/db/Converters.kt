package com.nbjiragale.notificationpersistent.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun directionToString(v: Direction): String = v.name
    @TypeConverter fun stringToDirection(v: String): Direction = Direction.valueOf(v)

    @TypeConverter fun mediaTypeToString(v: MediaType): String = v.name
    @TypeConverter fun stringToMediaType(v: String): MediaType = MediaType.valueOf(v)

    @TypeConverter fun replyStatusToString(v: ReplyStatus): String = v.name
    @TypeConverter fun stringToReplyStatus(v: String): ReplyStatus = ReplyStatus.valueOf(v)
}
