package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.Pet
import com.nvv.petber.databinding.ItemFollowUserBinding
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.visible

class FollowPetAdapter(
    private val onItemClick: (String) -> Unit,
    private val onUnfollowClick: (String) -> Unit,
) : ListAdapter<Pet, FollowPetAdapter.PetViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val binding =
            ItemFollowUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PetViewHolder(private val binding: ItemFollowUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(pet: Pet) {
            binding.tvFullName.text = pet.name
            binding.tvUsername.text = pet.breed ?: pet.species
            binding.ivAvatar.loadAvatar(pet.avatarUrl)

            binding.btnFollow.visible()
            binding.btnFollow.text = binding.root.context.getString(R.string.unfollow)
            binding.btnFollow.setBackgroundResource(R.drawable.bg_button_following)
            binding.btnFollow.setTextColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.text_color
                )
            )

            binding.btnFollow.setOnClickListener { onUnfollowClick(pet.id) }
            binding.root.setOnClickListener { onItemClick(pet.id) }
        }
    }

    class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pet, newItem: Pet) = oldItem == newItem
    }
}