package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.PetSearchResult
import com.nvv.petber.databinding.ItemPetSearchResultBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadImage

class SearchPetResultAdapter(
    private val onClick: (PetSearchResult) -> Unit,
    private val onFollowClick: (PetSearchResult) -> Unit
) :
    ListAdapter<PetSearchResult, SearchPetResultAdapter.ViewHolder>(DiffCallback) {

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
        val result = getItem(position)
        val item = result.pet
        holder.binding.apply {
            tvPetName.text = item.name
            if(item.breed.isNullOrEmpty()) tvPetBreed.gone()
            tvPetBreed.text = item.breed
            item.avatarUrl?.let {
                ivPetAvatar.loadImage(it)
            }

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

    object DiffCallback : DiffUtil.ItemCallback<PetSearchResult>() {
        override fun areItemsTheSame(old: PetSearchResult, new: PetSearchResult) = old.pet.id == new.pet.id
        override fun areContentsTheSame(old: PetSearchResult, new: PetSearchResult) = old == new
    }
}