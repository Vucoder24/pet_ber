package com.nvv.petber.ui.adapter

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StoryRowAdapter(
    private val storyAdapter: StoryAdapter
) : RecyclerView.Adapter<StoryRowAdapter.StoryRowViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryRowViewHolder {
        val rv = RecyclerView(parent.context).apply {
            layoutManager =
                LinearLayoutManager(
                    parent.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
            adapter = storyAdapter
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return StoryRowViewHolder(rv)
    }

    override fun onBindViewHolder(holder: StoryRowViewHolder, position: Int) {
        // No-op, adapter is already set
    }

    override fun getItemCount(): Int = 1

    class StoryRowViewHolder(view: View) : RecyclerView.ViewHolder(view)
}