package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.nvv.petber.databinding.ItemMediaPreviewBinding

class MediaPreviewAdapter(
    private val items: MutableList<MediaItem>,
    private val onRemove: (Int) -> Unit,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<MediaPreviewAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemMediaPreviewBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMediaPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        Glide.with(holder.binding.ivPreview.context)
            .load(item.uri)
            .centerCrop()
            .into(holder.binding.ivPreview)

        if (item.isVideo) {
            holder.binding.txtDuration.visibility = View.VISIBLE
            holder.binding.txtDuration.text = formatDuration(item.duration)
        } else {
            holder.binding.txtDuration.visibility = View.GONE
        }

        holder.binding.btnRemove.setOnClickListener {
            onRemove(position)
        }

        holder.binding.root.setOnClickListener {
            onClick(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newItems: List<MediaItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    @SuppressLint("DefaultLocale")
    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format("%02d:%02d", min, sec)
    }

}