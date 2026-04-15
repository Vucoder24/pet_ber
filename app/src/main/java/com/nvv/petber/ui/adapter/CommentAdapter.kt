package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.CommentUI
import com.nvv.petber.databinding.ItemCommentBinding
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.formatSocialCount
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.visible

class CommentAdapter(
    private val postAuthorId: String,
    private val onReplyClick: (CommentUI) -> Unit,
    private val onLikeClick: (CommentUI) -> Unit,
    private val onToggleReplies: (CommentUI) -> Unit
) :
    ListAdapter<CommentUI, CommentAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(uiModel: CommentUI) {
            val comment = uiModel.comment

            binding.tvUsername.text =
                comment.users?.fullName ?: itemView.context.getString(R.string.petber_user)
            binding.tvContent.text = comment.content
            binding.ivAvatar.loadAvatar(comment.users?.avatarUrl)
            comment.createdAt?.let {
                binding.tvTime.text = TimeUtils.formatTimeAgo(itemView.context, it)
            }

            if (comment.userId == postAuthorId) {
                binding.tvAuthorBadge.visible()
            } else {
                binding.tvAuthorBadge.gone()
            }

            // Like
            binding.tvLikeCount.text = comment.likeCount.formatSocialCount()
            if (comment.isLiked) {
                binding.ivLike.setImageResource(R.drawable.ic_liked)
                binding.ivLike.setColorFilter(itemView.context.getColor(R.color.red_500))
            } else {
                binding.ivLike.setImageResource(R.drawable.ic_like)
                binding.ivLike.setColorFilter(itemView.context.getColor(R.color.gray_400))
            }

            // Indentation handling is based on depth
            val maxDepth = 2
            val actualDepth = if (uiModel.depth > maxDepth) maxDepth else uiModel.depth
            val paddingStart = actualDepth * 40 // 40dp per level
            val dpToPx = (paddingStart * itemView.context.resources.displayMetrics.density).toInt()

            val params = binding.indentContainer.layoutParams as ConstraintLayout.LayoutParams
            params.marginStart = dpToPx
            binding.indentContainer.layoutParams = params

            // Handle the See more / Hide answer button
            if (uiModel.hasReplies) {
                binding.tvToggleReplies.visible()
                if (uiModel.isExpanded) {
                    binding.tvToggleReplies.text =
                        binding.root.context.getString(R.string.hide_answers)
                } else {
                    binding.tvToggleReplies.text =
                        binding.root.context.getString(R.string.see_x_answers, uiModel.replyCount)
                }
            } else {
                binding.tvToggleReplies.gone()
            }

            // Click Listeners
            binding.tvReply.setOnClickListener { onReplyClick(uiModel) }
            binding.ivLike.setOnClickListener { onLikeClick(uiModel) }
            binding.tvToggleReplies.setOnClickListener { onToggleReplies(uiModel) }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<CommentUI>() {
        override fun areItemsTheSame(oldItem: CommentUI, newItem: CommentUI) =
            oldItem.comment.id == newItem.comment.id

        override fun areContentsTheSame(oldItem: CommentUI, newItem: CommentUI) = oldItem == newItem
    }
}