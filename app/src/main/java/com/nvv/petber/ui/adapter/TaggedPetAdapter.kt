package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import com.nvv.petber.R
import com.nvv.petber.data.model.Pet
import com.nvv.petber.utils.ext.loadAvatar

class TaggedPetAdapter(
    private val onClick: (Pet) -> Unit
) : ListAdapter<Pet, TaggedPetAdapter.ViewHolder>(PetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_tagged_pet, parent, false)
        return ViewHolder(view as ViewGroup)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pet = getItem(position)
        holder.bind(pet)
    }

    inner class ViewHolder(itemView: ViewGroup) : RecyclerView.ViewHolder(itemView) {
        private val imgPetAvatar: ShapeableImageView = itemView.findViewById(R.id.imgPetAvatar)

        fun bind(pet: Pet) {
            imgPetAvatar.loadAvatar(pet.avatarUrl)
            itemView.setOnClickListener { onClick(pet) }
        }
    }

    private class PetDiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Pet, newItem: Pet): Boolean {
            return oldItem == newItem
        }
    }
}