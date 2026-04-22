package com.nvv.petber.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SavedPostsViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {
    private val pendingRemovedPosts = mutableMapOf<String, Pair<Post, Int>>()
    private var userId: String = ""

    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _isLastPage = MutableLiveData(false)
    val isLastPage: LiveData<Boolean> = _isLastPage

    private val pageSize = 10
    private var currentPage = 0

    fun init(userId: String) {
        if (this.userId == userId) return
        this.userId = userId
        refresh()
    }

    fun refresh() {
        currentPage = 0
        _isLastPage.value = false
        loadPage(isRefresh = true)
    }

    fun loadMore() {
        if (_isLoading.value == true) return
        if (_isLoadingMore.value == true) return
        if (_isLastPage.value == true) return

        currentPage += 1
        loadPage(isRefresh = false)
    }

    fun removePostLocal(post: Post) {
        val currentList = _posts.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == post.id }

        if (index != -1) {
            pendingRemovedPosts[post.id] = Pair(post, index)
            currentList.removeAt(index)
            _posts.value = currentList
        }
    }

    fun unsavePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.toggleSavePost(post.id, userId, true)
                .onSuccess {
                    pendingRemovedPosts.remove(post.id)
                }
                .onFailure {
                    val pending = pendingRemovedPosts[post.id] ?: return@onFailure
                    val (originalPost, index) = pending

                    val currentList = _posts.value.orEmpty().toMutableList()

                    if (currentList.none { it.id == originalPost.id }) {
                        val safeIndex = index.coerceAtMost(currentList.size)
                        currentList.add(safeIndex, originalPost)
                        _posts.postValue(currentList)
                    }

                    pendingRemovedPosts.remove(post.id)
                }
        }
    }

    private fun loadPage(isRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isRefresh) {
                _isLoading.postValue(true)
            } else {
                _isLoadingMore.postValue(true)
            }

            try {
                val result =
                    homeRepository.fetchSavedPosts(
                        userId = userId,
                        page = currentPage,
                        pageSize = pageSize
                    )

                result.onSuccess { newPosts ->
                    val currentList = _posts.value.orEmpty()

                    val merged = if (isRefresh) {
                        newPosts
                    } else {
                        (currentList + newPosts).distinctBy { it.id }
                    }

                    _posts.postValue(merged)

                    if (newPosts.size < pageSize) {
                        _isLastPage.postValue(true)
                    }
                }.onFailure { e ->
                    Log.e("SavedPostsVM", "Load saved posts failed: ${e.message}")
                    if (!isRefresh && currentPage > 0) currentPage -= 1
                }
            } finally {
                if (isRefresh) {
                    _isLoading.postValue(false)
                } else {
                    _isLoadingMore.postValue(false)
                }
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val wasLikedBefore = post.isLiked
            val originalLikeCount = post.likeCount

            val result = homeRepository.toggleLike(
                postId = post.id,
                userId = userId,
                isCurrentlyLiked = wasLikedBefore
            )

            result.onSuccess { liked ->
                val current = _posts.value.orEmpty().toMutableList()
                val index = current.indexOfFirst { it.id == post.id }
                if (index != -1) {
                    current[index] = current[index].copy(
                        isLiked = liked,
                        likeCount = if (liked) originalLikeCount + 1 else originalLikeCount - 1
                    )
                    _posts.postValue(current)
                }
            }.onFailure { e ->
                Log.e("SavedPostsVM", "Toggle like failed: ${e.message}")
            }
        }
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.incrementShareCount(postId)
        }
    }
}