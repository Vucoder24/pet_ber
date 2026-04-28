package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.databinding.ItemConversationBinding
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.visible

class ConversationAdapter(
    private val onClick: (ConversationModel) -> Unit,
    private val onLongClick: (ConversationModel) -> Unit
) : ListAdapter<ConversationModel, ConversationAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemConversationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ConversationModel) {
            binding.tvFullName.text = if (item.otherUserName.isNullOrEmpty()){
                itemView.context.getString(R.string.petber_user)
            } else item.otherUserName

            val context = binding.root.context
            binding.tvLastMessage.text = when (item.lastMessageMediaType) {
                "image" -> context.getString(R.string.msg_sent_image)
                "video" -> context.getString(R.string.msg_sent_video)
                else -> item.lastMessageContent ?: ""
            }

            binding.ivAvatar.loadAvatar(item.otherUserAvatar)
            item.lastMessageTime?.let {
                binding.tvTime.visible()
                binding.tvTime.text = TimeUtils.formatMessageTime(it)
            }?: {
                binding.tvTime.gone()
            }


            binding.root.setOnClickListener { onClick(item) }
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
        private val DiffCallback = object : DiffUtil.ItemCallback<ConversationModel>() {
            override fun areItemsTheSame(oldItem: ConversationModel, newItem: ConversationModel) =
                oldItem.conversationId == newItem.conversationId

            override fun areContentsTheSame(
                oldItem: ConversationModel,
                newItem: ConversationModel
            ) = oldItem == newItem
        }
    }
}