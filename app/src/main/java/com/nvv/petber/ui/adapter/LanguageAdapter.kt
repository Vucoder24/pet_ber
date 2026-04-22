package com.nvv.petber.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.Language
import com.nvv.petber.data.model.LanguageListItem
import com.nvv.petber.databinding.ItemLanguageBinding
import com.nvv.petber.databinding.ItemLanguageHeaderBinding

class LanguageAdapter(
    private val onItemClick: (Language) -> Unit
) : ListAdapter<LanguageListItem, RecyclerView.ViewHolder>(LanguageDiffCallback()) {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_LANGUAGE = 1
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is LanguageListItem.Header -> TYPE_HEADER
        is LanguageListItem.LanguageItem -> TYPE_LANGUAGE
    }

    inner class HeaderViewHolder(val binding: ItemLanguageHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(header: LanguageListItem.Header) {
            binding.tvHeader.setText(header.titleRes)
        }
    }

    inner class LanguageViewHolder(val binding: ItemLanguageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LanguageListItem.LanguageItem) {
            val lang = item.language
            binding.tvFlag.text = lang.flag
            binding.tvLangName.text = itemView.context.getString(lang.displayNameRes)
            binding.tvNative.text = itemView.context.getString(lang.nativeNameRes)
            binding.radioBtn.setImageResource(
                if (lang.isSelected) R.drawable.ic_radio_select
                else R.drawable.ic_radio_unselect
            )
            binding.root.setBackgroundColor(
                if (lang.isSelected)
                    ContextCompat.getColor(itemView.context, R.color.selected_bg)
                else Color.WHITE
            )
            binding.root.setOnClickListener { onItemClick(lang) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        TYPE_HEADER -> HeaderViewHolder(
            ItemLanguageHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        else -> LanguageViewHolder(
            ItemLanguageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is LanguageListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is LanguageListItem.LanguageItem -> (holder as LanguageViewHolder).bind(item)
        }
    }


    class LanguageDiffCallback : DiffUtil.ItemCallback<LanguageListItem>() {
        override fun areItemsTheSame(
            oldItem: LanguageListItem,
            newItem: LanguageListItem
        ): Boolean {
            if (oldItem is LanguageListItem.Header && newItem is LanguageListItem.Header) {
                return oldItem.titleRes == newItem.titleRes
            }
            if (oldItem is LanguageListItem.LanguageItem && newItem is LanguageListItem.LanguageItem) {
                return oldItem.language.code == newItem.language.code
            }
            return false
        }

        override fun areContentsTheSame(
            oldItem: LanguageListItem,
            newItem: LanguageListItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}