package com.nvv.petber.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.MessageModel
import com.nvv.petber.databinding.ItemMessageReceivedBinding
import com.nvv.petber.databinding.ItemMessageSentBinding
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.loadImage
import com.nvv.petber.utils.ext.visible

class MessageAdapter(
    private val currentUserId: String,
    private val otherAvatarUrl: String?,
    private val onAvatarClick: (String) -> Unit,
    private val onMessageLongClick: (MessageModel, View) -> Unit,
    private val onMediaClick: (MessageModel) -> Unit
) : ListAdapter<MessageModel, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2

        private val DiffCallback = object : DiffUtil.ItemCallback<MessageModel>() {
            override fun areItemsTheSame(oldItem: MessageModel, newItem: MessageModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: MessageModel, newItem: MessageModel) =
                oldItem == newItem
        }
    }


    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            SentMessageViewHolder(ItemMessageSentBinding.inflate(inflater, parent, false))
        } else {
            ReceivedMessageViewHolder(ItemMessageReceivedBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        val prevMessage = if (position > 0) getItem(position - 1) else null

        val showTime = TimeUtils.shouldShowTime(message.createdAt, prevMessage?.createdAt)
        if (holder is SentMessageViewHolder) holder.bind(message, showTime)
        else if (holder is ReceivedMessageViewHolder) holder.bind(message, showTime)
    }

    inner class SentMessageViewHolder(private val binding: ItemMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel, showTime: Boolean) {
            binding.ivMedia.visibility = View.GONE
            binding.ivPlayIcon.visibility = View.GONE
            binding.tvContent.visibility = View.VISIBLE
            binding.flMessageContainer.setBackgroundResource(R.drawable.bg_bubble_sent)
            if (showTime) {
                binding.tvTime.text = TimeUtils.formatMessageTime(message.createdAt)
                binding.tvTime.visible()
            } else {
                binding.tvTime.gone()
            }
            // Deleted
            if (message.isDeleted) {
                showDeleted()
                bindLongClick(message)
                return
            }

            // Content / Media
            if (!message.mediaUrl.isNullOrEmpty()) {
                binding.flMessageContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.tvContent.gone()
                binding.ivMedia.visibility = View.VISIBLE
                binding.ivMedia.loadImage(message.mediaUrl)
                binding.ivPlayIcon.visibility =
                    if (message.mediaType == "video") View.VISIBLE else View.GONE
                binding.ivMedia.setOnClickListener {
                    onMediaClick(message)
                }
                binding.ivMedia.setOnLongClickListener {
                    onMessageLongClick(message, it)
                    true
                }
                binding.ivPlayIcon.setOnClickListener {
                    onMediaClick(message)
                }
                binding.ivPlayIcon.setOnLongClickListener{
                    onMessageLongClick(message, it)
                    true
                }
            } else {
                binding.flMessageContainer.setBackgroundResource(R.drawable.bg_bubble_sent)
                binding.tvContent.visibility = View.VISIBLE
                binding.ivMedia.visibility = View.GONE
                binding.ivPlayIcon.visibility = View.GONE
                binding.tvContent.text = message.content
                binding.tvContent.setTypeface(null, Typeface.NORMAL)
                binding.ivMedia.setOnClickListener(null)
                binding.ivPlayIcon.setOnClickListener(null)
            }

            bindLongClick(message)
        }

        private fun showDeleted() {
            binding.tvContent.text =
                binding.root.context.getString(R.string.msg_deleted_placeholder)
            binding.tvContent.setTypeface(null, Typeface.ITALIC)
            binding.tvContent.visibility = View.VISIBLE
            binding.ivMedia.visibility = View.GONE
            binding.ivPlayIcon.visibility = View.GONE
        }

        private fun bindLongClick(message: MessageModel) {
            binding.flMessageContainer.setOnLongClickListener {
                onMessageLongClick(message, it)
                true
            }
        }
    }


    inner class ReceivedMessageViewHolder(private val binding: ItemMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: MessageModel, showTime: Boolean) {
            binding.ivMedia.visibility = View.GONE
            binding.ivPlayIcon.visibility = View.GONE
            binding.tvContent.visibility = View.VISIBLE
            binding.flMessageContainer.setBackgroundResource(R.drawable.bg_bubble_received)
            if (showTime) {
                binding.tvTime.text = TimeUtils.formatMessageTime(message.createdAt)
                binding.tvTime.visibility = View.VISIBLE
            } else {
                binding.tvTime.visibility = View.GONE
            }
            binding.ivAvatar.loadAvatar(otherAvatarUrl)
            binding.ivAvatar.setOnClickListener { onAvatarClick(message.senderId) }
            // Deleted
            if (message.isDeleted) {
                showDeleted()
                bindLongClick(message)
                return
            }

            // Content / Media
            if (!message.mediaUrl.isNullOrEmpty()) {
                binding.flMessageContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                binding.tvContent.visibility = View.GONE
                binding.ivMedia.visibility = View.VISIBLE
                binding.ivMedia.loadImage(message.mediaUrl)
                binding.ivPlayIcon.visibility =
                    if (message.mediaType == "video") View.VISIBLE else View.GONE
                binding.ivMedia.setOnClickListener {
                    onMediaClick(message)
                }
                binding.ivMedia.setOnLongClickListener {
                    onMessageLongClick(message, it)
                    true
                }
                binding.ivPlayIcon.setOnClickListener {
                    onMediaClick(message)
                }
                binding.ivPlayIcon.setOnLongClickListener{
                    onMessageLongClick(message, it)
                    true
                }
            } else {
                binding.flMessageContainer.setBackgroundResource(R.drawable.bg_bubble_received)
                binding.tvContent.visibility = View.VISIBLE
                binding.ivMedia.visibility = View.GONE
                binding.ivPlayIcon.visibility = View.GONE
                binding.tvContent.text = message.content
                binding.tvContent.setTypeface(null, Typeface.NORMAL)
                binding.ivMedia.setOnClickListener(null)
                binding.ivPlayIcon.setOnClickListener(null)
            }

            bindLongClick(message)
        }

        private fun showDeleted() {
            binding.tvContent.text =
                binding.root.context.getString(R.string.msg_deleted_placeholder)
            binding.tvContent.setTypeface(null, Typeface.ITALIC)
            binding.tvContent.visibility = View.VISIBLE
            binding.ivMedia.visibility = View.GONE
            binding.ivPlayIcon.visibility = View.GONE
        }

        private fun bindLongClick(message: MessageModel) {
            binding.flMessageContainer.setOnLongClickListener {
                onMessageLongClick(message, it)
                true
            }
        }
    }
}