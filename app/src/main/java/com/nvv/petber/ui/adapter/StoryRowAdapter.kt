package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R

class StoryRowAdapter(
    private val storyAdapter: StoryAdapter,
    private val onLoadMore: () -> Unit
) : RecyclerView.Adapter<StoryRowAdapter.StoryRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryRowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story_header, parent, false)
        return StoryRowViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryRowViewHolder, position: Int) {
        holder.rvStories.apply {
            if (layoutManager == null) {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                adapter = storyAdapter

                addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val lm = layoutManager as LinearLayoutManager
                        val totalItemCount = lm.itemCount
                        val lastVisibleItem = lm.findLastVisibleItemPosition()

                        if (totalItemCount <= (lastVisibleItem + 2)) {
                            onLoadMore()
                        }
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int = 1

    class StoryRowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rvStories: RecyclerView = view.findViewById(R.id.rvStoriesHorizontal)
    }

}