package com.nvv.petber.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.local.ProfileRepositoryLocal
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepoLocal: ProfileRepositoryLocal,
    private val homeRepository: HomeRepository,
    context: Context
) : ViewModel() {
    private val currentUserId = SharePrefUtils.getCurrentUserId(context)

    val user = profileRepoLocal.getLocalUser(currentUserId).asLiveData()
    val pets = profileRepoLocal.getLocalPets(currentUserId).asLiveData()
    val posts = profileRepoLocal.getLocalPosts(currentUserId).asLiveData()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _uiState = MutableStateFlow<UpdateUserState>(UpdateUserState.Idle)
    val uiState: StateFlow<UpdateUserState> = _uiState

    init {
        refreshProfile()
    }

    fun refreshProfile(isRefreshing: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isRefreshing) _isRefreshing.postValue(true)
            else _isLoading.postValue(true)
            try {
                profileRepoLocal.syncProfile(currentUserId)
            } catch (e: Exception) {
                Log.e("ProfileVM", "Sync error: ${e.message}")
            } finally {
                if (isRefreshing) _isRefreshing.postValue(false)
                else _isLoading.postValue(false)
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = currentUserId

            val wasLikedBefore = post.isLiked
            val originalLikeCount = post.likeCount

            val updatedPost = post.copy(
                isLiked = !post.isLiked,
                likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
            )
            profileRepoLocal.updatePost(updatedPost)

            homeRepository.toggleLike(post.id, userId, wasLikedBefore)
                .onFailure { error ->
                    Log.e("ProfileVM", "Toggle like failed: ${error.message}")
                    val revertedPost = post.copy(
                        isLiked = wasLikedBefore,
                        likeCount = originalLikeCount
                    )
                    profileRepoLocal.updatePost(revertedPost)
                }
        }
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UpdateUserState.Loading("avatar")
            try {
                withContext(Dispatchers.IO){
                    profileRepoLocal.updateAvatar(currentUserId, uri, user.value?.avatarUrl)
                    profileRepoLocal.syncUser(currentUserId)
                }
                _uiState.value = UpdateUserState.Success
            } catch (e: Exception) {
                _uiState.value = UpdateUserState.Error(e.message ?: "Unknown error")
                Log.e("ProfileVM", "Update avatar error: ${e.message}")
            }
        }
    }

    fun updateCover(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = UpdateUserState.Loading("cover")
            try {
                withContext(Dispatchers.IO){
                    profileRepoLocal.updateCover(currentUserId, uri, user.value?.coverUrl)
                    profileRepoLocal.syncUser(currentUserId)
                }
                _uiState.value = UpdateUserState.Success
            } catch (e: Exception) {
                _uiState.value = UpdateUserState.Error(e.message ?: "Unknown error")
                Log.e("ProfileVM", "Update cover error: ${e.message}")
            }
        }
    }

    fun resetState(){
        _uiState.value = UpdateUserState.Idle
    }

    fun syncPets() {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.postValue(true)
            try {
                profileRepoLocal.syncPets(currentUserId)
            } catch (e: Exception) {
                Log.e("ProfileVM", "Sync error: ${e.message}")
            } finally {
                _isRefreshing.postValue(false)
            }
        }
    }

    fun updateProfile(user: User){
        viewModelScope.launch{
            _uiState.value = UpdateUserState.Loading("information")
            try {
                withContext(Dispatchers.IO){
                    profileRepoLocal.updateProfile(user)
                }
                _uiState.value = UpdateUserState.Success
            }catch (e: Exception){
                _uiState.value = UpdateUserState.Error(e.message ?: "Unknown error")
                Log.e("ProfileVM", "Update profile error: ${e.message}")
            }
        }
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            homeRepository.incrementShareCount(postId)
        }
    }

    fun removePostById(deletedPostId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                profileRepoLocal.deletePostById(deletedPostId)
            } catch (e: Exception) {
                Log.e("ProfileVM", "Delete post error: ${e.message}")
            }
        }
    }

}

sealed class UpdateUserState {
    object Idle : UpdateUserState()
    data class Loading(val style: String) : UpdateUserState()
    object Success : UpdateUserState()
    data class Error(val message: String) : UpdateUserState()
}

