package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.nvv.petber.databinding.ItemCreatePostBinding
import com.nvv.petber.databinding.ItemPostBinding
import com.nvv.petber.databinding.ItemPostLoadMoreShimmerBinding
import com.nvv.petber.ui.activity.CreateEditPostActivity
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
) : ListAdapter<PostAdapter.PostItem, RecyclerView.ViewHolder>(PostDiffCallback()) {
    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_LOADING = 1
        private const val TYPE_CREATE = 3
    }

    sealed class PostItem {
        data class Data(val post: Post) : PostItem()
        object Loading : PostItem()
        object CreatePost : PostItem()
    }

    fun submitPostData(list: List<Post>?, isLoadingMore: Boolean, showCreatePost: Boolean = true) {
        val items = mutableListOf<PostItem>()

        if (showCreatePost) {
            items.add(PostItem.CreatePost)
        }

        list?.let {
            items.addAll(it.map { post -> PostItem.Data(post) })
        }

        if (isLoadingMore && !list.isNullOrEmpty()) {
            items.add(PostItem.Loading)
        }
        submitList(items)
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is PostItem.Data -> TYPE_ITEM
            is PostItem.Loading -> TYPE_LOADING
            is PostItem.CreatePost -> TYPE_CREATE
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ITEM -> {
                val binding = ItemPostBinding.inflate(inflater, parent, false)
                PostViewHolder(binding)
            }

            TYPE_LOADING -> {
                val binding = ItemPostLoadMoreShimmerBinding.inflate(inflater, parent, false)
                LoadingViewHolder(binding)
            }

            else -> {
                val binding = ItemCreatePostBinding.inflate(inflater, parent, false)
                CreatePostViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when {
            holder is PostViewHolder && item is PostItem.Data -> {
                holder.bind(item.post)
            }

            holder is LoadingViewHolder -> {
                holder.binding.shimmerLoadMore.startShimmer()
            }

            holder is CreatePostViewHolder -> {
                holder.binding.root.setOnClickListener {
                    val context = holder.itemView.context
                    context.startActivity(
                        Intent(context, CreateEditPostActivity::class.java)
                    )
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is PostViewHolder) holder.detachPlayer()
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is PostViewHolder) holder.pausePlayer()
    }

    inner class CreatePostViewHolder(val binding: ItemCreatePostBinding) :
        RecyclerView.ViewHolder(binding.root)

    inner class LoadingViewHolder(val binding: ItemPostLoadMoreShimmerBinding) :
        RecyclerView.ViewHolder(binding.root)

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
                icLike.setImageResource(if (post.isLiked) R.drawable.ic_liked else R.drawable.ic_like)
                imgUser.loadAvatar(post.users?.avatarUrl)

                caption.setOnClickListener {
                    binding.caption.maxLines =
                        if (binding.caption.maxLines == 3) Int.MAX_VALUE else 3
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
                listOf(icPlay1, icPlay2, icPlay3, icPlay4).forEach {
                    it.gone()
                }
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

                val images = listOf(ivGrid1, ivGrid2, ivGrid3, ivGrid4)
                val plays = listOf(icPlay1, icPlay2, icPlay3, icPlay4)
                val grids = listOf(
                    grid1, grid2, grid3, grid4
                )

                grids.forEach { it.visibility = View.GONE }
                plays.forEach { it.visibility = View.GONE }
                overlayMore.visibility = View.GONE

                val size = mediaList.size
                val displayCount = minOf(size, 4)

                layoutMediaGrid.visibility = View.VISIBLE

                // show grid cần thiết
                for (i in 0 until displayCount) {
                    grids[i].visibility = View.VISIBLE
                }

                // bind data
                mediaList.take(4).forEachIndexed { index, media ->

                    val imageView = images[index]
                    val playView = plays[index]

                    imageView.loadImage(media.mediaUrl)

                    // reset tránh recycle bug
                    playView.visibility = View.GONE

                    if (media.mediaType.contains("video", true)) {
                        playView.visibility = View.VISIBLE
                    }

                    imageView.setOnClickListener {
                        openMediaViewer(mediaList, index)
                    }
                }

                // overlay more
                if (size > 4) {
                    overlayMore.visibility = View.VISIBLE
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

    private class PostDiffCallback : DiffUtil.ItemCallback<PostItem>() {
        override fun areItemsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
            return if (oldItem is PostItem.Data && newItem is PostItem.Data) {
                oldItem.post.id == newItem.post.id
            } else {
                oldItem is PostItem.Loading && newItem is PostItem.Loading
            }
        }

        override fun areContentsTheSame(oldItem: PostItem, newItem: PostItem): Boolean {
            return oldItem == newItem
        }
    }
}