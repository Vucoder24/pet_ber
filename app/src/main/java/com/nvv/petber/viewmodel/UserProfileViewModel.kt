package com.nvv.petber.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.mapper.toEntity
import com.nvv.petber.data.model.ConversationEntity
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.TranslationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val profileRepoRemote: ProfileRepositoryRemote,
    private val homeRepository: HomeRepository,
    @ApplicationContext val ctx: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(ctx)
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
    private val _isLoadingSendMessage = MutableLiveData<Boolean>()
    val isLoadingSendMessage: LiveData<Boolean> = _isLoadingSendMessage

    private val _isLoadMore = MutableLiveData(false)
    val isLoadMore: LiveData<Boolean> = _isLoadMore

    private val _navigateToChat = MutableLiveData<ConversationEntity?>()
    val navigateToChat: LiveData<ConversationEntity?> = _navigateToChat

    private val _errorMsg = MutableSharedFlow<String>()
    val errorMsg = _errorMsg.asSharedFlow()

    private var currentOffset = 0
    private val limit = 10
    private var hasMoreData = true

    fun loadUserProfile(userId: String, isRefresh: Boolean = false) {
        targetUserId = userId
        if (isRefresh) {
            currentOffset = 0
            hasMoreData = true
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (isRefresh) _isRefreshing.postValue(true)
            else _isLoading.postValue(true)

            try {
                val userDeferred = async { profileRepoRemote.getUser(userId) }
                val statsDeferred = async { profileRepoRemote.getUserStats(userId) }
                val petsDeferred = async { profileRepoRemote.getPets(userId) }
                val postsDeferred = async { profileRepoRemote.getPosts(userId, 0, limit, currentUserId) }
                val followDeferred =
                    async { homeRepository.checkFollowStatus(currentUserId, userId) }

                val remoteUser = userDeferred.await()
                val stats = statsDeferred.await()

                var finalUser = remoteUser?.copy(
                    friendsCount = stats.friendsCount,
                    petFollowingCount = stats.petFollowingCount,
                    followerCount = stats.followerCount,
                    followingCount = stats.followingCount
                )
                finalUser = finalUser?.let { user ->
                    val translatedFields = TranslationUtils.translateAll(
                        user.gender,
                    )
                    user.copy(
                        gender = translatedFields[0],
                    )
                }

                _user.postValue(finalUser)
                _pets.postValue(petsDeferred.await())
                _posts.postValue(postsDeferred.await())
                currentOffset = postsDeferred.await().size
                hasMoreData = postsDeferred.await().size >= limit

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

    fun loadMorePosts() {
        if (_isLoadMore.value == true || !hasMoreData || targetUserId.isEmpty()) return

        _isLoadMore.postValue(true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newPosts = profileRepoRemote.getPosts(targetUserId, currentOffset, limit, currentUserId)

                val currentList = _posts.value ?: emptyList()
                val updatedList = (currentList + newPosts).distinctBy { it.id }

                _posts.postValue(updatedList)
                currentOffset = updatedList.size
                hasMoreData = newPosts.size >= limit

            } catch (e: Exception) {
                Log.e("UserProfileVM", "Load more error: ${e.message}")
            } finally {
                _isLoadMore.postValue(false)
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
            _isFollowing.postValue(!currentlyFollowing)

            val result =
                homeRepository.toggleFollowUser(currentUserId, targetUserId, currentlyFollowing)
            result.onFailure {
                _isFollowing.postValue(currentlyFollowing)
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch(Dispatchers.IO) {
            val wasLikedBefore = post.isLiked
            val originalLikeCount = post.likeCount

            val updatedPosts = _posts.value?.map {
                if (it.id == post.id) {
                    it.copy(
                        isLiked = !post.isLiked,
                        likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                    )
                } else it
            }
            _posts.postValue(updatedPosts)

            homeRepository.toggleLike(post.id, currentUserId, wasLikedBefore)
                .onFailure { error ->
                    Log.e("UserProfileVM", "Toggle like failed: ${error.message}")
                    val revertedPosts = _posts.value?.map {
                        if (it.id == post.id) {
                            it.copy(isLiked = wasLikedBefore, likeCount = originalLikeCount)
                        } else it
                    }
                    _posts.postValue(revertedPosts)
                }
        }
    }

    fun startConversation() {
        if (targetUserId.isEmpty() || currentUserId.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoadingSendMessage.postValue(true)
                val conversation = profileRepoRemote.getOrCreateConversation(currentUserId, targetUserId)
                _isLoadingSendMessage.postValue(false)
                _navigateToChat.postValue(conversation.toEntity(currentUserId))
            }catch (e: Exception){
                Log.d("ERR", "${e.message}")
                _isLoadingSendMessage.postValue(false)
                _errorMsg.emit(ctx.getString(R.string.error_action))
            }

        }
    }

    fun onChatNavigated() {
        _navigateToChat.value = null
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) { homeRepository.incrementShareCount(postId) }
    }
}