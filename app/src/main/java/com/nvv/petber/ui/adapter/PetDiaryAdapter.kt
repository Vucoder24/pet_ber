package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.data.model.DiaryMonth
import com.nvv.petber.data.model.Post
import com.nvv.petber.databinding.ItemDiaryMonthBinding

class PetDiaryAdapter(
    private val onViewPostClick: (Post) -> Unit
) :
    ListAdapter<DiaryMonth, PetDiaryAdapter.DiaryViewHolder>(DiaryDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val binding =
            ItemDiaryMonthBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiaryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DiaryViewHolder(private val binding: ItemDiaryMonthBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val innerAdapter = MiniPostAdapter(
            onClickMiniPost = onViewPostClick
        )


        init {
            binding.rvMonthPosts.apply {
                layoutManager = LinearLayoutManager(
                    context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = innerAdapter
            }
        }

        fun bind(item: DiaryMonth) {
            binding.tvMonthYear.text = item.monthYear
            innerAdapter.submitList(item.posts)
        }
    }

    class DiaryDiffCallback : DiffUtil.ItemCallback<DiaryMonth>() {
        override fun areItemsTheSame(oldItem: DiaryMonth, newItem: DiaryMonth): Boolean =
            oldItem.monthYear == newItem.monthYear

        override fun areContentsTheSame(oldItem: DiaryMonth, newItem: DiaryMonth): Boolean =
            oldItem == newItem
    }
}

