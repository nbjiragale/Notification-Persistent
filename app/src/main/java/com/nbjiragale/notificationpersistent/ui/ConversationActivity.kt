package com.nbjiragale.notificationpersistent.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nbjiragale.notificationpersistent.databinding.ActivityConversationBinding
import kotlinx.coroutines.launch

class ConversationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConversationBinding
    private val vm: MainViewModel by viewModels()
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConversationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val convId = intent.getLongExtra(EXTRA_CONV_ID, -1L)
        val contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Chat"
        supportActionBar?.title = contactName
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = MessageAdapter()
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerView.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.observeMessages(convId).collect { messages ->
                    adapter.submitList(messages) {
                        if (messages.isNotEmpty()) {
                            binding.recyclerView.scrollToPosition(messages.size - 1)
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    companion object {
        const val EXTRA_CONV_ID = "conv_id"
        const val EXTRA_CONTACT_NAME = "contact_name"
    }
}
