package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.loadImage
import com.nvv.petber.utils.ext.visible

class PostAdapter(
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onProfileClick: (Post) -> Unit,
    private val onLoadMore: () -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
        // Trigger load more when near end
        if (position >= itemCount - 2) {
            onLoadMore()
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.imgUser)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUserName)
        private val petName: TextView = itemView.findViewById(R.id.pet_name)
        private val hashtags: TextView = itemView.findViewById(R.id.hashtags)
        private val ivPostImage: ImageView = itemView.findViewById(R.id.ivPostImage)
        private val ibLike: ImageView = itemView.findViewById(R.id.btn_like)
        private val ibComment: ImageView = itemView.findViewById(R.id.btn_cmt)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCaption: TextView = itemView.findViewById(R.id.caption)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCmtCount)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)

        @SuppressLint("SetTextI18n")
        fun bind(post: Post) {
            tvUsername.text =
                post.users?.fullName ?: itemView.context.getString(R.string.petber_user)

            if (post.pets == null) {
                petName.gone()
            } else {
                petName.visible()
            }
            petName.text = "${post.pets?.name} (${post.pets?.breed})"
            tvLikeCount.text = post.likeCount.toString()
            if (post.hashtags.isNullOrEmpty()) {
                hashtags.gone()
            } else {
                hashtags.text = post.hashtags
                hashtags.visible()
            }
            tvCaption.text = post.caption?.ifEmpty { "" }
            tvCaption.visibility =
                if (post.caption?.isNotEmpty() == true) View.VISIBLE else View.GONE
            tvCommentCount.text = post.commentCount.toString()
            post.createdAt?.let { tvTimeAgo.text = TimeUtils.formatTimeAgo(it) }

            // Avatar
            ivUserAvatar.loadAvatar(post.users?.avatarUrl)

            val firstMediaUrl = post.postMedia?.firstOrNull()?.mediaUrl
            ivPostImage.loadImage(firstMediaUrl)

            // Like state
            ibLike.setImageResource(
                if (post.isLiked) R.drawable.ic_liked else R.drawable.ic_like
            )

            // Listeners
            ibLike.setOnClickListener { onLikeClick(post) }
            ibComment.setOnClickListener { onCommentClick(post) }
//            ibShare.setOnClickListener { onShareClick(post) }
            ivUserAvatar.setOnClickListener { onProfileClick(post) }
            tvUsername.setOnClickListener { onProfileClick(post) }
        }

    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}