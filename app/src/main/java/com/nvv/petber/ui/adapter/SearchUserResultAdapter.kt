package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.repo.remote.UserSearchResult
import com.nvv.petber.databinding.ItemUserSearchResultBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar

class SearchUserResultAdapter(
    private val onClick: (UserSearchResult) -> Unit,
    private val onFollowClick: (UserSearchResult) -> Unit
) :
    ListAdapter<UserSearchResult, SearchUserResultAdapter.ViewHolder>(DiffCallback) {

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
        val result = getItem(position)
        val item = result.user
        holder.binding.apply {
            tvName.text =
                item.fullName ?: holder.itemView.context.getString(R.string.petber_user)

            if(item.bio.isNullOrEmpty()) tvSubInfo.gone()
            tvSubInfo.text = item.bio

            ivAvatar.loadAvatar(item.avatarUrl)

            if (result.isFollowing) {
                btnFollow.text = holder.itemView.context.getString(R.string.unfollow)
                btnFollow.setBackgroundColor(
                    holder.itemView.context.resources.getColor(R.color.gray_light)
                )
            } else {
                btnFollow.text = holder.itemView.context.getString(R.string.follow)
                btnFollow.setBackgroundColor(
                    holder.itemView.context.resources.getColor(R.color.bg_btn)
                )
            }

            root.setOnClickListener { onClick(result) }
            btnFollow.setOnClickListener { onFollowClick(result) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<UserSearchResult>() {
        override fun areItemsTheSame(old: UserSearchResult, new: UserSearchResult) =
            old.user.id == new.user.id

        override fun areContentsTheSame(old: UserSearchResult, new: UserSearchResult) = old == new
    }
}