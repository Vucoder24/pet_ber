package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.data.model.Post
import com.nvv.petber.databinding.ItemMiniPostBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadImage
import com.nvv.petber.utils.ext.visible

class MiniPostAdapter(
    private val onClickMiniPost: (Post) -> Unit
) :
    ListAdapter<Post, MiniPostAdapter.MiniPostViewHolder>(MiniPostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MiniPostViewHolder {
        val binding =
            ItemMiniPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MiniPostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MiniPostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MiniPostViewHolder(private val binding: ItemMiniPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            val mediaList = post.postMedia
            if (!mediaList.isNullOrEmpty()) {
                val firstMedia = mediaList[0]
                binding.ivThumbnail.loadImage(firstMedia.mediaUrl)

                if (firstMedia.mediaType.lowercase().contains("video")) {
                    binding.icPlay.visible()
                } else {
                    binding.icPlay.gone()
                }
            } else {
                binding.ivThumbnail.setImageDrawable(null)
                binding.icPlay.gone()
            }
            binding.root.setOnClickListener { onClickMiniPost(post) }
        }
    }

    class MiniPostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean = oldItem == newItem
    }
}