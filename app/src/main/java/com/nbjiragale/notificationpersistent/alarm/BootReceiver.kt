package com.nbjiragale.notificationpersistent.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import com.nbjiragale.notificationpersistent.service.ListenerForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in HANDLED_ACTIONS) return
        if (!AppSettings(context).listeningEnabledBlocking()) return
        ListenerForegroundService.start(context)
        WatchdogScheduler(context).scheduleNext()
    }

    companion object {
        val HANDLED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_SET,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
