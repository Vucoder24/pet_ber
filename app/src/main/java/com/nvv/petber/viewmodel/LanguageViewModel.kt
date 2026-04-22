package com.nvv.petber.viewmodel

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.Language
import com.nvv.petber.data.model.LanguageListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val prefs: SharedPreferences
) : ViewModel() {

    companion object {
        const val KEY_LANGUAGE = "app_language"
        const val DEFAULT_LANG = "en"
    }

    private val _languages = MutableStateFlow<List<Language>>(emptyList())
    val languages: StateFlow<List<Language>> = _languages

    private val _selectedCode = MutableStateFlow(getSavedLanguage())
    val selectedCode: StateFlow<String> = _selectedCode

    val displayList: StateFlow<List<LanguageListItem>> = _languages.map { list ->
        buildList {
            val current = list.filter { it.isSelected }
            val others = list.filter { !it.isSelected }

            if (current.isNotEmpty()) {
                add(LanguageListItem.Header(R.string.currently_using))
                current.forEach { add(LanguageListItem.LanguageItem(it)) }
            }

            add(LanguageListItem.Header(R.string.all_languages))
            others.forEach { add(LanguageListItem.LanguageItem(it)) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init { loadLanguages() }

    private fun loadLanguages() {
        val saved = getSavedLanguage()
        _selectedCode.value = saved
        _languages.value = listOf(
            Language("en", R.string.lang_en_display, R.string.lang_en_native, "🇺🇸"),
            Language("vi", R.string.lang_vi_display, R.string.lang_vi_native, "🇻🇳"),
            Language("ja", R.string.lang_ja_display, R.string.lang_ja_native, "🇯🇵"),
            Language("ko", R.string.lang_ko_display, R.string.lang_ko_native, "🇰🇷"),
            Language("zh", R.string.lang_zh_display, R.string.lang_zh_native, "🇨🇳"),
            Language("fr", R.string.lang_fr_display, R.string.lang_fr_native, "🇫🇷"),
            Language("de", R.string.lang_de_display, R.string.lang_de_native, "🇩🇪"),
            Language("es", R.string.lang_es_display, R.string.lang_es_native, "🇪🇸"),
            Language("pt", R.string.lang_pt_display, R.string.lang_pt_native, "🇧🇷"),
            Language("th", R.string.lang_th_display, R.string.lang_th_native, "🇹🇭"),
        ).map { it.copy(isSelected = it.code == saved) }
    }

    fun selectLanguage(code: String) {
        _selectedCode.value = code
        _languages.value = _languages.value.map {
            it.copy(isSelected = it.code == code)
        }
    }

    fun applyLanguage() {
        prefs.edit { putString(KEY_LANGUAGE, _selectedCode.value) }
    }

    fun getSavedLanguage(): String =
        prefs.getString(KEY_LANGUAGE, DEFAULT_LANG) ?: DEFAULT_LANG
}