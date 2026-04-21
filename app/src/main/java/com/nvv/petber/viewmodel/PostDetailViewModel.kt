package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    context: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(context)

    private val _post = MutableLiveData<Post?>()
    val post: LiveData<Post?> = _post

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadPostById(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.postValue(true)
            homeRepository.fetchPostById(postId, currentUserId)
                .onSuccess { result ->
                    _post.postValue(result)
                    _isLoading.postValue(false)
                }
                .onFailure { e ->
                    _error.postValue(e.message)
                    _isLoading.postValue(false)
                }
        }
    }

    fun toggleLike(currentPost: Post) {
        val updatedPost = currentPost.copy(
            isLiked = !currentPost.isLiked,
            likeCount = if (currentPost.isLiked) currentPost.likeCount - 1 else currentPost.likeCount + 1
        )
        _post.value = updatedPost

        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.toggleLike(currentPost.id, currentUserId, currentPost.isLiked)
                .onFailure {
                    _post.value = currentPost
                    _error.value = it.message
                }
        }
    }

    fun incrementShareCount() {
        val currentPost = _post.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.incrementShareCount(currentPost.id)
        }
    }
}