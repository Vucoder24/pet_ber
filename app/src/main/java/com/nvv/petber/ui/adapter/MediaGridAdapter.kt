package com.nvv.petber.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.nvv.petber.R
import com.nvv.petber.databinding.ItemMediaGridBinding
import com.nvv.petber.utils.ext.toast
import kotlinx.parcelize.Parcelize
import java.util.Locale

@Parcelize
data class MediaItem(
    val uri: Uri,
    val isVideo: Boolean,
    var duration: Long? = null,
    val isSelected: Boolean = false,
    val isFromRemote: Boolean = false,
    val remoteId: String? = null,
    val remoteUrl: String? = null
): Parcelable

class MediaGridAdapter(
    private val ctx: Context,
    private var multiSelect: Boolean = false,
    private val maxSelect: Int = 10,
    private val onSelectionChanged: ((Int) -> Unit)? = null
) : ListAdapter<MediaItem, MediaGridAdapter.VH>(DIFF) {

    // keep list of selected Uris in order of selection
    private val selected = LinkedHashMap<Uri, Int>() // uri -> order (1-based)

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<MediaItem>() {
            override fun areItemsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
                oldItem.uri == newItem.uri

            override fun areContentsTheSame(oldItem: MediaItem, newItem: MediaItem): Boolean =
                oldItem == newItem
        }
    }

    inner class VH(private val b: ItemMediaGridBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: MediaItem) {
            // load thumbnail
            Glide.with(ctx)
                .load(item.uri)
                .thumbnail(0.25f)
                .override(300, 300)
                .centerCrop()
                .format(DecodeFormat.PREFER_RGB_565)
                .into(b.ivThumbnail)

            // video duration
            b.txtDuration.visibility = if (item.isVideo) View.VISIBLE else View.GONE
            if (item.isVideo) {
                b.txtDuration.text = formatDuration(item.duration!!)
            }

            // update selection overlay and index
            val order = selected[item.uri]
            if (order != null) {
                b.overlayCheck.visibility = View.VISIBLE
                b.txtIndex.visibility = View.VISIBLE
                b.txtIndex.text = order.toString()
            } else {
                b.overlayCheck.visibility = View.GONE
                b.txtIndex.visibility = View.GONE
            }

            // click handling
            b.root.setOnClickListener {
                if (multiSelect) {
                    toggleSelection(item)
                    notifyListUpdated()
                } else {
                    // single -> return immediately
                    val result = ArrayList<MediaItem>()
                    result.add(item)
                    if (ctx is ActivityWithResult) {
                        ctx.returnResult(result)
                    }
                }
            }

            // also allow tap on index overlay to deselect
            b.root.setOnLongClickListener {
                if (multiSelect) {
                    toggleSelection(item)
                    notifyListUpdated()
                    true
                } else false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemMediaGridBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun notifyListUpdated() {
        // inform observers about change
        onSelectionChanged?.invoke(selected.size)
        // we must refresh visible items to update indices / overlay
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setInitialSelection(preselected: List<MediaItem>) {
        selected.clear()
        preselected.forEachIndexed { index, item ->
            selected[item.uri] = index + 1
        }
        notifyDataSetChanged()
    }

    private fun toggleSelection(item: MediaItem) {
        val uri = item.uri
        if (selected.containsKey(uri)) {
            // remove
            selected.remove(uri)
            // reassign orders so they remain consecutive
            val tmp = LinkedHashMap<Uri, Int>()
            var i = 1
            selected.keys.forEach { u ->
                tmp[u] = i++
            }
            selected.clear()
            selected.putAll(tmp)
        } else {
            // add
            if (selected.size >= maxSelect) {
                ctx.toast(ctx.getString(R.string.maximum_media_and_count, maxSelect))
                return
            }
            selected[uri] = selected.size + 1
        }
    }

    fun getSelectedUris(): List<Uri> = selected.keys.toList()
    fun getSelectedItems(): List<MediaItem> {
        val map = currentList.associateBy { it.uri }
        return selected.keys.mapNotNull { map[it] }
    }

    fun setMultiSelect(value: Boolean) {
        multiSelect = value
        if (!value) {
            // clear selection when switching to single
            selected.clear()
            notifyListUpdated()
        } else {
            notifyListUpdated()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun submitList(list: List<MediaItem>?) {
        super.submitList(list?.toList()) // defensive copy
        // keep selection consistency: remove selected not in new list
        val currentSet = list?.map { it.uri }?.toSet() ?: emptySet()
        val toRemove = selected.keys.filter { it !in currentSet }
        if (toRemove.isNotEmpty()) {
            toRemove.forEach { selected.remove(it) }
            // reassign indices
            val tmp = LinkedHashMap<Uri, Int>()
            var i = 1
            selected.keys.forEach { u -> tmp[u] = i++ }
            selected.clear()
            selected.putAll(tmp)
            onSelectionChanged?.invoke(selected.size)
        }
        notifyDataSetChanged()
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0L) return ""
        val totalSeconds = (ms / 1000).toInt()
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) String.format(
            Locale.getDefault(),
            "%d:%02d:%02d",
            hours,
            minutes,
            seconds
        )
        else String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

// tiny interface so adapter can return result to activity in single select mode
interface ActivityWithResult {
    fun returnResult(list: ArrayList<MediaItem>)
}