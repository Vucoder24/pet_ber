package com.nvv.petber.viewmodel


import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.DiaryMonth
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.HomeRepository
import com.nvv.petber.data.repo.remote.PetRepository
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import com.nvv.petber.utils.FilterPostUtils
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

sealed class PetProfileEvent {
    object PetDeleted : PetProfileEvent()
    object PetDeleteError: PetProfileEvent()
}

@HiltViewModel
class PetProfileViewModel @Inject constructor(
    private val repository: ProfileRepositoryRemote,
    private val homeRepository: HomeRepository,
    private val petRepo: PetRepository,
    context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow<UpdatePetState>(UpdatePetState.Idle)
    val uiState: StateFlow<UpdatePetState> = _uiState.asStateFlow()

    private val _isOwner = MutableStateFlow(false)
    val isOwner: StateFlow<Boolean> = _isOwner.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()
    private val _followerCount = MutableStateFlow(0L)
    val followerCount: StateFlow<Long> = _followerCount.asStateFlow()
    private val _petState = MutableStateFlow<Pet?>(null)
    val petState: StateFlow<Pet?> = _petState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // State cho Posts
    private val _posts = MutableStateFlow<List<Post>>(emptyList())
    val posts: StateFlow<List<Post>> = _posts.asStateFlow()

    private val _isLoadMore = MutableStateFlow(false)
    val isLoadMore: StateFlow<Boolean> = _isLoadMore.asStateFlow()
    private val _diaryPosts = MutableStateFlow<List<DiaryMonth>>(emptyList())
    val diaryPosts: StateFlow<List<DiaryMonth>> = _diaryPosts.asStateFlow()

    private val _event = MutableSharedFlow<PetProfileEvent>()
    val event: SharedFlow<PetProfileEvent> = _event.asSharedFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    private var currentOffset = 0
    private val limitPost = 10
    private var hasMoreData = true
    private val currentUserId = SharePrefUtils.getCurrentUserId(context)

    fun setPetData(pet: Pet, isOwner: Boolean) {
        _petState.value = pet
        _isOwner.value = isOwner
        loadPetPosts(pet.id, isRefresh = true)
        loadDiaryPosts(pet.id)
        loadFollowerCount(pet.id)
    }

    fun loadFollowerCount(petId: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                petRepo.getPetFollowerCount(petId)
            }
            result.onSuccess { count ->
                _followerCount.value = count
            }
        }
    }


    fun fetchPetById(petId: String) {
        viewModelScope.launch {
            _error.value = null
            try {
                val pet = withContext(Dispatchers.IO) {
                    repository.getPetById(petId)
                }
                if (pet != null) {
                    _petState.value = pet
                    checkOwnershipAndFollowStatus(pet)
                    loadPetPosts(petId, isRefresh = true)
                    loadDiaryPosts(petId)
                    loadFollowerCount(petId)
                } else {
                    _error.value = "Pet information not found"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun removePostById(postId: String) {
        _posts.value = _posts.value.filterNot { it.id == postId }

        _diaryPosts.value = _diaryPosts.value.map { month ->
            month.copy(
                posts = month.posts.filterNot { it.id == postId }
            )
        }.filter { it.posts.isNotEmpty() }
    }

    private fun loadDiaryPosts(petId: String) {
        viewModelScope.launch {
            try {
                val allPosts = withContext(Dispatchers.IO) {
                    petRepo.getAllPetDiaryPosts(petId)
                }

                val groupedData = FilterPostUtils.groupPostsByMonth(allPosts)
                _diaryPosts.value = groupedData
            } catch (_: Exception) {
                // Handle error
            }
        }
    }

    private suspend fun checkOwnershipAndFollowStatus(pet: Pet) {
        val isPetOwner = pet.ownerId == currentUserId
        _isOwner.value = isPetOwner

        if (!isPetOwner) {
            _isFollowing.value = petRepo.checkIsFollowingPet(currentUserId, pet.id)
        } else {
            _isFollowing.value = false
        }
    }

    fun toggleFollow(context: Context) {
        val petId = _petState.value?.id ?: return
        val currentStatus = _isFollowing.value

        // Optimistic UI update
        _isFollowing.value = !currentStatus
        if (!currentStatus) _followerCount.value++ else _followerCount.value--

        viewModelScope.launch {
            val success = withContext(Dispatchers.IO) {
                petRepo.toggleFollowPet(currentUserId, petId, currentStatus)
            }
            if (!success) {
                _isFollowing.value = currentStatus
                if (!currentStatus) _followerCount.value-- else _followerCount.value++
                _error.value = context.getString(R.string.error_action)
            }
        }
    }

    fun loadPetPosts(petId: String, isRefresh: Boolean = false) {
        if (!isRefresh && (_isLoadMore.value || !hasMoreData)) return

        if (isRefresh) {
            _isLoading.value = true
            currentOffset = 0
            hasMoreData = true
        } else {
            _isLoadMore.value = true
        }

        viewModelScope.launch {
            try {
                val offsetToFetch = if (isRefresh) 0 else currentOffset
                val newPosts = withContext(Dispatchers.IO) {
                    repository.getPetPosts(petId, offsetToFetch, limitPost, currentUserId)
                }

                if (isRefresh) {
                    _posts.value = newPosts
                    currentOffset = newPosts.size
                } else {
                    val updated = (_posts.value + newPosts).distinctBy { it.id }
                    _posts.value = updated
                    currentOffset = updated.size
                }

                hasMoreData = newPosts.size >= limitPost
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _isLoading.value = false
                _isLoadMore.value = false
            }
        }
    }

    fun updateAvatar(uri: Uri) {
        val currentPet = _petState.value ?: return
        viewModelScope.launch {
            _uiState.value = UpdatePetState.Loading("avatar")
            try {
                val updatedPet = withContext(Dispatchers.IO) {
                    petRepo.updatePetAvatar(currentPet.id, uri, currentPet.avatarUrl)
                }
                _petState.value = updatedPet
                _uiState.value = UpdatePetState.Success
            } catch (e: Exception) {
                _uiState.value = UpdatePetState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateCover(uri: Uri) {
        val currentPet = _petState.value ?: return
        viewModelScope.launch {
            _uiState.value = UpdatePetState.Loading("cover")
            try {
                val updatedPet = withContext(Dispatchers.IO) {
                    petRepo.updatePetCover(currentPet.id, uri, currentPet.coverUrl)
                }
                _petState.value = updatedPet
                _uiState.value = UpdatePetState.Success
            } catch (e: Exception) {
                _uiState.value = UpdatePetState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val wasLikedBefore = post.isLiked
            val originalLikeCount = post.likeCount

            val updatedPosts = _posts.value.map {
                if (it.id == post.id) {
                    it.copy(
                        isLiked = !post.isLiked,
                        likeCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                    )
                } else it
            }
            _posts.value = updatedPosts

            val result = withContext(Dispatchers.IO) {
                homeRepository.toggleLike(post.id, currentUserId, wasLikedBefore)
            }
            result.onFailure { error ->
                val revertedPosts = _posts.value.map {
                    if (it.id == post.id) {
                        it.copy(isLiked = wasLikedBefore, likeCount = originalLikeCount)
                    } else it
                }
                _posts.value = revertedPosts
            }

        }
    }

    fun incrementShareCount(postId: String) {
        viewModelScope.launch(Dispatchers.IO) { homeRepository.incrementShareCount(postId) }
    }

    fun resetUiState() {
        _uiState.value = UpdatePetState.Idle
    }

    fun deletePet() {
        val petId = _petState.value?.id ?: return
        viewModelScope.launch {
            _isDeleting.value = true
            val result = withContext(Dispatchers.IO) {
                petRepo.deletePet(petId)
            }
            _isDeleting.value = false
            result.onSuccess {
                _event.emit(PetProfileEvent.PetDeleted)
            }.onFailure { e ->
                _event.emit(PetProfileEvent.PetDeleteError)
            }
        }
    }
}

sealed class UpdatePetState {
    object Idle : UpdatePetState()
    data class Loading(val style: String) : UpdatePetState()
    object Success : UpdatePetState()
    data class Error(val message: String) : UpdatePetState()
}