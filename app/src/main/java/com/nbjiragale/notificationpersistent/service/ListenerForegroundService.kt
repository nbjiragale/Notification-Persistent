package com.nbjiragale.notificationpersistent.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.nbjiragale.notificationpersistent.R
import com.nbjiragale.notificationpersistent.alarm.WatchdogScheduler
import com.nbjiragale.notificationpersistent.ui.MainActivity
import com.nbjiragale.notificationpersistent.util.AppNotificationChannels

class ListenerForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        AppNotificationChannels.ensureCreated(this)
        startForegroundCompat()
        WatchdogScheduler(this).scheduleNext()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notification = Notification.Builder(this, AppNotificationChannels.PERSISTENT)
            .setContentTitle("WA Backup")
            .setContentText("Monitoring messages…")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val NOTIFICATION_ID = 9201

        fun start(context: Context) {
            val i = Intent(context, ListenerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ListenerForegroundService::class.java))
        }
    }
}
