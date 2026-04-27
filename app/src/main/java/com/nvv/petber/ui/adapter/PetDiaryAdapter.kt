package com.nvv.petber.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.DiaryMonth
import com.nvv.petber.data.model.PetHealthLog
import com.nvv.petber.data.model.Post
import com.nvv.petber.databinding.ItemDiaryCreateBinding
import com.nvv.petber.databinding.ItemDiaryMonthBinding
import com.nvv.petber.ui.dialog.PetHealthDetailBottomSheet
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.visible

class PetDiaryAdapter(
    private val onViewPostClick: (Post) -> Unit,
    private val onCreatePostClick: () -> Unit
) :
    ListAdapter<PetDiaryAdapter.DiaryItem, RecyclerView.ViewHolder>(DiaryDiffCallback()) {


    companion object {
        private const val TYPE_CREATE = 0
        private const val TYPE_MONTH = 1
    }

    sealed class DiaryItem {
        data class Month(val data: DiaryMonth) : DiaryItem()
        object CreatePost : DiaryItem()
    }

    fun submitData(list: List<DiaryMonth>?, showCreatePost: Boolean = true) {
        val items = mutableListOf<DiaryItem>()

        if (showCreatePost) {
            items.add(DiaryItem.CreatePost)
        }

        list?.let {
            items.addAll(it.map { month -> DiaryItem.Month(month) })
        }

        submitList(items)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_CREATE -> {
                val binding = ItemDiaryCreateBinding.inflate(inflater, parent, false)
                CreatePostViewHolder(binding)
            }

            else -> {
                val binding = ItemDiaryMonthBinding.inflate(inflater, parent, false)
                DiaryViewHolder(binding)
            }
        }
    }

    inner class CreatePostViewHolder(
        val binding: ItemDiaryCreateBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is DiaryItem.CreatePost -> TYPE_CREATE
            is DiaryItem.Month -> TYPE_MONTH
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (val item = getItem(position)) {

            is DiaryItem.CreatePost -> {
                (holder as CreatePostViewHolder).binding.root.setOnClickListener {
                    onCreatePostClick()
                }
            }

            is DiaryItem.Month -> {
                (holder as DiaryViewHolder).bind(item.data)
            }
        }
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
                setHasFixedSize(true)
                itemAnimator = null
            }
        }

        fun bind(item: DiaryMonth) {
            binding.tvMonthYear.text = item.monthYear

            if (item.posts.isEmpty()) {
                innerAdapter.submitList(emptyList())
            } else {
                innerAdapter.submitList(item.posts)
                val log = item.healthLog
                if (log != null) {
                    binding.cardHealth.visible()
                    binding.tvWeight.text = binding.root.context.getString(
                        R.string.label_weight,
                        log.weight?.let { "$it kg" }
                            ?: binding.root.context.getString(R.string.value_na)
                    )

                    binding.tvClinicalStatus.text = binding.root.context.getString(
                        R.string.label_clinical_status,
                        log.clinicalStatus ?:
                        binding.root.context.getString(R.string.value_na)
                    )
                    binding.btnDetail.setOnClickListener {
                        showHealthDetailDialog(log)
                    }
                } else {
                    binding.cardHealth.gone()
                }
            }
        }

        private fun showHealthDetailDialog(log: PetHealthLog) {
            val fragment = binding.root.findFragment<Fragment>()
            PetHealthDetailBottomSheet
                .newInstance(log)
                .show(fragment.childFragmentManager, PetHealthDetailBottomSheet.TAG)
        }
    }

    class DiaryDiffCallback : DiffUtil.ItemCallback<DiaryItem>() {

        override fun areItemsTheSame(
            oldItem: DiaryItem,
            newItem: DiaryItem
        ): Boolean {
            return when {
                oldItem is DiaryItem.CreatePost &&
                        newItem is DiaryItem.CreatePost -> true

                oldItem is DiaryItem.Month &&
                        newItem is DiaryItem.Month ->
                    oldItem.data.monthYear == newItem.data.monthYear

                else -> false
            }
        }

        override fun areContentsTheSame(
            oldItem: DiaryItem,
            newItem: DiaryItem
        ): Boolean = oldItem == newItem
    }
}

