package com.nbjiragale.notificationpersistent.oem

import android.content.ComponentName
import android.content.Context
import android.content.Intent

object DeepLinks {
    fun autostartIntent(oem: OemType): Intent? = when (oem) {
        OemType.XIAOMI -> Intent().apply {
            component = ComponentName(
                "com.miui.securitycenter",
                "com.miui.permcenter.autostart.AutoStartManagementActivity"
            )
        }
        OemType.VIVO -> Intent().apply {
            component = ComponentName(
                "com.iqoo.secure",
                "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
            )
        }
        OemType.OPPO -> Intent().apply {
            component = ComponentName(
                "com.coloros.safecenter",
                "com.coloros.safecenter.permission.startup.StartupAppListActivity"
            )
        }
        OemType.SAMSUNG -> Intent("com.samsung.android.sm.ACTION_APP_LOCK")
        OemType.HUAWEI -> Intent().apply {
            component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
            )
        }
        else -> null
    }

    fun canResolve(context: Context, intent: Intent): Boolean =
        context.packageManager.resolveActivity(intent, 0) != null
}
