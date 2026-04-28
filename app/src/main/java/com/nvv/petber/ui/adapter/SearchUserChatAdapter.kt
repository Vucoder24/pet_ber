package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.ConversationEntity
import com.nvv.petber.utils.ext.loadAvatar

class SearchUserChatAdapter(private val onClick: (ConversationEntity) -> Unit) :
    ListAdapter<ConversationEntity, SearchUserChatAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.ivAvatar)
        val tvName: TextView = view.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_user_chat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.tvName.text = item.otherUserName
        holder.ivAvatar.loadAvatar(item.otherUserAvatar)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    object DiffCallback : DiffUtil.ItemCallback<ConversationEntity>() {
        override fun areItemsTheSame(old: ConversationEntity, new: ConversationEntity) = old.otherUserId == new.otherUserId
        override fun areContentsTheSame(old: ConversationEntity, new: ConversationEntity) = old == new
    }
}