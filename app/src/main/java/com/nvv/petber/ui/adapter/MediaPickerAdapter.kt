package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nvv.petber.databinding.ItemMediaPickerBinding

data class MediaModel(
    val uri: Uri,
    val isVideo: Boolean
)

class MediaPickerAdapter(
    private val onMediaSelected: (MediaModel) -> Unit
) : RecyclerView.Adapter<MediaPickerAdapter.MediaViewHolder>() {

    private val mediaList = mutableListOf<MediaModel>()

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(list: List<MediaModel>) {
        mediaList.clear()
        mediaList.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding =
            ItemMediaPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaList[position])
    }

    override fun getItemCount() = mediaList.size

    inner class MediaViewHolder(private val binding: ItemMediaPickerBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(media: MediaModel) {
            Glide.with(binding.root.context)
                .load(media.uri)
                .centerCrop()
                .into(binding.ivThumbnail)

            binding.ivVideoIndicator.visibility = if (media.isVideo) View.VISIBLE else View.GONE

            binding.root.setOnClickListener {
                onMediaSelected(media)
            }
        }
    }
}