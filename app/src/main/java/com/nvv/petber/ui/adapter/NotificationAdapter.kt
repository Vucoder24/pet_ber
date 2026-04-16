package com.nvv.petber.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.Notification
import com.nvv.petber.databinding.ItemNotificationBinding
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.loadAvatar

class NotificationAdapter(
    private val onClick: (Notification) -> Unit,
    private val onMoreClick: (Notification) -> Unit
) : ListAdapter<Notification, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
            // 1. Gán nội dung
            binding.tvMessage.text = notification.message ?: "You have a new announcement"
            notification.createdAt?.let {
                binding.tvTime.text = TimeUtils.formatTimeAgo(binding.root.context, it)
            }

            if (notification.isRead) {
                binding.tvMessage.setTypeface(null, Typeface.NORMAL)
                binding.rootLayout.setBackgroundColor(Color.TRANSPARENT)
            } else {
                binding.tvMessage.setTypeface(null, Typeface.BOLD)
                binding.rootLayout.background =
                    ContextCompat.getDrawable(binding.root.context, R.color.unread_notification_bg)
            }

            binding.ivAvatar.loadAvatar(notification.avatarUrl)

            binding.root.setOnClickListener {
                onClick(notification)
            }

            binding.ivMore.setOnClickListener {
                onMoreClick(notification)
            }
        }
    }
}

class NotificationDiffCallback : DiffUtil.ItemCallback<Notification>() {
    override fun areItemsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Notification, newItem: Notification): Boolean {
        return oldItem == newItem
    }
}