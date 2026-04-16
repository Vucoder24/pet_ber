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
import com.nvv.petber.data.model.Story
import com.nvv.petber.utils.ext.loadAvatar

class StoryAdapter(
    private val onStoryClick: (Story) -> Unit
) : ListAdapter<Story, StoryAdapter.StoryViewHolder>(StoryDiffCallback()) {
    var originalStories: List<Story> = emptyList()
        private set

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_story, parent, false)
        return StoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun submitList(list: List<Story>?) {
        originalStories = list ?: emptyList()
        super.submitList(list?.distinctBy { it.userId })
    }

    inner class StoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivStoryAvatar: ImageView = itemView.findViewById(R.id.imgStoryAvatar)
        private val tvStoryUsername: TextView = itemView.findViewById(R.id.tvUserName)

        fun bind(story: Story) {
            tvStoryUsername.text = story.users?.username

            ivStoryAvatar.loadAvatar(story.users?.avatarUrl?.ifEmpty { null })

            itemView.setOnClickListener { onStoryClick(story) }
        }
    }

    private class StoryDiffCallback : DiffUtil.ItemCallback<Story>() {
        override fun areItemsTheSame(oldItem: Story, newItem: Story) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Story, newItem: Story) = oldItem == newItem
    }
}