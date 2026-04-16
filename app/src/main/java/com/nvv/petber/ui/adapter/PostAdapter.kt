package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.PostMedia
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
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
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

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // UI Components
        private val icPlay1: ImageView = itemView.findViewById(R.id.icPlay1)
        private val containerMedia: MaterialCardView = itemView.findViewById(R.id.containerMedia)
        private val icPlay2: ImageView = itemView.findViewById(R.id.icPlay2)
        private val icPlay3: ImageView = itemView.findViewById(R.id.icPlay3)
        private val icPlay4: ImageView = itemView.findViewById(R.id.icPlay4)
        private val ibMore: ImageButton = itemView.findViewById(R.id.btnMore)
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.imgUser)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUserName)
        private val hashtags: TextView = itemView.findViewById(R.id.hashtags)
        private val ibLike: ImageView = itemView.findViewById(R.id.btn_like)
        private val ibComment: ImageView = itemView.findViewById(R.id.btn_cmt)
        private val ibShare: ImageView = itemView.findViewById(R.id.btn_share)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCaption: TextView = itemView.findViewById(R.id.caption)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCmtCount)
        private val tvShareCount: TextView = itemView.findViewById(R.id.tvShareCount)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        val ivSingleImage: ImageView = itemView.findViewById(R.id.ivSingleImage)
        private val layoutMediaGrid: ConstraintLayout = itemView.findViewById(R.id.layoutMediaGrid)
        val ivGrid1: ImageView = itemView.findViewById(R.id.ivGrid1)
        val ivGrid2: ImageView = itemView.findViewById(R.id.ivGrid2)
        val ivGrid3: ImageView = itemView.findViewById(R.id.ivGrid3)
        val ivGrid4: ImageView = itemView.findViewById(R.id.ivGrid4)
        private val overlayMore: View = itemView.findViewById(R.id.overlayMore)
        private val tvMoreCount: TextView = itemView.findViewById(R.id.tvMoreCount)
        private val pbLoadingSingle: ProgressBar = itemView.findViewById(R.id.pbLoadingSingle)
        private val playerViewSingle: PlayerView = itemView.findViewById(R.id.playerViewSingle)
        private val icPlaySingle: ImageView = itemView.findViewById(R.id.icPlaySingle)

        private var currentVideoUrl: String? = null

        private val playerListener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    icPlaySingle.gone()
                    pbLoadingSingle.gone()
                    ivSingleImage.gone()
                } else {
                    if (exoPlayer.playbackState == Player.STATE_READY) icPlaySingle.visible()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        pbLoadingSingle.visible(); icPlaySingle.gone()
                    }

                    Player.STATE_READY -> {
                        pbLoadingSingle.gone()
                        if (exoPlayer.playWhenReady) {
                            ivSingleImage.gone()
                        } else {
                            icPlaySingle.visible()
                        }
                    }

                    Player.STATE_ENDED, Player.STATE_IDLE -> {
                        pbLoadingSingle.gone()
                        icPlaySingle.visible()
                        ivSingleImage.visible()
                    }
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(post: Post) {
            // Bind Basic Data
            tvUsername.text =
                post.users?.fullName ?: itemView.context.getString(R.string.petber_user)
            tvLikeCount.text = post.likeCount.formatSocialCount()
            tvCommentCount.text = post.commentCount.formatSocialCount()
            tvShareCount.text = post.shareCount.formatSocialCount()
            tvCaption.text = post.caption?.ifEmpty { "" }
            tvCaption.visibility =
                if (post.caption?.isNotEmpty() == true) View.VISIBLE else View.GONE

            if (post.hashtags.isNullOrEmpty()) hashtags.gone() else {
                hashtags.text = post.hashtags; hashtags.visible()
            }
            post.createdAt?.let { tvTimeAgo.text = TimeUtils.formatTimeAgo(itemView.context, it) }
            ibLike.setImageResource(if (post.isLiked) R.drawable.ic_liked else R.drawable.ic_like)
            ivUserAvatar.loadAvatar(post.users?.avatarUrl)

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
            ibLike.setOnClickListener { onLikeClick(post) }
            ibComment.setOnClickListener { onCommentClick(post) }
            ibShare.setOnClickListener { onShareClick(post) }
            ivUserAvatar.setOnClickListener { onProfileClick(post) }
            tvUsername.setOnClickListener { onProfileClick(post) }
            ibMore.setOnClickListener { onMoreOption(post) }
        }

        private fun resetMediaVisibility() {
            playerViewSingle.gone()
            icPlaySingle.gone()
            ivSingleImage.gone()
            layoutMediaGrid.gone()
            pbLoadingSingle.gone()
            playerViewSingle.player = null
        }

        private fun setupVideoListeners() {
            icPlaySingle.setSafeOnClickListener { playThisVideo() }
            playerViewSingle.setSafeOnClickListener {
                if (playerViewSingle.player == exoPlayer && exoPlayer.isPlaying) {
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
            playerViewSingle.visible()
            playerViewSingle.player = exoPlayer
            exoPlayer.addListener(playerListener)

            val mediaItem = MediaItem.fromUri(url)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }

        fun detachPlayer() {
            if (playerViewSingle.player == exoPlayer) {
                exoPlayer.removeListener(playerListener)
                playerViewSingle.player = null
            }

            playerViewSingle.gone()
            if (currentVideoUrl != null) {
                ivSingleImage.visible()
                icPlaySingle.visible()
            }

            currentVideoUrl = null
        }

        fun pausePlayer() {
            if (playerViewSingle.player == exoPlayer) {
                exoPlayer.pause()
            }
        }

        private fun openVideoFullscreen() {
            val fragment = itemView.context.getFragmentActivity() ?: return
            playerViewSingle.player = null // Tạm gỡ để Dialog sử dụng

            val dialog = MediaFullscreenDialog.newVideoInstance(exoPlayer)
            dialog.onDismissCallback = {
                playerViewSingle.player = exoPlayer
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
            val views = listOf(ivGrid1, ivGrid2, ivGrid3, ivGrid4)
            val playIcons = listOf(icPlay1, icPlay2, icPlay3, icPlay4)
            views.forEach { it.gone() }
            playIcons.forEach { it.gone() }
            overlayMore.gone()

            val displayCount = minOf(mediaList.size, 4)
            for (i in 0 until displayCount) {
                views[i].visible()
                views[i].loadImage(mediaList[i].mediaUrl)
                if (mediaList[i].mediaType.lowercase().contains("video")) playIcons[i].visible()
                views[i].setOnClickListener { openMediaViewer(mediaList, i) }
            }

            if (mediaList.size > 4) {
                overlayMore.visible()
                tvMoreCount.text = "+${mediaList.size - 4}"
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