package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R

class StoryRowAdapter(
    private val storyAdapter: StoryAdapter
) : RecyclerView.Adapter<StoryRowAdapter.StoryRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryRowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_story_header, parent, false)
        return StoryRowViewHolder(view)
    }

    override fun onBindViewHolder(holder: StoryRowViewHolder, position: Int) {
        holder.rvStories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = storyAdapter
        }
    }

    override fun getItemCount(): Int = 1

    class StoryRowViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rvStories: RecyclerView = view.findViewById(R.id.rvStoriesHorizontal)
    }

}