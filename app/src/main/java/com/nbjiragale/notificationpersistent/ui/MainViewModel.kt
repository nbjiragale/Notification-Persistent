package com.nbjiragale.notificationpersistent.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nbjiragale.notificationpersistent.data.db.AppDatabase
import com.nbjiragale.notificationpersistent.data.db.ConversationEntity
import com.nbjiragale.notificationpersistent.data.db.MessageEntity
import com.nbjiragale.notificationpersistent.data.repository.MessageRepository
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import com.nbjiragale.notificationpersistent.service.ListenerForegroundService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = MessageRepository(db)
    val settings = AppSettings(application)

    val conversations: StateFlow<List<ConversationEntity>> =
        repo.observeConversations()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val listeningEnabled: StateFlow<Boolean> =
        settings.listeningEnabled
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun setListeningEnabled(enabled: Boolean) = viewModelScope.launch {
        settings.setListeningEnabled(enabled)
        val ctx = getApplication<Application>()
        if (enabled) ListenerForegroundService.start(ctx) else ListenerForegroundService.stop(ctx)
    }

    fun observeMessages(conversationId: Long): StateFlow<List<MessageEntity>> =
        repo.observeMessages(conversationId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setConversationMonitored(id: Long, monitored: Boolean) = viewModelScope.launch {
        repo.setConversationMonitored(id, monitored)
    }

    fun pruneOldRecords() = viewModelScope.launch(Dispatchers.IO) { repo.pruneOldRecords() }
}
