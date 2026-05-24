package com.nbjiragale.notificationpersistent.oem

import android.app.AlarmManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.nbjiragale.notificationpersistent.service.WhatsAppNotificationListener

data class RestrictionState(
    val notificationListenerEnabled: Boolean,
    val ignoringBatteryOptimisations: Boolean,
    val exactAlarmGranted: Boolean
)

class RestrictionDetector(private val context: Context) {

    fun snapshot() = RestrictionState(
        notificationListenerEnabled = isNotificationListenerEnabled(),
        ignoringBatteryOptimisations = isIgnoringBatteryOptimisations(),
        exactAlarmGranted = canScheduleExactAlarms()
    )

    fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.contains(
            ComponentName(context, WhatsAppNotificationListener::class.java).flattenToString()
        )
    }

    private fun isIgnoringBatteryOptimisations(): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }
}
