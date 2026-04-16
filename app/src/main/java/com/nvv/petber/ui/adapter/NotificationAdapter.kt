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
import com.nvv.petber.databinding.ItemNotifLoadMoreShimmerBinding
import com.nvv.petber.databinding.ItemNotificationBinding
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.loadAvatar

class NotificationAdapter(
    private val onClick: (Notification) -> Unit,
    private val onMoreClick: (Notification) -> Unit
) : ListAdapter<Notification, RecyclerView.ViewHolder>(NotificationDiffCallback()) {

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_LOADING = 1
    }
    var isLoadMore = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    notifyItemInserted(currentList.size)
                } else {
                    notifyItemRemoved(currentList.size)
                }
            }
        }

    override fun getItemCount(): Int {
        return currentList.size + (if (isLoadMore) 1 else 0)
    }

    override fun getItemViewType(position: Int): Int {
        return if (isLoadMore && position == currentList.size) {
            TYPE_LOADING
        } else {
            TYPE_ITEM
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ITEM -> {
                val binding = ItemNotificationBinding.inflate(inflater, parent, false)
                NotificationViewHolder(binding)
            }
            else -> {
                val binding = ItemNotifLoadMoreShimmerBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is NotificationViewHolder) {
            holder.bind(getItem(position))
        }
    }

    inner class NotificationViewHolder(private val binding: ItemNotificationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification) {
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
    inner class LoadingViewHolder(val binding: ItemNotifLoadMoreShimmerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.shimmerLoadMore.startShimmer()
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