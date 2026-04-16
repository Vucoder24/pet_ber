package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.PostMedia
import com.nvv.petber.databinding.ItemPostBinding
import com.nvv.petber.ui.activity.MediaViewerActivity
import com.nvv.petber.ui.dialog.MediaFullscreenDialog
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.formatSocialCount
import com.nvv.petber.utils.ext.getFragmentActivity
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.loadImage
import com.nvv.petber.utils.ext.setSafeOnClickListener
import com.nvv.petber.utils.ext.visible

class PostAdapter(
    private val exoPlayer: ExoPlayer,
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onProfileClick: (Post) -> Unit,
    private val onMoreOption: (Post) -> Unit,
    private val onLoadMore: () -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
        if (position >= itemCount - 1) {
            onLoadMore()
        }
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.detachPlayer()
    }

    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.pausePlayer()
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {
        // UI Components

        private var currentVideoUrl: String? = null

        private val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    binding.icPlaySingle.gone()
                    binding.pbLoadingSingle.gone()
                    binding.ivSingleImage.gone()
                } else {
                    if (exoPlayer.playbackState == Player.STATE_READY) binding.icPlaySingle.visible()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        binding.pbLoadingSingle.visible(); binding.icPlaySingle.gone()
                    }

                    Player.STATE_READY -> {
                        binding.pbLoadingSingle.gone()
                        if (exoPlayer.playWhenReady) {
                            binding.ivSingleImage.gone()
                        } else {
                            binding.icPlaySingle.visible()
                        }
                    }

                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        binding.pbLoadingSingle.gone()
                        binding.icPlaySingle.visible()
                        binding.ivSingleImage.visible()
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(post: Post) {
            binding.apply {
                // Bind Basic Data
                tvUserName.text =
                    post.users?.fullName ?: itemView.context.getString(R.string.petber_user)
                tvLikeCount.text = post.likeCount.formatSocialCount()
                tvCmtCount.text = post.commentCount.formatSocialCount()
                tvShareCount.text = post.shareCount.formatSocialCount()
                caption.text = post.caption?.ifEmpty { "" }
                caption.visibility =
                    if (post.caption?.isNotEmpty() == true) View.VISIBLE else View.GONE



                if (post.hashtags.isNullOrEmpty()) hashtags.gone() else {
                    hashtags.text = post.hashtags; hashtags.visible()
                }
                post.createdAt?.let {
                    tvTimeAgo.text = TimeUtils.formatTimeAgo(itemView.context, it)
                }
                btnLike.setImageResource(if (post.isLiked) R.drawable.ic_liked else R.drawable.ic_like)
                imgUser.loadAvatar(post.users?.avatarUrl)

                caption.setOnClickListener {
                    TransitionManager.beginDelayedTransition(binding.layoutContainer as ViewGroup)

                    if (binding.caption.maxLines == 3) {
                        binding.caption.maxLines = Int.MAX_VALUE
                    } else {
                        binding.caption.maxLines = 3
                    }
                }

                // Media Reset & Logic
                detachPlayer()
                val mediaList = post.postMedia ?: emptyList()
                if (mediaList.isEmpty()) containerMedia.gone() else containerMedia.visible()

                resetMediaVisibility()

                if (mediaList.isNotEmpty()) {
                    if (mediaList.size == 1) {
                        val media = mediaList[0]
                        if (media.mediaType.lowercase().contains("video")) {
                            currentVideoUrl = media.mediaUrl
                            ivSingleImage.visible()
                            playerViewSingle.visible()
                            icPlaySingle.visible()
                            ivSingleImage.loadImage(media.mediaUrl)
                            setupVideoListeners()
                        } else {
                            ivSingleImage.visible()
                            ivSingleImage.loadImage(media.mediaUrl)
                            ivSingleImage.setSafeOnClickListener { openFullscreenImage(media.mediaUrl) }
                        }
                    } else {
                        layoutMediaGrid.visible()
                        setupMediaGrid(mediaList)
                    }
                }

                // Click Listeners
                btnLike.setOnClickListener { onLikeClick(post) }
                btnCmt.setOnClickListener { onCommentClick(post) }
                btnShare.setOnClickListener { onShareClick(post) }
                imgUser.setOnClickListener { onProfileClick(post) }
                tvUserName.setOnClickListener { onProfileClick(post) }
                btnMore.setOnClickListener { onMoreOption(post) }
            }
        }

        private fun resetMediaVisibility() {
            binding.apply {
                playerViewSingle.gone()
                icPlaySingle.gone()
                ivSingleImage.gone()
                layoutMediaGrid.gone()
                pbLoadingSingle.gone()
                playerViewSingle.player = null
            }
        }

        private fun setupVideoListeners() {
            binding.icPlaySingle.setSafeOnClickListener { playThisVideo() }
            binding.playerViewSingle.setSafeOnClickListener {
                if (binding.playerViewSingle.player == exoPlayer && exoPlayer.isPlaying) {
                    openVideoFullscreen()
                } else {
                    playThisVideo()
                }
            }
        }

        private fun playThisVideo() {
            val url = currentVideoUrl ?: return

            exoPlayer.stop()
            exoPlayer.removeListener(playerListener)

            // Gán player vào view hiện tại
            binding.playerViewSingle.visible()
            binding.playerViewSingle.player = exoPlayer
            exoPlayer.addListener(playerListener)

            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }

        fun detachPlayer() {
            if (binding.playerViewSingle.player == exoPlayer) {
                exoPlayer.removeListener(playerListener)
                binding.playerViewSingle.player = null
            }

            binding.playerViewSingle.gone()
            if (currentVideoUrl != null) {
                binding.ivSingleImage.visible()
                binding.icPlaySingle.visible()
            }

            currentVideoUrl = null
        }

        fun pausePlayer() {
            if (binding.playerViewSingle.player == exoPlayer) {
                exoPlayer.pause()
            }
        }

        private fun openVideoFullscreen() {
            val fragment = itemView.context.getFragmentActivity() ?: return
            binding.playerViewSingle.player = null

            val dialog = MediaFullscreenDialog.newVideoInstance(exoPlayer)
            dialog.onDismissCallback = {
                binding.playerViewSingle.player = exoPlayer
            }
            dialog.show(
                fragment.supportFragmentManager,
                "video_fullscreen_${System.currentTimeMillis()}"
            )
        }

        private fun openFullscreenImage(url: String) {
            val fragment = itemView.context.getFragmentActivity() ?: return
            MediaFullscreenDialog.newImageInstance(url)
                .show(fragment.supportFragmentManager, "image_full_${System.currentTimeMillis()}")
        }

        @SuppressLint("SetTextI18n")
        private fun setupMediaGrid(mediaList: List<PostMedia>) {
            binding.apply {
                val views = listOf(ivGrid1, ivGrid2, ivGrid3, ivGrid4)
                val playIcons = listOf(icPlay1, icPlay2, icPlay3, icPlay4)

                views.forEach { it.gone() }
                playIcons.forEach { it.gone() }
                overlayMore.gone()

                val size = mediaList.size

                // 🔥 CASE ĐẶC BIỆT: 3 ITEM
                if (size == 3) {
                    // show 3 view
                    ivGrid1.visible()
                    ivGrid2.visible()
                    ivGrid3.visible()
                    ivGrid4.gone()

                    // 👉 set constraint động
                    val params1 = ivGrid1.layoutParams as ConstraintLayout.LayoutParams
                    val params2 = ivGrid2.layoutParams as ConstraintLayout.LayoutParams
                    val params3 = ivGrid3.layoutParams as ConstraintLayout.LayoutParams

                    // ivGrid1 chiếm full bên trái
                    params1.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    params1.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                    params1.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                    params1.endToStart = ivGrid2.id

                    // ivGrid2 (trên phải)
                    params2.topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                    params2.startToEnd = ivGrid1.id
                    params2.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    params2.bottomToTop = ivGrid3.id

                    // ivGrid3 (dưới phải)
                    params3.topToBottom = ivGrid2.id
                    params3.startToEnd = ivGrid1.id
                    params3.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                    params3.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID

                    ivGrid1.layoutParams = params1
                    ivGrid2.layoutParams = params2
                    ivGrid3.layoutParams = params3

                } else {
                    // default (1,2,4+)
                    val displayCount = minOf(size, 4)
                    for (i in 0 until displayCount) {
                        views[i].visible()
                    }
                }

                // bind data chung
                val displayCount = minOf(size, 4)
                for (i in 0 until displayCount) {
                    views[i].loadImage(mediaList[i].mediaUrl)

                    if (mediaList[i].mediaType.lowercase().contains("video")) {
                        playIcons[i].visible()
                    }

                    views[i].setOnClickListener {
                        openMediaViewer(mediaList, i)
                    }
                }

                if (size > 4) {
                    overlayMore.visible()
                    tvMoreCount.text = "+${size - 4}"
                }
            }
        }

        private fun openMediaViewer(mediaList: List<PostMedia>, startIndex: Int) {
            val intent = Intent(itemView.context, MediaViewerActivity::class.java).apply {
                putExtra(MediaViewerActivity.EXTRA_MEDIA_LIST, Gson().toJson(mediaList))
                putExtra(MediaViewerActivity.EXTRA_START_INDEX, startIndex)
            }
            itemView.context.startActivity(intent)
        }
    }

    fun pauseAllPlayers() {
        exoPlayer.pause()
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}