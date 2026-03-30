package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.PhotoView
import com.nvv.petber.R
import com.nvv.petber.data.model.PostMedia

class MediaPagerAdapter(
    private val mediaList: List<PostMedia>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val activePlayers = mutableListOf<ExoPlayer>()

    private val VIEW_TYPE_IMAGE = 1
    private val VIEW_TYPE_VIDEO = 2

    override fun getItemViewType(position: Int): Int {
        return if (mediaList[position].mediaType.lowercase().contains("video"))
            VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
    }

    override fun getItemCount(): Int {
        return mediaList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_IMAGE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_viewer_image, parent, false)
            ImageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_viewer_video, parent, false)
            VideoViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val media = mediaList[position]
        if (holder is ImageViewHolder) {
            holder.bind(media.mediaUrl)
        } else if (holder is VideoViewHolder) {
            holder.bind(media.mediaUrl)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.releasePlayer()
        }
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        fun bind(url: String) {
            Glide.with(itemView.context).load(url).into(photoView)
        }
    }

    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playerView: PlayerView = itemView.findViewById(R.id.playerViewFull)
        private var exoPlayer: ExoPlayer? = null

        fun bind(url: String) {
            exoPlayer = ExoPlayer.Builder(itemView.context).build().apply {
                setMediaItem(MediaItem.fromUri(url))
                prepare()
                playWhenReady = true
            }
            playerView.player = exoPlayer
            exoPlayer?.let { activePlayers.add(it) }
        }

        fun releasePlayer() {
            exoPlayer?.let {
                activePlayers.remove(it)
                it.release()
            }
            exoPlayer = null
            playerView.player = null
        }
    }
    fun pauseAll() {
        activePlayers.forEach { it.pause() }
    }

    fun releaseAll() {
        activePlayers.forEach { it.release() }
        activePlayers.clear()
    }
}