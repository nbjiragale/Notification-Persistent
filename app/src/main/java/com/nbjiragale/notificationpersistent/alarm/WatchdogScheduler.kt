package com.nbjiragale.notificationpersistent.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class WatchdogScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNext() {
        val pi = buildPendingIntent()
        val triggerMs = System.currentTimeMillis() + INTERVAL_MS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pi)
        } else {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerMs, pi), pi)
        }
    }

    fun cancel() = alarmManager.cancel(buildPendingIntent())

    private fun buildPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, WatchdogReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        private const val INTERVAL_MS = 30 * 60 * 1000L
        const val REQUEST_CODE = 7001
    }
}
