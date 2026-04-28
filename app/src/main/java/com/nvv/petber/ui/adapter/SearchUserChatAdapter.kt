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
import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.utils.ext.loadAvatar

class SearchUserChatAdapter(private val onClick: (ConversationModel) -> Unit) :
    ListAdapter<ConversationModel, SearchUserChatAdapter.ViewHolder>(DiffCallback) {

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

    object DiffCallback : DiffUtil.ItemCallback<ConversationModel>() {
        override fun areItemsTheSame(old: ConversationModel, new: ConversationModel) = old.otherUserId == new.otherUserId
        override fun areContentsTheSame(old: ConversationModel, new: ConversationModel) = old == new
    }
}