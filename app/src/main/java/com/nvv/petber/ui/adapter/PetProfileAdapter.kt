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
import com.nvv.petber.utils.ext.loadAvatar

class PetProfileAdapter(
    private val onAddClick: () -> Unit,
    private val onClick: (Pet) -> Unit
) : ListAdapter<PetProfileAdapter.Item, RecyclerView.ViewHolder>(ItemDiffCallback()) {

    sealed class Item {
        object Add : Item()
        data class PetData(val pet: Pet) : Item()
    }

    companion object {
        private const val TYPE_ADD = 0
        private const val TYPE_PET = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Item.Add -> TYPE_ADD
            is Item.PetData -> TYPE_PET
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ADD) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_add_pet, parent, false)
            AddPetViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pet_profile, parent, false)
            PetProfileViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Item.PetData -> (holder as PetProfileViewHolder).bind(item.pet)
            is Item.Add -> (holder as AddPetViewHolder).itemView.setOnClickListener { onAddClick() }
        }
    }

    fun submitPets(pets: List<Pet>?) {
        val list = mutableListOf<Item>(Item.Add)
        pets?.let { list.addAll(it.map { pet -> Item.PetData(pet) }) }
        submitList(list)
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

    private class ItemDiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return if (oldItem is Item.Add && newItem is Item.Add) true
            else if (oldItem is Item.PetData && newItem is Item.PetData) oldItem.pet.id == newItem.pet.id
            else false
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return oldItem == newItem
        }
    }
}