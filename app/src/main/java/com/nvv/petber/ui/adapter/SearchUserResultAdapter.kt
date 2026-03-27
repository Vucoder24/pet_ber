package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.User
import com.nvv.petber.databinding.ItemUserSearchResultBinding
import com.nvv.petber.utils.ext.loadAvatar

class SearchUserResultAdapter(private val onClick: (User) -> Unit) :
    ListAdapter<User, SearchUserResultAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemUserSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemUserSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvName.text =
                item.fullName ?: holder.itemView.context.getString(R.string.petber_user)
            tvSubInfo.text = item.bio

            ivAvatar.loadAvatar(item.avatarUrl)

            root.setOnClickListener { onClick(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(old: User, new: User) = old.id == new.id
        override fun areContentsTheSame(old: User, new: User) = old == new
    }
}