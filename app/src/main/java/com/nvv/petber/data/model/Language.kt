package com.nvv.petber.data.model

import androidx.annotation.StringRes

data class Language(
    val code: String,
    @StringRes val displayNameRes: Int,
    @StringRes val nativeNameRes: Int,
    val flag: String,
    val isSelected: Boolean = false
)

sealed class LanguageListItem {
    data class Header(@StringRes val titleRes: Int) : LanguageListItem()
    data class LanguageItem(val language: Language) : LanguageListItem()
}