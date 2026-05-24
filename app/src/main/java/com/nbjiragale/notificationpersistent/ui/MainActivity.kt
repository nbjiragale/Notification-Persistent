package com.nbjiragale.notificationpersistent.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nbjiragale.notificationpersistent.R
import com.nbjiragale.notificationpersistent.databinding.ActivityMainBinding
import com.nbjiragale.notificationpersistent.oem.RestrictionDetector
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private lateinit var detector: RestrictionDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.title = "WA Backup"
        detector = RestrictionDetector(this)

        adapter = ConversationAdapter(
            onClick = { conv ->
                startActivity(
                    Intent(this, ConversationActivity::class.java)
                        .putExtra(ConversationActivity.EXTRA_CONV_ID, conv.id)
                        .putExtra(ConversationActivity.EXTRA_CONTACT_NAME, conv.contactName)
                )
            },
            onToggleMonitor = { conv, monitored ->
                vm.setConversationMonitored(conv.id, monitored)
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.conversations.collect { list ->
                    adapter.submitList(list)
                    binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.listeningEnabled.collect { enabled ->
                    binding.fabToggle.setImageResource(
                        if (enabled) R.drawable.ic_pause else R.drawable.ic_play
                    )
                }
            }
        }

        binding.fabToggle.setOnClickListener { vm.setListeningEnabled(!vm.listeningEnabled.value) }
        binding.fabSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        vm.pruneOldRecords()
    }

    override fun onResume() {
        super.onResume()
        refreshWarnings()
    }

    private fun refreshWarnings() {
        val state = detector.snapshot()
        val warning = when {
            !state.notificationListenerEnabled -> "Notification access off — tap to fix"
            !state.ignoringBatteryOptimisations -> "Battery optimisation on — messages may be missed"
            else -> null
        }
        binding.tvWarning.text = warning
        binding.tvWarning.visibility = if (warning != null) View.VISIBLE else View.GONE
        binding.tvWarning.setOnClickListener {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }
    }
}
