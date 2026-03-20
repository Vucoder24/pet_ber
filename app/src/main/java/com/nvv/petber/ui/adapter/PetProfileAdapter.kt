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
import com.nvv.petber.data.model.Pet
import com.nvv.petber.utils.loadAvatar

class PetProfileAdapter(
    private val onAddClick: () -> Unit,
    private val onClick: (Pet) -> Unit
) : ListAdapter<Pet, RecyclerView.ViewHolder>(StoryDiffCallback()) {

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_PET = 1
    }

    override fun getItemCount(): Int {
        val count = super.getItemCount()
        return if (count >= 0) count + 1 else 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_ADD else TYPE_PET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_add_pet, parent, false)
            AddPetViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pet_profile, parent, false)
            PetProfileViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        if (holder is PetProfileViewHolder) {
            holder.bind(getItem(position - 1))
        } else if (holder is AddPetViewHolder) {
            holder.itemView.setOnClickListener { onAddClick() }
        }
    }

    inner class PetProfileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPetAvatar: ImageView = itemView.findViewById(R.id.imgPetAvatar)
        private val tvPetName: TextView = itemView.findViewById(R.id.tvPetName)

        fun bind(pet: Pet) {
            tvPetName.text = pet.name
            ivPetAvatar.loadAvatar(pet.avatarUrl)

            itemView.setOnClickListener { onClick(pet) }
        }
    }

    inner class AddPetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)


    private class StoryDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Pet, newItem: Pet) = oldItem == newItem
    }
}