package com.nbjiragale.notificationpersistent.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import com.nbjiragale.notificationpersistent.data.db.AppDatabase
import com.nbjiragale.notificationpersistent.data.db.Direction
import com.nbjiragale.notificationpersistent.data.db.MessageEntity
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DailyBackupReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val settings = AppSettings(context)
        val backupNumber = settings.backupNumberBlocking() ?: return
        val backupContact = settings.backupContactBlocking() ?: return
        if (backupNumber.isBlank() || backupContact.isBlank()) return

        val midnight = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val messages = runBlocking {
            AppDatabase.getInstance(context)
                .messageDao()
                .getForContactSince(backupContact, midnight)
        }

        if (messages.isNotEmpty()) {
            sendSmsBackup(context, backupNumber, backupContact, messages)
        }

        // Reschedule for same time tomorrow
        DailyBackupScheduler(context).scheduleFor(
            settings.backupHourBlocking(),
            settings.backupMinuteBlocking()
        )
    }

    private fun sendSmsBackup(
        context: Context,
        number: String,
        contactName: String,
        messages: List<MessageEntity>
    ) {
        val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val header = "WA Backup [$contactName] $date\n"
        val body = messages.joinToString("\n") { msg ->
            val t = timeFmt.format(Date(msg.timestampMs))
            val arrow = if (msg.direction == Direction.INCOMING) "<<" else ">>"
            "$t $arrow ${msg.content}"
        }
        val full = header + body

        @Suppress("DEPRECATION")
        val sms: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            SmsManager.getDefault()
        }
        val parts = sms.divideMessage(full)
        sms.sendMultipartTextMessage(number, null, parts, null, null)
    }
}
