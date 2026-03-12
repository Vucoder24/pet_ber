package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.data.model.Pet
import com.nvv.petber.databinding.ItemPetSearchResultBinding
import com.nvv.petber.utils.loadImage

class SearchPetResultAdapter(private val onClick: (Pet) -> Unit) :
    ListAdapter<Pet, SearchPetResultAdapter.ViewHolder>(DiffCallback) {

    class ViewHolder(val binding: ItemPetSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemPetSearchResultBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvPetName.text = item.name
            tvPetBreed.text = item.breed
            item.avatarUrl?.let {
                ivPetAvatar.loadImage(it)
            }
            root.setOnClickListener { onClick(item) }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<Pet>() {
        override fun areItemsTheSame(old: Pet, new: Pet) = old.id == new.id
        override fun areContentsTheSame(old: Pet, new: Pet) = old == new
    }
}