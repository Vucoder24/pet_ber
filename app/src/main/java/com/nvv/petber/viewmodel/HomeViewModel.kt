package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.Story
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val stories: List<Story> = emptyList(),
    val posts: List<Post> = emptyList(),
    val isLoadingStories: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    context: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(context)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()


    init {
        observeStoriesRealtime()
        loadInitialData()
    }

    fun loadInitialData() {
        loadStories()
        loadPosts(refresh = true)
    }

    private fun loadStories() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoadingStories = true)
            homeRepository.fetchStories()
                .onSuccess { stories ->
                    _uiState.value = _uiState.value.copy(
                        stories = stories,
                        isLoadingStories = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingStories = false,
                        error = e.message
                    )
                }
        }
    }

    fun loadPosts(refresh: Boolean = false) {
        if (_uiState.value.isLoadingPosts || _uiState.value.isLoadingMore) return

        viewModelScope.launch {
            val page = if (refresh) 0 else _uiState.value.currentPage

            if (refresh) {
                _uiState.value = _uiState.value.copy(isLoadingPosts = true, error = null)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }
            try {
                homeRepository.fetchPosts(currentUserId, page)
                    .onSuccess { newPosts ->
                        val updatedPosts = if (refresh) newPosts else _uiState.value.posts + newPosts
                        _uiState.value = _uiState.value.copy(
                            posts = updatedPosts,
                            isLoadingPosts = false,
                            isLoadingMore = false,
                            currentPage = page + 1,
                            hasMore = newPosts.size == 10
                        )
                    }
                    .onFailure { e ->
                        _uiState.value = _uiState.value.copy(
                            isLoadingPosts = false,
                            isLoadingMore = false,
                            error = e.message
                        )
                    }
            }catch (_: Exception){
            }


        }
    }

    private fun observeStoriesRealtime() {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.getStoriesFlow()
                .collect {
                    loadStories()
                }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            // Optimistic update
            val updatedPosts = _uiState.value.posts.map { p ->
                if (p.id == post.id) {
                    p.copy(
                        isLiked = !p.isLiked,
                        likeCount = if (p.isLiked) p.likeCount - 1 else p.likeCount + 1
                    )
                } else p
            }
            _uiState.value = _uiState.value.copy(posts = updatedPosts)

            homeRepository.toggleLike(post.id!!, currentUserId, post.isLiked)
                .onFailure {
                    // Revert on error
                    val revertedPosts = _uiState.value.posts.map { p ->
                        if (p.id == post.id) {
                            p.copy(
                                isLiked = post.isLiked,
                                likeCount = post.likeCount
                            )
                        } else p
                    }
                    _uiState.value = _uiState.value.copy(posts = revertedPosts, error = it.message)
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
