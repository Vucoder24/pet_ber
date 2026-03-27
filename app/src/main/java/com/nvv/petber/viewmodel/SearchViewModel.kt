package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post

import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.remote.SearchRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    context: Context
) : ViewModel(){
    private val currentUserId = SharePrefUtils.getCurrentUserId(context)
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users = _users.asStateFlow()

    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets = _pets.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        executeSearch("")
    }
    fun onSearchChanged(newQuery: String) {
        _query.value = newQuery
        executeSearch(newQuery)
    }

    private fun executeSearch(q: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            launch { _users.value = searchRepository.searchUsers(q) }
            launch { _pets.value = searchRepository.searchPets(q) }
            launch { _posts.value = searchRepository.searchPostsByHashtag(q, currentUserId) }
            _isLoading.value = false
        }
    }
}