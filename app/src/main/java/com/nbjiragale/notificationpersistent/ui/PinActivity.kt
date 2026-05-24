package com.nbjiragale.notificationpersistent.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nbjiragale.notificationpersistent.data.db.AppDatabase
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import com.nbjiragale.notificationpersistent.databinding.ActivityPinBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

class PinActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPinBinding
    private lateinit var settings: AppSettings
    private var isSettingPin = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings(this)

        lifecycleScope.launch {
            val existingHash = settings.pinHash.first()
            isSettingPin = existingHash.isNullOrBlank()
            binding.tvTitle.text = if (isSettingPin) "Set your 4-digit mPIN" else "Enter your mPIN"
            binding.tvForgotPin.visibility =
                if (isSettingPin) android.view.View.GONE else android.view.View.VISIBLE
        }

        binding.btnConfirm.setOnClickListener {
            val pin = binding.etPin.text?.toString() ?: ""
            if (pin.length != 4 || !pin.all { it.isDigit() }) {
                Toast.makeText(this, "Enter a 4-digit PIN", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (isSettingPin) setPin(pin) else verifyPin(pin)
        }

        binding.tvForgotPin.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Forgot PIN")
                .setMessage("This will delete all saved data. Are you sure?")
                .setPositiveButton("Yes, Reset") { _, _ -> resetApp() }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setPin(pin: String) {
        lifecycleScope.launch {
            val salt = generateSalt()
            val hash = hashPin(pin, salt)
            settings.setPinHash(hash, salt)
            navigateAfterPin()
        }
    }

    private fun verifyPin(pin: String) {
        lifecycleScope.launch {
            val storedHash = settings.pinHash.first()
            val salt = settings.pinSalt.first()
            if (storedHash.isNullOrBlank() || salt.isNullOrBlank()) {
                navigateAfterPin()
                return@launch
            }
            if (hashPin(pin, salt) == storedHash) {
                navigateAfterPin()
            } else {
                Toast.makeText(this@PinActivity, "Wrong PIN", Toast.LENGTH_SHORT).show()
                binding.etPin.text?.clear()
            }
        }
    }

    private fun resetApp() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                AppDatabase.getInstance(applicationContext).clearAllTables()
            }
            settings.clearAll()
            recreate()
        }
    }

    private suspend fun navigateAfterPin() {
        val done = settings.onboardingDone.first()
        startActivity(
            Intent(this, if (done) MainActivity::class.java else OnboardingActivity::class.java)
        )
        finish()
    }

    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val input = (pin + salt).toByteArray()
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
