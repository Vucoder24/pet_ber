package com.nvv.petber.ui.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.nvv.petber.R

data class MediaItemPicker(
    val id: Long,
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val duration: Long = 0L,
    val dateAdded: Long
) {
    val isVideo: Boolean get() = mimeType.startsWith("video")
}


class MediaPickerSheetAdapter(
    private val onItemClick: (MediaItemPicker) -> Unit
) : ListAdapter<MediaItemPicker, MediaPickerSheetAdapter.MediaViewHolder>(DIFF_CALLBACK) {

    private var selectedItemId: Long? = null

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<MediaItemPicker>() {
            override fun areItemsTheSame(old: MediaItemPicker, new: MediaItemPicker) = old.id == new.id
            override fun areContentsTheSame(old: MediaItemPicker, new: MediaItemPicker) = old == new
        }
    }

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivMediaThumbnail)
        val tvDuration: TextView = itemView.findViewById(R.id.tvVideoDuration)
        val ivSelected: ImageView = itemView.findViewById(R.id.ivSelectedOverlay)
        val viewSelectedDim: View = itemView.findViewById(R.id.viewSelectedDim)

        fun bind(item: MediaItemPicker) {
            Glide.with(itemView.context)
                .load(item.uri)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .centerCrop()
                .into(ivThumbnail)

            if (item.isVideo) {
                tvDuration.visibility = View.VISIBLE
                tvDuration.text = formatDuration(item.duration)
            } else {
                tvDuration.visibility = View.GONE
            }

            val isSelected = selectedItemId == item.id
            ivSelected.visibility = if (isSelected) View.VISIBLE else View.GONE
            viewSelectedDim.visibility = if (isSelected) View.VISIBLE else View.GONE

            itemView.setOnClickListener {
                val previousId = selectedItemId
                val currentId = item.id

                if (previousId == currentId) {
                    selectedItemId = null
                    notifyItemChanged(layoutPosition)
                } else {
                    selectedItemId = currentId
                    if (previousId != null) {
                        val oldPos = currentList.indexOfFirst { it.id == previousId }
                        if (oldPos != -1) notifyItemChanged(oldPos)
                    }
                    notifyItemChanged(layoutPosition)
                }
                onItemClick(item)
            }
        }

        private fun formatDuration(durationMs: Long): String {
            if (durationMs <= 0) return ""
            val totalSec = durationMs / 1000
            val min = totalSec / 60
            val sec = totalSec % 60
            return "%d:%02d".format(min, sec)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getSelectedItem(): MediaItemPicker? {
        return currentList.find { it.id == selectedItemId }
    }
}