package com.nvv.petber.utils.ext

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

fun RecyclerView.addFeedScrollListener(
    layoutManager: LinearLayoutManager,
    headerCount: Int = 0,
    preloadCount: Int = 3,
    onPauseItem: (RecyclerView.ViewHolder) -> Unit,
    onPreloadItem: (Int) -> Unit
): RecyclerView.OnScrollListener {
    val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val firstVisible = layoutManager.findFirstVisibleItemPosition()
            val lastVisible = layoutManager.findLastVisibleItemPosition()

            if (firstVisible == RecyclerView.NO_POSITION) return

            // 1. Pause items that are hidden from the screen
            for (i in 0 until recyclerView.childCount) {
                val child = recyclerView.getChildAt(i)
                val viewHolder = recyclerView.getChildViewHolder(child)
                val position = viewHolder.bindingAdapterPosition

                if (position < firstVisible || position > lastVisible) {
                    onPauseItem(viewHolder)
                }
            }

            // 2. Calculate the actual index to preload
            val firstDataIndex = maxOf(0, firstVisible - headerCount)
            val lastDataIndex = maxOf(0, lastVisible - headerCount)

            val startPreload = maxOf(0, firstDataIndex - preloadCount)
            val endPreload = lastDataIndex + preloadCount

            for (i in startPreload..endPreload) {
                if (i in firstDataIndex..lastDataIndex) continue
                onPreloadItem(i)
            }
        }
    }
    this.addOnScrollListener(scrollListener)
    return scrollListener
}