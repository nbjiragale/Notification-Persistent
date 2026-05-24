package com.nbjiragale.notificationpersistent.alarm

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.PowerManager
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import com.nbjiragale.notificationpersistent.service.ListenerForegroundService
import com.nbjiragale.notificationpersistent.service.WhatsAppNotificationListener

class WatchdogReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "notificationpersistent:watchdog")
        wl.acquire(10_000L)
        try {
            if (!AppSettings(context).listeningEnabledBlocking()) return
            ensureFgsRunning(context)
            rebindListenerService(context)
        } finally {
            WatchdogScheduler(context).scheduleNext()
            if (wl.isHeld) wl.release()
        }
    }

    @Suppress("DEPRECATION")
    private fun ensureFgsRunning(context: Context) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val running = am.getRunningServices(Int.MAX_VALUE)
            ?.any { it.service.className == ListenerForegroundService::class.java.name } == true
        if (!running) ListenerForegroundService.start(context)
    }

    private fun rebindListenerService(context: Context) {
        val component = ComponentName(context, WhatsAppNotificationListener::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
        pm.setComponentEnabledSetting(
            component,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}
