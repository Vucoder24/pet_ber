package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private val onLikeClick: (Post) -> Unit,
    private val onCommentClick: (Post) -> Unit,
    private val onShareClick: (Post) -> Unit,
    private val onSaveClick: (Post) -> Unit,
    private val onProfileClick: (Post) -> Unit,
    private val onLoadMore: () -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {
    private val activePlayers = mutableListOf<ExoPlayer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.releasePlayer()
    }

    override fun onViewDetachedFromWindow(holder: PostViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.pausePlayer()
    }


    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
        // Trigger load more when near end
        if (position >= itemCount - 2) {
            onLoadMore()
        }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icPlay1: ImageView = itemView.findViewById(R.id.icPlay1)
        private val icPlay2: ImageView = itemView.findViewById(R.id.icPlay2)
        private val icPlay3: ImageView = itemView.findViewById(R.id.icPlay3)
        private val icPlay4: ImageView = itemView.findViewById(R.id.icPlay4)
        private val ivUserAvatar: ImageView = itemView.findViewById(R.id.imgUser)
        private val tvUsername: TextView = itemView.findViewById(R.id.tvUserName)
        private val petName: TextView = itemView.findViewById(R.id.pet_name)
        private val hashtags: TextView = itemView.findViewById(R.id.hashtags)
        private val ibLike: ImageView = itemView.findViewById(R.id.btn_like)
        private val ibComment: ImageView = itemView.findViewById(R.id.btn_cmt)
        private val ibShare: ImageView = itemView.findViewById(R.id.btn_share)
        private val ibSave: ImageView = itemView.findViewById(R.id.btn_bookmark)
        private val tvLikeCount: TextView = itemView.findViewById(R.id.tvLikeCount)
        private val tvCaption: TextView = itemView.findViewById(R.id.caption)
        private val tvCommentCount: TextView = itemView.findViewById(R.id.tvCmtCount)
        private val tvShareCount: TextView = itemView.findViewById(R.id.tvShareCount)
        private val tvTimeAgo: TextView = itemView.findViewById(R.id.tvTimeAgo)
        private val ivSingleImage: ImageView = itemView.findViewById(R.id.ivSingleImage)
        private val layoutMediaGrid: ConstraintLayout = itemView.findViewById(R.id.layoutMediaGrid)
        private val ivGrid1: ImageView = itemView.findViewById(R.id.ivGrid1)
        private val ivGrid2: ImageView = itemView.findViewById(R.id.ivGrid2)
        private val ivGrid3: ImageView = itemView.findViewById(R.id.ivGrid3)
        private val ivGrid4: ImageView = itemView.findViewById(R.id.ivGrid4)
        private val overlayMore: View = itemView.findViewById(R.id.overlayMore)
        private val tvMoreCount: TextView = itemView.findViewById(R.id.tvMoreCount)
        private val pbLoadingSingle: ProgressBar = itemView.findViewById(R.id.pbLoadingSingle)
        private val playerViewSingle: PlayerView = itemView.findViewById(R.id.playerViewSingle)
        private val icPlaySingle: ImageView = itemView.findViewById(R.id.icPlaySingle)
        private var exoPlayer: ExoPlayer? = null

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
            tvLikeCount.text = post.likeCount.formatSocialCount()

            if (post.hashtags.isNullOrEmpty()) {
                hashtags.gone()
            } else {
                hashtags.text = post.hashtags
                hashtags.visible()
            }
            tvCaption.text = post.caption?.ifEmpty { "" }
            tvCaption.visibility =
                if (post.caption?.isNotEmpty() == true) View.VISIBLE else View.GONE
            tvCommentCount.text = post.commentCount.formatSocialCount()

            tvShareCount.text = post.shareCount.formatSocialCount()

            post.createdAt?.let {
                tvTimeAgo.text = TimeUtils.formatTimeAgo(itemView.context, it)
            }
            // Like state
            ibLike.setImageResource(
                if (post.isLiked) R.drawable.ic_liked else R.drawable.ic_like
            )

            // Avatar
            ivUserAvatar.loadAvatar(post.users?.avatarUrl)

            releasePlayer() // Reset player old
            val mediaList = post.postMedia ?: emptyList()

            playerViewSingle.gone()
            icPlaySingle.gone()
            ivSingleImage.gone()
            layoutMediaGrid.gone()

            if (mediaList.isNotEmpty()) {
                if (mediaList.size == 1) {
                    val media = mediaList[0]
                    if (media.mediaType.lowercase().contains("video")) {
                        playerViewSingle.visible()
                        icPlaySingle.visible()
                        setupVideoPlayer(media.mediaUrl)
                    } else {
                        ivSingleImage.visible()
                        ivSingleImage.loadImage(media.mediaUrl)
                        ivSingleImage.setSafeOnClickListener {
                            val fragmentActivity = itemView.context.getFragmentActivity()
                                ?: return@setSafeOnClickListener
                            val uniqueTag = "image_fullscreen_${System.currentTimeMillis()}"

                            try {
                                MediaFullscreenDialog.newImageInstance(media.mediaUrl)
                                    .show(fragmentActivity.supportFragmentManager, uniqueTag)
                            } catch (e: IllegalStateException) {
                                e.printStackTrace()
                            }
                        }
                    }

                } else {
                    layoutMediaGrid.visible()
                    setupMediaGrid(mediaList)

                    ivGrid1.setOnClickListener { openMediaViewer(mediaList, 0) }
                    ivGrid2.setOnClickListener { openMediaViewer(mediaList, 1) }
                    ivGrid3.setOnClickListener { openMediaViewer(mediaList, 2) }
                    ivGrid4.setOnClickListener { openMediaViewer(mediaList, 3) }
                }
            }

            // Listeners
            ibLike.setOnClickListener { onLikeClick(post) }
            ibComment.setOnClickListener { onCommentClick(post) }
            ibShare.setOnClickListener { onShareClick(post) }
            ibSave.setOnClickListener { onSaveClick(post) }
            ivUserAvatar.setOnClickListener { onProfileClick(post) }
            tvUsername.setOnClickListener { onProfileClick(post) }
        }

        private fun setupVideoPlayer(url: String) {
            exoPlayer = ExoPlayer.Builder(itemView.context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                repeatMode = ExoPlayer.REPEAT_MODE_ALL
                prepare()
                playWhenReady = false
            }
            playerViewSingle.player = exoPlayer
            exoPlayer?.let { activePlayers.add(it) }

            // Update play/pause icon according to status
            exoPlayer?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (isPlaying) {
                        icPlaySingle.gone()
                        pbLoadingSingle.gone()
                    } else {
                        if (exoPlayer?.playbackState == Player.STATE_READY) {
                            icPlaySingle.visible()
                            pbLoadingSingle.gone()
                        }
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_BUFFERING -> {
                            // Loading video -> Show Loading, Hide Play
                            pbLoadingSingle.visible()
                            icPlaySingle.gone()
                        }
                        Player.STATE_READY -> {
                            // Download completed -> Hide Loading, Show Play (if stopped)
                            pbLoadingSingle.gone()
                            if (exoPlayer?.isPlaying == false) {
                                icPlaySingle.visible()
                            }
                        }
                        Player.STATE_ENDED, Player.STATE_IDLE -> {
                            pbLoadingSingle.gone()
                            icPlaySingle.visible()
                        }
                    }
                }
            })

            // Click the play icon → play the video in place
            icPlaySingle.setSafeOnClickListener {
                exoPlayer?.play()
            }

            // Click PlayerView → open fullscreen dialog, reuse player
            playerViewSingle.setSafeOnClickListener {
                openVideoFullscreen()
            }
        }

        private fun openVideoFullscreen() {
            val fragment = itemView.context.getFragmentActivity() ?: return

            // Detach the player from the small playerView first
            val player = exoPlayer ?: return
            playerViewSingle.player = null

            val dialog = MediaFullscreenDialog.newVideoInstance(player)
            dialog.onDismissCallback = {
                // Returns the player to the small playerView
                playerViewSingle.player = exoPlayer
            }
            val uniqueTag = "video_fullscreen_${System.currentTimeMillis()}"
            try {
                dialog.show(fragment.supportFragmentManager, uniqueTag)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                playerViewSingle.player = exoPlayer
            }
        }

        @SuppressLint("SetTextI18n")
        private fun setupMediaGrid(mediaList: List<PostMedia>) {
            val views = listOf(ivGrid1, ivGrid2, ivGrid3, ivGrid4)
            val playIcons = listOf(icPlay1, icPlay2, icPlay3, icPlay4)
            playIcons.forEach { it.gone() }
            views.forEach { it.gone() }
            overlayMore.gone()

            val displayCount = minOf(mediaList.size, 4)
            for (i in 0 until displayCount) {
                views[i].visible()
                views[i].loadImage(mediaList[i].mediaUrl)
                if (mediaList[i].mediaType.lowercase().contains("video")) {
                    playIcons[i].visible()
                }
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

        fun releasePlayer() {
            exoPlayer?.let {
                activePlayers.remove(it)
                it.release()
            }
            exoPlayer = null
            playerViewSingle.player = null
        }
        fun pausePlayer() {
            exoPlayer?.playWhenReady = false
            exoPlayer?.pause()
        }
    }

    fun pauseAllPlayers() {
        activePlayers.forEach { it.pause() }
    }
    fun releaseAllPlayers() {
        activePlayers.forEach { it.release() }
        activePlayers.clear()
    }

    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
    }
}