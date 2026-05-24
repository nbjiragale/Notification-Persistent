package com.nbjiragale.notificationpersistent.ui

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nbjiragale.notificationpersistent.alarm.DailyBackupScheduler
import com.nbjiragale.notificationpersistent.alarm.WatchdogScheduler
import com.nbjiragale.notificationpersistent.data.settings.AppSettings
import com.nbjiragale.notificationpersistent.databinding.ActivityOnboardingBinding
import com.nbjiragale.notificationpersistent.oem.DeepLinks
import com.nbjiragale.notificationpersistent.oem.OemDetector
import com.nbjiragale.notificationpersistent.service.ListenerForegroundService
import kotlinx.coroutines.launch

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var settings: AppSettings
    private var currentStep = 0

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { nextStep() }
    private val requestSmsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { nextStep() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = AppSettings(this)
        supportActionBar?.title = "Setup"

        binding.btnAction.setOnClickListener { handleStepAction() }
        binding.btnSkip.setOnClickListener { nextStep() }
        showStep(0)
    }

    private fun showStep(step: Int) {
        currentStep = step
        val s = STEPS[step]
        binding.tvStepTitle.text = s.title
        binding.tvStepDesc.text = s.desc
        binding.btnAction.text = s.actionLabel
        binding.btnSkip.visibility = if (s.canSkip) View.VISIBLE else View.GONE
        binding.tvProgress.text = "${step + 1} / ${STEPS.size}"
    }

    private fun handleStepAction() {
        when (currentStep) {
            0 -> startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            1 -> startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            2 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else nextStep()
            }
            3 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } else nextStep()
            }
            4 -> startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
            5 -> {
                val intent = DeepLinks.autostartIntent(OemDetector.detect())
                if (intent != null && DeepLinks.canResolve(this, intent)) {
                    startActivity(intent)
                } else nextStep()
            }
            6 -> requestSmsPermission.launch(Manifest.permission.SEND_SMS)
        }
    }

    private fun nextStep() {
        if (currentStep < STEPS.lastIndex) showStep(currentStep + 1)
        else finishOnboarding()
    }

    private fun finishOnboarding() {
        lifecycleScope.launch {
            settings.setOnboardingDone(true)
            settings.setListeningEnabled(true)
            ListenerForegroundService.start(this@OnboardingActivity)
            WatchdogScheduler(this@OnboardingActivity).scheduleNext()
            DailyBackupScheduler(this@OnboardingActivity).scheduleFor(6, 0)
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (currentStep == 0) {
            val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: ""
            if (flat.contains(packageName)) nextStep()
        }
    }

    private data class Step(val title: String, val desc: String, val actionLabel: String, val canSkip: Boolean)

    companion object {
        private val STEPS = listOf(
            Step("Notification Access",
                "Allow reading WhatsApp notifications to capture incoming messages.",
                "Open Settings", false),
            Step("Accessibility Access",
                "Allow detecting when you send messages in WhatsApp.",
                "Open Settings", false),
            Step("Notification Permission",
                "Allow showing the monitoring status notification.",
                "Grant", true),
            Step("Exact Alarms",
                "Allow precise scheduling for the 6am daily backup alarm.",
                "Grant", true),
            Step("Battery Optimisation",
                "Exclude this app from battery optimisation so it keeps running in the background.",
                "Disable Optimisation", true),
            Step("Autostart (OEM)",
                "Enable autostart for this app so it survives device restarts.",
                "Open Settings", true),
            Step("SMS Permission (Optional)",
                "Allow sending the daily backup SMS to your backup number. Skip if not needed.",
                "Grant SMS", true)
        )
    }
}
