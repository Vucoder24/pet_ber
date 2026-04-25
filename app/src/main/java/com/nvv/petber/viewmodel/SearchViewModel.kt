package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.FilterType
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.PetSearchResult
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.SearchFilter

import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.remote.SearchRepository
import com.nvv.petber.data.repo.remote.UserSearchResult
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    @ApplicationContext ctx: Context
) : ViewModel(){
    private val currentUserId = SharePrefUtils.getCurrentUserId(ctx)
    private val _query = MutableStateFlow("")

    private val _users = MutableStateFlow<List<UserSearchResult>>(emptyList())
    val users = _users.asStateFlow()

    private val _pets = MutableStateFlow<List<PetSearchResult>>(emptyList())
    val pets = _pets.asStateFlow()

    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    private val _userFilter = MutableStateFlow(SearchFilter(FilterType.USER))
    private val _petFilter = MutableStateFlow(SearchFilter(FilterType.PET))
    private val _postFilter = MutableStateFlow(SearchFilter(FilterType.POST))

    private val _speciesList = MutableStateFlow<List<String>>(emptyList())
    val speciesList = _speciesList.asStateFlow()
    private val _isSpeciesLoading = MutableStateFlow(false)
    val isSpeciesLoading = _isSpeciesLoading.asStateFlow()

    init {
        fetchSpecies()
        executeSearch("")
    }
    private fun fetchSpecies() {
        viewModelScope.launch {
            _isSpeciesLoading.value = true
            try {
                val result = withContext(Dispatchers.IO){
                    searchRepository.getUniqueSpecies()
                }
                _speciesList.value = result
            } finally {
                _isSpeciesLoading.value = false
            }
        }
    }

    fun onSearchChanged(newQuery: String) {
        _query.value = newQuery
        executeSearch(newQuery)
    }

    fun getCurrentFilter(position: Int): SearchFilter {
        return when(position) {
            0 -> _userFilter.value
            1 -> _petFilter.value
            else -> _postFilter.value
        }
    }

    fun applyFilter(filter: SearchFilter) {
        when(filter.type) {
            FilterType.USER -> _userFilter.value = filter
            FilterType.PET -> _petFilter.value = filter
            FilterType.POST -> _postFilter.value = filter
        }
        executeSearch(_query.value)
    }

    private fun executeSearch(q: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.emit(true)
            launch { _users.value = searchRepository.searchUsers(q, currentUserId, _userFilter.value) }
            launch { _pets.value = searchRepository.searchPets(q, currentUserId, _petFilter.value) }
            launch { _posts.value = searchRepository.searchPosts(q, currentUserId, _postFilter.value) }
            _isLoading.emit(false)
        }
    }

    fun toggleFollow(targetUser: User, isFollowing: Boolean, context: Context) {
        val previousList = _users.value

        _users.value = previousList.map {
            if (it.user.id == targetUser.id) it.copy(isFollowing = !isFollowing) else it
        }

        viewModelScope.launch(Dispatchers.IO) {
            val success = searchRepository.toggleFollow(currentUserId, targetUser.id, isFollowing)
            if (!success) {
                _users.value = previousList
                val message = context.getString(R.string.error_action)
                _errorEvent.emit(message)
            }
        }
    }

    fun toggleFollowPet(targetPet: Pet, isFollowing: Boolean, context: Context) {
        val previousList = _pets.value
        // Optimistic Update
        _pets.value = previousList.map {
            if (it.pet.id == targetPet.id) it.copy(isFollowing = !isFollowing) else it
        }

        viewModelScope.launch(Dispatchers.IO) {
            val success = searchRepository.toggleFollowPet(currentUserId, targetPet.id, isFollowing)
            if (!success) {
                _pets.value = previousList
                val message = context.getString(R.string.error_action)
                _errorEvent.emit(message)
            }
        }
    }
}