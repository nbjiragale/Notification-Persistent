package com.nbjiragale.notificationpersistent.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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

// Configuration (event types, package filter, canRetrieveWindowContent) is declared
// in res/xml/accessibility_service_config.xml. We intentionally do NOT override
// serviceInfo here — doing so would drop canRetrieveWindowContent and break
// rootInActiveWindow traversal.
class WhatsAppAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repo: MessageRepository
    private lateinit var settings: AppSettings

    // Contact/group name currently visible in the WhatsApp conversation toolbar
    private var currentContact: String? = null

    override fun onCreate() {
        super.onCreate()
        repo = MessageRepository(AppDatabase.getInstance(applicationContext))
        settings = AppSettings(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.packageName?.toString() !in WhatsAppNotificationListener.WHATSAPP_PACKAGES) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val title = event.text.firstOrNull()?.toString()
                    ?: event.contentDescription?.toString()
                if (!title.isNullOrBlank() && title != "WhatsApp" && title != "WhatsApp Business") {
                    currentContact = title
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                val source = event.source ?: return
                handleClick(source)
                source.recycle()
            }
        }
    }

    private fun handleClick(source: AccessibilityNodeInfo) {
        val contact = currentContact ?: return
        val resourceId = source.viewIdResourceName ?: ""
        val contentDesc = source.contentDescription?.toString()?.lowercase() ?: ""
        val text = source.text?.toString()?.lowercase() ?: ""

        val isSendButton = resourceId.contains("send", ignoreCase = true) ||
            contentDesc.contains("send") || text == "send"
        val isMediaButton = resourceId.contains("attachment", ignoreCase = true) ||
            resourceId.contains("camera", ignoreCase = true) ||
            contentDesc.contains("attach") || contentDesc.contains("camera")

        when {
            isSendButton -> {
                val msgText = findInputText(rootInActiveWindow)
                if (!msgText.isNullOrBlank()) {
                    saveOutgoing(contact, msgText, MediaType.TEXT)
                }
            }
            isMediaButton -> saveOutgoing(contact, "[Media]", MediaType.IMAGE)
        }
    }

    private fun findInputText(root: AccessibilityNodeInfo?): String? {
        root ?: return null
        if (root.isEditable && !root.text.isNullOrBlank()) return root.text.toString()
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findInputText(child)
            child.recycle()
            if (result != null) return result
        }
        return null
    }

    private fun saveOutgoing(contact: String, content: String, mediaType: MediaType) {
        scope.launch {
            if (!settings.listeningEnabled.first()) return@launch
            val monitored = settings.monitoredContacts.first()
            if (monitored.isNotEmpty() && contact !in monitored) return@launch

            val convId = repo.getOrCreateConversation(contact, "com.whatsapp", false)
            if (convId < 0) return@launch
            repo.insertMessage(
                MessageEntity(
                    conversationId = convId,
                    notificationKey = null,
                    dedupKey = null,
                    direction = Direction.SENT,
                    content = content,
                    mediaType = mediaType,
                    timestampMs = System.currentTimeMillis(),
                    replyStatus = ReplyStatus.NONE
                )
            )
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
