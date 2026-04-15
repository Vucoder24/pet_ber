package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.FollowUserUI
import com.nvv.petber.databinding.ItemFollowUserBinding
import com.nvv.petber.utils.ext.loadAvatar

class FollowUserAdapter(
    private val currentUserId: String,
    private val onFollowClick: (FollowUserUI) -> Unit,
    private val onItemClick: (String) -> Unit
) : ListAdapter<FollowUserUI, FollowUserAdapter.FollowViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowViewHolder {
        val binding =
            ItemFollowUserBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return FollowViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FollowViewHolder(private val binding: ItemFollowUserBinding) :
        RecyclerView.ViewHolder(binding.root) { 
        fun bind(item: FollowUserUI) {
            val user = item.user
            binding.tvFullName.text =
                user.fullName ?: binding.root.context.getString(R.string.petber_user)
            binding.tvUsername.text = user.username
            binding.ivAvatar.loadAvatar(user.avatarUrl)

            if (user.id == currentUserId) {
                binding.btnFollow.visibility = android.view.View.GONE
            } else {
                binding.btnFollow.visibility = android.view.View.VISIBLE
                if (item.isFollowing) {
                    binding.btnFollow.text = binding.root.context.getString(R.string.following)
                    binding.btnFollow.setBackgroundResource(R.drawable.bg_button_following)
                    binding.btnFollow.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.text_color
                        )
                    )
                } else {
                    binding.btnFollow.text = binding.root.context.getString(R.string.follow)
                    binding.btnFollow.setBackgroundResource(R.drawable.bg_button_follow)
                    binding.btnFollow.setTextColor(
                        ContextCompat.getColor(
                            binding.root.context,
                            R.color.white
                        )
                    )
                }
            }

            binding.btnFollow.setOnClickListener { onFollowClick(item) }
            binding.root.setOnClickListener { onItemClick(user.id) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FollowUserUI>() {
        override fun areItemsTheSame(oldItem: FollowUserUI, newItem: FollowUserUI) =
            oldItem.user.id == newItem.user.id

        override fun areContentsTheSame(oldItem: FollowUserUI, newItem: FollowUserUI) =
            oldItem == newItem
    }
}