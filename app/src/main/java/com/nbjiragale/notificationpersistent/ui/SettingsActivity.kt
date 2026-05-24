package com.nbjiragale.notificationpersistent.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nbjiragale.notificationpersistent.alarm.DailyBackupScheduler
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import com.nbjiragale.notificationpersistent.databinding.ActivitySettingsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "Settings"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        settings = AppSettings(this)

        lifecycleScope.launch {
            binding.etBackupNumber.setText(settings.backupNumber.first() ?: "")
            binding.etBackupContact.setText(settings.backupContact.first() ?: "")
            binding.etMonitoredContacts.setText(
                settings.monitoredContacts.first().joinToString(", ")
            )
            val h = settings.backupHour.first()
            val m = settings.backupMinute.first()
            binding.etBackupTime.setText("%02d:%02d".format(h, m))
        }

        binding.btnSave.setOnClickListener { saveSettings() }
        binding.btnChangePin.setOnClickListener { changePin() }
    }

    private fun saveSettings() {
        lifecycleScope.launch {
            val number = binding.etBackupNumber.text.toString().trim()
            val contact = binding.etBackupContact.text.toString().trim()
            val contactsRaw = binding.etMonitoredContacts.text.toString()
            val timeRaw = binding.etBackupTime.text.toString().trim()

            if (number.isNotBlank()) settings.setBackupNumber(number)
            if (contact.isNotBlank()) settings.setBackupContact(contact)

            val contacts = contactsRaw.split(",")
                .map { it.trim() }.filter { it.isNotBlank() }.toSet()
            settings.setMonitoredContacts(contacts)

            val timeParts = timeRaw.split(":")
            if (timeParts.size == 2) {
                val h = timeParts[0].toIntOrNull() ?: 6
                val m = timeParts[1].toIntOrNull() ?: 0
                settings.setBackupTime(h, m)
                DailyBackupScheduler(this@SettingsActivity).scheduleFor(h, m)
            }

            Toast.makeText(this@SettingsActivity, "Saved", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changePin() {
        lifecycleScope.launch {
            settings.setPinHash("", "")
            startActivity(android.content.Intent(this@SettingsActivity, PinActivity::class.java))
            finish()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
