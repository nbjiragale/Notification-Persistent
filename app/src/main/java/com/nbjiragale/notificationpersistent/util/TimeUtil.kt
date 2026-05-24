package com.nbjiragale.notificationpersistent.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeUtil {
    fun elapsed(postedAtMs: Long): String {
        val delta = System.currentTimeMillis() - postedAtMs
        return when {
            delta < 60_000 -> "Just now"
            delta < 3_600_000 -> "${delta / 60_000}m ago"
            delta < 86_400_000 -> "${delta / 3_600_000}h ago"
            else -> "${delta / 86_400_000}d ago"
        }
    }

    fun time(ms: Long): String =
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ms))

    fun dateTime(ms: Long): String =
        SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(ms))
}
