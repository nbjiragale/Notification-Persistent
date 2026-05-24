package com.nbjiragale.notificationpersistent.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object AppNotificationChannels {
    const val PERSISTENT = "channel_persistent"
    const val ALERTS = "channel_alerts"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(PERSISTENT, "Monitoring Status", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(ALERTS, "Warnings", NotificationManager.IMPORTANCE_HIGH)
            )
        )
    }
}
