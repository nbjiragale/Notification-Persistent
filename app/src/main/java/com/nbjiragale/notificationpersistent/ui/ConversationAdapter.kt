package com.nbjiragale.notificationpersistent.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nbjiragale.notificationpersistent.data.db.ConversationEntity
import com.nbjiragale.notificationpersistent.databinding.ItemConversationBinding
import com.nbjiragale.notificationpersistent.util.TimeUtil

class ConversationAdapter(
    private val onClick: (ConversationEntity) -> Unit,
    private val onToggleMonitor: (ConversationEntity, Boolean) -> Unit
) : ListAdapter<ConversationEntity, ConversationAdapter.VH>(DiffCb) {

    inner class VH(val binding: ItemConversationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            tvContactName.text = item.contactName
            tvLastTime.text = if (item.lastMessageAtMs > 0) TimeUtil.elapsed(item.lastMessageAtMs) else ""
            switchMonitor.setOnCheckedChangeListener(null)
            switchMonitor.isChecked = item.isMonitored
            switchMonitor.setOnCheckedChangeListener { _, checked -> onToggleMonitor(item, checked) }
            root.setOnClickListener { onClick(item) }
        }
    }

    companion object DiffCb : DiffUtil.ItemCallback<ConversationEntity>() {
        override fun areItemsTheSame(o: ConversationEntity, n: ConversationEntity) = o.id == n.id
        override fun areContentsTheSame(o: ConversationEntity, n: ConversationEntity) = o == n
    }
}
