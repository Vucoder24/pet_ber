package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.User
import com.nvv.petber.databinding.ItemFollowUserBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar

class PetFollowerUserAdapter(
    private val onUserClick: (User) -> Unit
) : ListAdapter<User, PetFollowerUserAdapter.UserViewHolder>(
    UserDiffCallback()
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding =
            ItemFollowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(private val binding: ItemFollowUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(user: User) {
            binding.tvFullName.text = if (user.fullName.isNullOrEmpty()){
                binding.root.context.getString(R.string.petber_user)
            } else user.fullName

            binding.tvUsername.text = "@${user.username}"
            binding.ivAvatar.loadAvatar(user.avatarUrl)

            binding.btnFollow.gone()

            binding.root.setOnClickListener { onUserClick(user) }
        }
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(
            old: User,
            new: User
        ) = old.id == new.id

        override fun areContentsTheSame(
            old: User,
            new: User
        ) = old == new
    }
}