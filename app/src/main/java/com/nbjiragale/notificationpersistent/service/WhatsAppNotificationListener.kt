package com.nbjiragale.notificationpersistent.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
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
        val notif = sbn.notification ?: return
        if (notif.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val titleExtra = notif.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()

        scope.launch {
            if (!settings.listeningEnabled.first()) return@launch

            val messages = extractMessages(sbn, titleExtra)
            if (messages.isEmpty()) return@launch

            val contactName = messages.first().conversation
            val monitored = settings.monitoredContacts.first()
            if (monitored.isNotEmpty() && contactName !in monitored) return@launch

            val convId = repo.getOrCreateConversation(
                contactName, sbn.packageName, messages.first().isGroup
            )
            if (convId < 0) return@launch

            for (m in messages) {
                if (repo.existsByDedupKey(m.dedupKey)) continue
                repo.insertMessage(
                    MessageEntity(
                        conversationId = convId,
                        notificationKey = sbn.key,
                        dedupKey = m.dedupKey,
                        direction = Direction.INCOMING,
                        content = m.content,
                        mediaType = MediaType.TEXT,
                        timestampMs = m.timestamp,
                        replyStatus = ReplyStatus.PENDING
                    )
                )
            }
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

    private data class IncomingMsg(
        val conversation: String,
        val isGroup: Boolean,
        val content: String,
        val timestamp: Long,
        val dedupKey: String
    )

    // Prefer MessagingStyle (WhatsApp uses it) so every message in a stacked
    // notification is captured, with per-sender names in group chats. Falls back
    // to plain title/text extras for notifications without a MessagingStyle.
    private fun extractMessages(sbn: StatusBarNotification, titleExtra: String?): List<IncomingMsg> {
        val notif = sbn.notification
        val style = try {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notif)
        } catch (_: Exception) {
            null
        }

        if (style != null && style.messages.isNotEmpty()) {
            val convTitle = style.conversationTitle?.toString()
            val isGroup = style.isGroupConversation || convTitle != null
            val conversation = convTitle ?: titleExtra ?: "Unknown"
            return style.messages.mapNotNull { m ->
                val text = m.text?.toString() ?: return@mapNotNull null
                val sender = m.person?.name?.toString()
                val ts = if (m.timestamp > 0) m.timestamp else sbn.postTime
                val content = if (isGroup && !sender.isNullOrBlank()) "[$sender] $text" else text
                IncomingMsg(
                    conversation = conversation,
                    isGroup = isGroup,
                    content = content,
                    timestamp = ts,
                    dedupKey = "${sbn.key}|$ts|${text.hashCode()}"
                )
            }
        }

        // Fallback
        val title = titleExtra ?: return emptyList()
        val extras = notif.extras ?: return emptyList()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return emptyList()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val body = bigText ?: text
        val colonIdx = text.indexOf(": ")
        val isGroup = colonIdx in 1..39
        val content = if (isGroup) {
            val sender = text.substring(0, colonIdx)
            "[$sender] ${body.substringAfter(": ")}"
        } else {
            body
        }
        return listOf(
            IncomingMsg(
                conversation = title,
                isGroup = isGroup,
                content = content,
                timestamp = sbn.postTime,
                dedupKey = "${sbn.key}|${sbn.postTime}|${content.hashCode()}"
            )
        )
    }

    companion object {
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
        private const val REASON_LISTENER_CANCEL = 6
        private const val REASON_LISTENER_CANCEL_ALL = 7
        private const val REASON_APP_CANCEL = 8
        private const val REASON_APP_CANCEL_ALL = 9
    }
}
