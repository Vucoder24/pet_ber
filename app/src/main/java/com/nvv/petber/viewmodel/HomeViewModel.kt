package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.Story
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val stories: List<Story> = emptyList(),
    val posts: List<Post> = emptyList(),
    val isInitialLoading: Boolean = true,
    val isLoadingStories: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMorePost: Boolean = true,
    val isLoadingMoreStories: Boolean = false,
    val currentStoryPage: Int = 0,
    val hasMoreStories: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    @ApplicationContext val context: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(context)
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val limitPost = 15
    val limitStory = 15

    init {
        observeLocalData()
        observeStoriesRealtime()
        loadInitialData()
    }

    private fun observeLocalData() {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.observeOfflinePosts().collectLatest { localPosts ->
                _uiState.value = _uiState.value.copy(
                    posts = localPosts,
                    isInitialLoading = false
                )
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.observeOfflineStories().collectLatest { localStories ->
                _uiState.value = _uiState.value.copy(
                    stories = localStories
                )
            }
        }
    }

    fun loadInitialData() {
        _uiState.value = _uiState.value.copy(
            isInitialLoading = true
        )
        loadStories()
        loadPosts(refresh = false)
    }

    fun refreshData() {
        loadStories(refresh = true)
        loadPosts(refresh = true)
    }

    fun loadStories(refresh: Boolean = false) {
        if (_uiState.value.isLoadingStories || _uiState.value.isLoadingMoreStories) return
        if (!refresh && !_uiState.value.hasMoreStories) return

        viewModelScope.launch {
            val page = if (refresh) 0 else _uiState.value.currentStoryPage

            if (refresh) {
                _uiState.value = _uiState.value.copy(isLoadingStories = true)
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMoreStories = true)
            }

            val result = withContext(Dispatchers.IO) {
                homeRepository.fetchStories(page = page, userLimit = limitStory)
            }

            result.onSuccess { newStories ->
                val newUniqueUsersCount = newStories.distinctBy { it.userId }.size

                _uiState.value = _uiState.value.copy(
                    isLoadingStories = false,
                    isLoadingMoreStories = false,
                    currentStoryPage = page + 1,
                    hasMoreStories = newUniqueUsersCount >= limitStory
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoadingStories = false, isLoadingMoreStories = false)
            }
        }
    }

    fun loadPosts(refresh: Boolean = false) {
        if (_uiState.value.isLoadingPosts || _uiState.value.isLoadingMore || _uiState.value.isRefreshing) return
        if (!refresh && !_uiState.value.hasMorePost) return

        viewModelScope.launch {
            val page = if (refresh) 0 else _uiState.value.currentPage

            if (refresh) {
                if (_uiState.value.posts.isEmpty()) {
                    _uiState.value = _uiState.value.copy(isLoadingPosts = true, error = null)
                } else {
                    _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoadingMore = true)
            }

            try {
                val result = withContext(Dispatchers.IO) {
                    homeRepository.fetchPosts(currentUserId, page, limitPost)
                }

                result.onSuccess { newPosts ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPosts = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        currentPage = page + 1,
                        hasMorePost = newPosts.size >= limitPost
                    )
                }.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingPosts = false,
                        isRefreshing = false,
                        isLoadingMore = false,
                        error = e.message
                    )
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingPosts = false,
                    isRefreshing = false,
                    isLoadingMore = false
                )
            }
        }
    }

    private fun observeStoriesRealtime() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                homeRepository.getStoriesFlow()
                    .collect {
                        loadStories(refresh = true)
                    }
            } catch (_: Exception) {
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    homeRepository.toggleLike(
                        postId = post.id,
                        userId = currentUserId,
                        isCurrentlyLiked = post.isLiked,
                        currentLikeCount = post.likeCount
                    )
                }

                result.onFailure {
                    _uiState.value = _uiState.value.copy(error = it.message)
                }

            } catch (_: Exception) {
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.incrementShareCount(postId)
        }
    }

    fun removePostById(postId: String) {
        val currentList = _uiState.value.posts

        val updatedList = currentList.filterNot { it.id == postId }

        _uiState.value = _uiState.value.copy(
            posts = updatedList
        )
    }
}
