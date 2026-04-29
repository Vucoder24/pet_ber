package com.nvv.petber.ui.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.Conversation
import com.nvv.petber.databinding.ItemConversationBinding
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.visible

class ConversationAdapter(
    private val currentUserId: String,
    private val onClick: (Conversation) -> Unit,
    private val onLongClick: (Conversation) -> Unit,
    private val updateIsSeen: (String) -> Unit
) : ListAdapter<Conversation, ConversationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Conversation) {
            val isUnread = !item.isSeen && item.lastMessageSenderId != currentUserId
            val typeface = if (isUnread) Typeface.BOLD else Typeface.NORMAL

            binding.tvLastMessage.setTypeface(null, typeface)
            binding.tvTime.setTypeface(null, typeface)
            binding.tvFullName.setTypeface(null, typeface)

            binding.tvFullName.text = if (item.otherUserName.isNullOrEmpty()) {
                itemView.context.getString(R.string.petber_user)
            } else item.otherUserName

            val context = binding.root.context
            val isMe = item.lastMessageSenderId == currentUserId

            val message = when (item.lastMessageMediaType) {
                "image" -> context.getString(R.string.msg_sent_image)
                "video" -> context.getString(R.string.msg_sent_video)
                else -> {
                    if (item.lastMessageContent?.contains("DELETED_MSG_PETBER") == true) {
                        if (isMe) {
                            context.getString(R.string.you_deleted_msg)
                        } else {
                            context.getString(R.string.other_user_deleted_msg)
                        }
                    } else {
                        item.lastMessageContent ?: ""
                    }
                }
            }

            binding.tvLastMessage.text =
                if (isMe && message.isNotEmpty())
                    context.getString(R.string.you_prefix, message)
                else message

            binding.ivAvatar.loadAvatar(item.otherUserAvatar)
            item.lastMessageAt?.let {
                binding.tvTime.visible()
                binding.tvTime.text = TimeUtils.formatMessageTime(it)
            } ?: {
                binding.tvTime.gone()
            }


            binding.root.setOnClickListener {
                if (isUnread) updateIsSeen(item.conversationId)
                onClick(item)
            }
            binding.root.setOnLongClickListener {
                onLongClick(item)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemConversationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Conversation>() {
            override fun areItemsTheSame(oldItem: Conversation, newItem: Conversation) =
                oldItem.conversationId == newItem.conversationId

            override fun areContentsTheSame(
                oldItem: Conversation,
                newItem: Conversation
            ) = oldItem == newItem
        }
    }
}