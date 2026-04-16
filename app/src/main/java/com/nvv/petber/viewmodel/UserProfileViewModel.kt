package com.nvv.petber.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val profileRepoRemote: ProfileRepositoryRemote,
    private val homeRepository: HomeRepository,
    context: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(context)
    private var targetUserId: String = ""

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _pets = MutableLiveData<List<Pet>>()
    val pets: LiveData<List<Pet>> = _pets

    private val _posts = MutableLiveData<List<Post>?>()
    val posts: LiveData<List<Post>?> = _posts

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> = _isFollowing

    private val _isRefreshing = MutableLiveData<Boolean>()
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUserProfile(userId: String, isRefresh: Boolean = false) {
        targetUserId = userId
        viewModelScope.launch(Dispatchers.IO) {
            if (isRefresh) _isRefreshing.postValue(true)
            else _isLoading.postValue(true)

            try {
                val userDeferred = async { profileRepoRemote.getUser(userId) }
                val statsDeferred = async { profileRepoRemote.getUserStats(userId) }
                val petsDeferred = async { profileRepoRemote.getPets(userId) }
                val postsDeferred = async { profileRepoRemote.getPosts(userId) }
                val followDeferred =
                    async { homeRepository.checkFollowStatus(currentUserId, userId) }

                val remoteUser = userDeferred.await()
                val stats = statsDeferred.await()

                val finalUser = remoteUser?.copy(
                    friendsCount = stats.friendsCount,
                    petFollowingCount = stats.petFollowingCount,
                    followerCount = stats.followerCount,
                    followingCount = stats.followingCount
                )

                _user.postValue(finalUser)
                _pets.postValue(petsDeferred.await())
                _posts.postValue(postsDeferred.await())

                val followResult = followDeferred.await()
                _isFollowing.postValue(followResult.getOrDefault(false))

            } catch (e: Exception) {
                Log.e("UserProfileVM", "Load profile error: ${e.message}")
            } finally {
                if (isRefresh) _isRefreshing.postValue(false)
                else _isLoading.postValue(false)
            }
        }
    }

    fun refreshProfile() {
        if (targetUserId.isNotEmpty()) {
            loadUserProfile(targetUserId, isRefresh = true)
        }
    }

    fun toggleFollow() {
        val currentlyFollowing = _isFollowing.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            // Cập nhật UI ngay lập tức (Optimistic UI update)
            _isFollowing.postValue(!currentlyFollowing)

            val result =
                homeRepository.toggleFollowUser(currentUserId, targetUserId, currentlyFollowing)
            result.onFailure {
                // Rollback nếu lỗi
                _isFollowing.postValue(currentlyFollowing)
                Log.e("UserProfileVM", "Toggle follow failed: ${it.message}")
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val wasLikedBefore = post.isLiked
            val originalLikeCount = post.likeCount

            // Cập nhật UI ngay lập tức
            val updatedPosts = _posts.value?.map {
                if (it.id == post.id) {
                    it.copy(
                        isLiked = !post.isLiked,
                        likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                    )
                } else it
            }
            _posts.postValue(updatedPosts)

            // Gọi API
            homeRepository.toggleLike(post.id, currentUserId, wasLikedBefore)
                .onFailure { error ->
                    Log.e("UserProfileVM", "Toggle like failed: ${error.message}")
                    // Rollback nếu lỗi
                    val revertedPosts = _posts.value?.map {
                        if (it.id == post.id) {
                            it.copy(isLiked = wasLikedBefore, likeCount = originalLikeCount)
                        } else it
                    }
                    _posts.postValue(revertedPosts)
                }
        }
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch { homeRepository.incrementShareCount(postId) }
    }
}