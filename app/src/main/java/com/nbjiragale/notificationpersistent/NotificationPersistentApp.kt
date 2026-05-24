package com.nbjiragale.notificationpersistent

import android.app.Application
import com.nbjiragale.notificationpersistent.util.AppNotificationChannels

class NotificationPersistentApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppNotificationChannels.ensureCreated(this)
    }
}
