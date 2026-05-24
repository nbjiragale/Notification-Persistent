package com.nbjiragale.notificationpersistent.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.nbjiragale.notificationpersistent.data.db.AppDatabase
import com.nbjiragale.notificationpersistent.data.db.Direction
import com.nbjiragale.notificationpersistent.data.db.MediaType
import com.nbjiragale.notificationpersistent.data.db.MessageEntity
import com.nbjiragale.notificationpersistent.data.db.ReplyStatus
import com.nbjiragale.notificationpersistent.data.repository.MessageRepository
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WhatsAppNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: MessageRepository
    private lateinit var settings: AppSettings

    override fun onCreate() {
        super.onCreate()
        repo = MessageRepository(AppDatabase.getInstance(applicationContext))
        settings = AppSettings(applicationContext)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        scope.launch { settings.setListenerLastBoundMs(System.currentTimeMillis()) }
        ListenerForegroundService.start(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        scope.launch { settings.setListenerLastBoundMs(0L) }
        try {
            requestRebind(ComponentName(this, WhatsAppNotificationListener::class.java))
        } catch (_: Exception) {}
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in WHATSAPP_PACKAGES) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()

        scope.launch {
            if (!settings.listeningEnabled.first()) return@launch

            val monitored = settings.monitoredContacts.first()
            if (monitored.isNotEmpty() && title !in monitored) return@launch

            val existing = AppDatabase.getInstance(applicationContext)
                .messageDao().findPendingByKey(sbn.key)
            if (existing != null) return@launch

            val (isGroup, groupSender, preview) = parseNotification(title, text, bigText)
            val convId = repo.getOrCreateConversation(title, sbn.packageName, isGroup)
            if (convId < 0) return@launch

            repo.insertMessage(
                MessageEntity(
                    conversationId = convId,
                    notificationKey = sbn.key,
                    direction = Direction.INCOMING,
                    content = if (groupSender != null) "[$groupSender] $preview" else preview,
                    mediaType = MediaType.TEXT,
                    timestampMs = sbn.postTime,
                    replyStatus = ReplyStatus.PENDING
                )
            )
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification,
        rankingMap: RankingMap,
        reason: Int
    ) {
        if (sbn.packageName !in WHATSAPP_PACKAGES) return
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val status = when (reason) {
            REASON_APP_CANCEL, REASON_APP_CANCEL_ALL -> ReplyStatus.REPLIED
            REASON_LISTENER_CANCEL, REASON_LISTENER_CANCEL_ALL -> ReplyStatus.DISMISSED
            else -> ReplyStatus.AUTO_CANCELLED
        }

        scope.launch { repo.markReplied(sbn.key, status, reason) }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    private data class ParsedNotification(
        val isGroup: Boolean,
        val groupSender: String?,
        val preview: String
    )

    private fun parseNotification(title: String, text: String, bigText: String?): ParsedNotification {
        val content = bigText ?: text
        // Group chat: EXTRA_TEXT = "SenderName: message" (colon after a short name)
        val colonIdx = text.indexOf(": ")
        val isGroup = colonIdx in 1..39
        return if (isGroup) {
            val sender = text.substring(0, colonIdx)
            val msg = content.substringAfter(": ")
            ParsedNotification(true, sender, msg)
        } else {
            ParsedNotification(false, null, content)
        }
    }

    companion object {
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
        private const val REASON_LISTENER_CANCEL = 6
        private const val REASON_LISTENER_CANCEL_ALL = 7
        private const val REASON_APP_CANCEL = 8
        private const val REASON_APP_CANCEL_ALL = 9
    }
}
