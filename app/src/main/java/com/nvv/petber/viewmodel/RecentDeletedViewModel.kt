package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RecentDeletedViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    @ApplicationContext val context: Context
) : ViewModel() {
    private val pendingActions = mutableMapOf<String, Pair<Post, Int>>()
    private var userId: String = ""
    private val _posts = MutableLiveData<List<Post>>(emptyList())
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isLoadingMore = MutableLiveData(false)
    val isLoadingMore: LiveData<Boolean> = _isLoadingMore

    private val _errorState = MutableLiveData<String?>()
    val errorState: LiveData<String?> = _errorState

    private val _successState = MutableLiveData<String?>()
    val successState: LiveData<String?> = _successState

    fun clearSuccess() {
        _successState.value = null
    }

    private val _isLastPage = MutableLiveData(false)
    val isLastPage: LiveData<Boolean> = _isLastPage

    private var currentPage = 0
    private val pageSize = 10

    fun init(userId: String) {
        this.userId = userId
        refresh()
    }

    fun refresh() {
        currentPage = 0
        _isLastPage.value = false
        loadPage(isRefresh = true)
    }

    fun loadMore() {
        if (_isLoading.value == true || _isLoadingMore.value == true || _isLastPage.value == true) return
        currentPage++
        loadPage(isRefresh = false)
    }

    private fun loadPage(isRefresh: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isRefresh) _isLoading.postValue(true) else _isLoadingMore.postValue(true)
            homeRepository.fetchDeletedPosts(userId, currentPage, pageSize)
                .onSuccess { newPosts ->
                    val current = _posts.value.orEmpty()
                    _posts.postValue(
                        if (isRefresh) newPosts
                        else (current + newPosts).distinctBy { it.id })

                    if (newPosts.size < pageSize) _isLastPage.postValue(true)
                }.onFailure {
                    if (!isRefresh) currentPage--
                }
            if (isRefresh) _isLoading.postValue(false)
            else _isLoadingMore.postValue(false)
        }
    }

    fun restorePost(post: Post) {
        val currentList = _posts.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == post.id }

        if (index != -1) {
            pendingActions[post.id] = Pair(post, index)
            currentList.removeAt(index)
            _posts.value = currentList

            viewModelScope.launch(Dispatchers.IO) {
                homeRepository.restorePost(post.id)
                    .onSuccess {
                        pendingActions.remove(post.id)
                        _successState.postValue(
                            context.getString(R.string.successfull_restore_post)
                        )
                    }
                    .onFailure {
                        rollback(post.id)
                        _errorState.postValue(context.getString(R.string.restore_failed))
                    }
            }
        }
    }

    fun hardDeletePost(post: Post) {
        val currentList = _posts.value.orEmpty().toMutableList()
        val index = currentList.indexOfFirst { it.id == post.id }

        if (index != -1) {
            pendingActions[post.id] = Pair(post, index)
            currentList.removeAt(index)
            _posts.value = currentList

            viewModelScope.launch(Dispatchers.IO) {
                homeRepository.hardDeletePostComplete(post)
                    .onSuccess {
                        pendingActions.remove(post.id)
                        _successState.postValue(
                            context.getString(R.string.permanently_deleted_post)
                        )
                    }
                    .onFailure {
                        rollback(post.id)
                        _errorState.postValue(
                            context.getString(R.string.error_permanently_delete_post)
                        )
                    }
            }
        }
    }

    private fun rollback(postId: String) {
        val pending = pendingActions[postId] ?: return
        val (post, index) = pending

        val currentList = _posts.value.orEmpty().toMutableList()
        val safeIndex = index.coerceAtMost(currentList.size)
        currentList.add(safeIndex, post)

        _posts.postValue(currentList)
        pendingActions.remove(postId)
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val wasLikedBefore = post.isLiked
            val originalLikeCount = post.likeCount

            val result = withContext(Dispatchers.IO) {
                homeRepository.toggleLike(
                    postId = post.id,
                    userId = userId,
                    isCurrentlyLiked = wasLikedBefore
                )
            }

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
                _errorState.postValue(context.getString(R.string.error_action))
            }
        }
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.incrementShareCount(postId)
        }
    }

    fun clearError() {
        _errorState.postValue(null)
    }
}