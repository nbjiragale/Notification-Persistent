package com.nbjiragale.notificationpersistent.data.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("app_settings")

class AppSettings(private val context: Context) {
    companion object {
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_PIN_SALT = stringPreferencesKey("pin_salt")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_LISTENING_ENABLED = booleanPreferencesKey("listening_enabled")
        val KEY_BACKUP_NUMBER = stringPreferencesKey("backup_number")
        val KEY_BACKUP_CONTACT = stringPreferencesKey("backup_contact")
        val KEY_BACKUP_HOUR = intPreferencesKey("backup_hour")
        val KEY_BACKUP_MINUTE = intPreferencesKey("backup_minute")
        val KEY_MONITORED_CONTACTS = stringPreferencesKey("monitored_contacts")
        val KEY_LISTENER_LAST_BOUND_MS = longPreferencesKey("listener_last_bound_ms")
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { it[KEY_PIN_HASH] }
    val pinSalt: Flow<String?> = context.dataStore.data.map { it[KEY_PIN_SALT] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }
    val listeningEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_LISTENING_ENABLED] ?: false }
    val backupNumber: Flow<String?> = context.dataStore.data.map { it[KEY_BACKUP_NUMBER] }
    val backupContact: Flow<String?> = context.dataStore.data.map { it[KEY_BACKUP_CONTACT] }
    val backupHour: Flow<Int> = context.dataStore.data.map { it[KEY_BACKUP_HOUR] ?: 6 }
    val backupMinute: Flow<Int> = context.dataStore.data.map { it[KEY_BACKUP_MINUTE] ?: 0 }
    val monitoredContacts: Flow<Set<String>> = context.dataStore.data.map {
        val raw = it[KEY_MONITORED_CONTACTS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split(",").map { s -> s.trim() }.filter { s -> s.isNotBlank() }.toSet()
    }
    val listenerLastBoundMs: Flow<Long> = context.dataStore.data.map { it[KEY_LISTENER_LAST_BOUND_MS] ?: 0L }

    suspend fun setPinHash(hash: String, salt: String) = context.dataStore.edit {
        it[KEY_PIN_HASH] = hash
        it[KEY_PIN_SALT] = salt
    }
    suspend fun setOnboardingDone(v: Boolean) = context.dataStore.edit { it[KEY_ONBOARDING_DONE] = v }
    suspend fun setListeningEnabled(v: Boolean) = context.dataStore.edit { it[KEY_LISTENING_ENABLED] = v }
    suspend fun setBackupNumber(v: String) = context.dataStore.edit { it[KEY_BACKUP_NUMBER] = v }
    suspend fun setBackupContact(v: String) = context.dataStore.edit { it[KEY_BACKUP_CONTACT] = v }
    suspend fun setBackupTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[KEY_BACKUP_HOUR] = hour
        it[KEY_BACKUP_MINUTE] = minute
    }
    suspend fun setMonitoredContacts(contacts: Set<String>) = context.dataStore.edit {
        it[KEY_MONITORED_CONTACTS] = contacts.joinToString(",")
    }
    suspend fun setListenerLastBoundMs(ms: Long) = context.dataStore.edit { it[KEY_LISTENER_LAST_BOUND_MS] = ms }
    suspend fun clearAll() = context.dataStore.edit { it.clear() }

    fun listeningEnabledBlocking(): Boolean = runBlocking { listeningEnabled.first() }
    fun backupNumberBlocking(): String? = runBlocking { backupNumber.first() }
    fun backupContactBlocking(): String? = runBlocking { backupContact.first() }
    fun backupHourBlocking(): Int = runBlocking { backupHour.first() }
    fun backupMinuteBlocking(): Int = runBlocking { backupMinute.first() }
    fun monitoredContactsBlocking(): Set<String> = runBlocking { monitoredContacts.first() }
    fun pinHashBlocking(): String? = runBlocking { pinHash.first() }
    fun pinSaltBlocking(): String? = runBlocking { pinSalt.first() }
}
