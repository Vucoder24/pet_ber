package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.GroupedStoryReaction
import com.nvv.petber.databinding.ItemStoryReactionBinding
import com.nvv.petber.utils.ext.loadAvatar

class StoryReactionAdapter(
    private val onUserClick: (String) -> Unit
) : ListAdapter<GroupedStoryReaction, StoryReactionAdapter.ViewHolder>(ReactionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemStoryReactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemStoryReactionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GroupedStoryReaction) {
            binding.tvName.text =
                item.user?.fullName ?: binding.root.context.getString(R.string.petber_user)
            binding.ivAvatar.loadAvatar(item.user?.avatarUrl)

            binding.llReactions.removeAllViews()

            val iconSize = (24 * itemView.resources.displayMetrics.density).toInt()
            val margin = (4 * itemView.resources.displayMetrics.density).toInt()

            item.reactions.take(5).forEach { reactionType ->
                val imageView = ImageView(itemView.context).apply {
                    layoutParams = LinearLayout.LayoutParams(iconSize, iconSize).apply {
                        marginStart = margin
                    }
                    val reactionRes = when (reactionType) {
                        "paw" -> R.drawable.ic_react_paw
                        "cat" -> R.drawable.ic_react_cat
                        "fish" -> R.drawable.ic_react_fish
                        "yarn" -> R.drawable.ic_react_yarn
                        else -> R.drawable.ic_react_paw
                    }
                    setImageResource(reactionRes)
                }
                binding.llReactions.addView(imageView)
            }

            binding.root.setOnClickListener {
                item.user?.id?.let { userId -> onUserClick(userId) }
            }
        }
    }

    class ReactionDiffCallback : DiffUtil.ItemCallback<GroupedStoryReaction>() {
        override fun areItemsTheSame(oldItem: GroupedStoryReaction, newItem: GroupedStoryReaction) =
            oldItem.user?.id == newItem.user?.id

        override fun areContentsTheSame(
            oldItem: GroupedStoryReaction,
            newItem: GroupedStoryReaction
        ) = oldItem == newItem
    }
}
