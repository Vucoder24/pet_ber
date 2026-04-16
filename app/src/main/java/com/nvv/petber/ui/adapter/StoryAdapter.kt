package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.nvv.petber.R
import com.nvv.petber.data.model.Story
import com.nvv.petber.utils.ext.loadAvatar

class StoryAdapter(
    private val onStoryClick: (Story) -> Unit
) : ListAdapter<StoryAdapter.StoryItem, RecyclerView.ViewHolder>(StoryDiffCallback()) {
    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_LOADING = 1
    }

    sealed class StoryItem {
        data class Data(val story: Story) : StoryItem()
        object Loading : StoryItem()
    }

    var originalStories: List<Story> = emptyList()
        private set

    fun submitStoryData(list: List<Story>?, isLoadingMore: Boolean) {
        originalStories = list ?: emptyList()
        val distinctList = list?.distinctBy { it.userId } ?: emptyList()

        val items = mutableListOf<StoryItem>()
        items.addAll(distinctList.map { StoryItem.Data(it) })

        if (isLoadingMore) {
            items.add(StoryItem.Loading)
        }

        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is StoryItem.Data -> TYPE_ITEM
            is StoryItem.Loading -> TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ITEM -> {
                val view = inflater.inflate(R.layout.item_story, parent, false)
                StoryViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_story_load_more_shimmer, parent, false)
                LoadingViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is StoryViewHolder && item is StoryItem.Data) {
            holder.bind(item.story)
        } else if (holder is LoadingViewHolder) {
            holder.binding.startShimmer()
        }
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
    inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ShimmerFrameLayout = itemView.findViewById(R.id.shimmerStoryLoadMore)
    }

    private class StoryDiffCallback : DiffUtil.ItemCallback<StoryItem>() {
        override fun areItemsTheSame(oldItem: StoryItem, newItem: StoryItem): Boolean {
            return if (oldItem is StoryItem.Data && newItem is StoryItem.Data) {
                oldItem.story.id == newItem.story.id
            } else {
                oldItem is StoryItem.Loading && newItem is StoryItem.Loading
            }
        }

        override fun areContentsTheSame(oldItem: StoryItem, newItem: StoryItem): Boolean {
            return oldItem == newItem
        }
    }
}