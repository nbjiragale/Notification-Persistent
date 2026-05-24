package com.nbjiragale.notificationpersistent.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nbjiragale.notificationpersistent.data.db.Direction
import com.nbjiragale.notificationpersistent.data.db.MessageEntity
import com.nbjiragale.notificationpersistent.data.db.ReplyStatus
import com.nbjiragale.notificationpersistent.databinding.ItemMessageIncomingBinding
import com.nbjiragale.notificationpersistent.databinding.ItemMessageSentBinding
import com.nbjiragale.notificationpersistent.util.TimeUtil

class MessageAdapter : ListAdapter<MessageEntity, RecyclerView.ViewHolder>(MessageDiffCb()) {

    inner class IncomingVH(val b: ItemMessageIncomingBinding) : RecyclerView.ViewHolder(b.root)
    inner class SentVH(val b: ItemMessageSentBinding) : RecyclerView.ViewHolder(b.root)

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).direction == Direction.INCOMING) TYPE_INCOMING else TYPE_SENT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_INCOMING) {
            IncomingVH(ItemMessageIncomingBinding.inflate(inf, parent, false))
        } else {
            SentVH(ItemMessageSentBinding.inflate(inf, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is IncomingVH -> with(holder.b) {
                tvContent.text = item.content
                tvTime.text = TimeUtil.time(item.timestampMs)
                tvStatus.text = when (item.replyStatus) {
                    ReplyStatus.PENDING -> "Unanswered"
                    ReplyStatus.REPLIED -> "✓ Replied"
                    ReplyStatus.DISMISSED -> "✗ Dismissed"
                    else -> ""
                }
            }
            is SentVH -> with(holder.b) {
                tvContent.text = item.content
                tvTime.text = TimeUtil.time(item.timestampMs)
            }
        }
    }

    companion object {
        const val TYPE_INCOMING = 0
        const val TYPE_SENT = 1
    }

    private class MessageDiffCb : DiffUtil.ItemCallback<MessageEntity>() {
        override fun areItemsTheSame(o: MessageEntity, n: MessageEntity) = o.id == n.id
        override fun areContentsTheSame(o: MessageEntity, n: MessageEntity) = o == n
    }
}
