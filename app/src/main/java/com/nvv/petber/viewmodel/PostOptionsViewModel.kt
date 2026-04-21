package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostOptionsViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(context)
    private val _isSavedRemote = MutableStateFlow<Boolean?>(null)
    val isSavedRemote = _isSavedRemote.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()
    private val _actionState = MutableStateFlow<ActionState>(ActionState.Idle)
    val actionState = _actionState.asStateFlow()
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError = _loadError.asStateFlow()

    private val _isFollowing = MutableStateFlow<Boolean?>(null)
    val isFollowing = _isFollowing.asStateFlow()

    fun loadInitData(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val savedDef = async { homeRepository.isPostSaved(post.id, currentUserId) }
            val followDef = async { homeRepository.checkFollowStatus(currentUserId, post.userId) }

            val savedResult = savedDef.await()
            val followResult = followDef.await()

            if (savedResult.isSuccess && followResult.isSuccess) {
                _isSavedRemote.value = savedResult.getOrNull()
                _isFollowing.value = followResult.getOrNull()
                _isLoading.value = false
            } else {
                _loadError.value = context.getString(R.string.error)
                _isLoading.value = false
            }
        }
    }

    fun toggleFollowUser(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentStatus = _isFollowing.value ?: false
            homeRepository.toggleFollowUser(
                currentUserId,
                post.userId,
                currentStatus
            ).onSuccess {
                    val newStatus = !currentStatus
                    _isFollowing.value = newStatus
                    val msg = if (newStatus)
                        context.getString(
                            R.string.followed_user,
                            post.users?.fullName ?: context.getString(R.string.petber_user)
                        )
                    else context.getString(
                        R.string.unfollowed_user,
                        post.users?.fullName ?: context.getString(R.string.petber_user)
                    )
                    _actionState.value = ActionState.Success(msg)
            }.onFailure {
                    _actionState.value =
                        ActionState.Error(context.getString(R.string.error_action))
                }
        }
    }

    fun toggleSavePost(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentStatus = _isSavedRemote.value ?: false
            homeRepository.toggleSavePost(post.id, currentUserId, currentStatus)
                .onSuccess {
                    val newStatus = !currentStatus
                    _isSavedRemote.value = newStatus
                    val msg = if (newStatus) context.getString(R.string.post_saved)
                    else context.getString(R.string.post_unsaved)
                    _actionState.value = ActionState.Success(msg)
                }
                .onFailure {
                    _actionState.value = ActionState.Error(context.getString(R.string.error_action))
                }
        }
    }

    fun hidePost(postId: String) {
        _actionState.value = ActionState.Success("Đã ẩn bài viết này khỏi bảng tin")
    }

    fun blockUser(targetUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // homeRepository.blockUser(currentUserId, targetUserId)
            _actionState.value = ActionState.Success(context.getString(R.string.blocked_user))
        }
    }

    fun resetState() {
        _actionState.value = ActionState.Idle
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.incrementShareCount(postId)
        }
    }
}

sealed class ActionState {
    object Idle : ActionState()
    data class Success(val message: String) : ActionState()
    data class Error(val message: String) : ActionState()
}