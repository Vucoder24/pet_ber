package com.nvv.petber.viewmodel


import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.PetRepository
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetProfileViewModel @Inject constructor(
    private val repository: ProfileRepositoryRemote,
    private val petRepo: PetRepository,
    context: Context
) : ViewModel() {
    private val _uiState = MutableStateFlow<UpdatePetState>(UpdatePetState.Idle)
    val uiState: StateFlow<UpdatePetState> = _uiState.asStateFlow()
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

    private var currentOffset = 0
    private val limit = 10
    private var hasMoreData = true
    private val currentUserId = SharePrefUtils.getCurrentUserId(context)

    fun setPetData(pet: Pet) {
        _petState.value = pet
        loadPetPosts(pet.id, isRefresh = true)
    }

    fun fetchPetById(petId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _error.value = null
            try {
                val pet = repository.getPetById(petId)
                if (pet != null) {
                    _petState.value = pet
                    loadPetPosts(petId, isRefresh = true)
                } else {
                    _error.value = "Không tìm thấy thông tin pet"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = e.message
                _isLoading.value = false
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
                val newPosts = repository.getPetPosts(petId, offsetToFetch, limit, currentUserId)

                if (isRefresh) {
                    _posts.value = newPosts
                    currentOffset = newPosts.size
                } else {
                    val updated = (_posts.value + newPosts).distinctBy { it.id }
                    _posts.value = updated
                    currentOffset = updated.size
                }

                hasMoreData = newPosts.size >= limit
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
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UpdatePetState.Loading("avatar")
            try {
                val updatedPet = petRepo.updatePetAvatar(currentPet.id, uri, currentPet.avatarUrl)
                _petState.value = updatedPet
                _uiState.value = UpdatePetState.Success
            } catch (e: Exception) {
                _uiState.value = UpdatePetState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun updateCover(uri: Uri) {
        val currentPet = _petState.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = UpdatePetState.Loading("cover")
            try {
                val updatedPet = petRepo.updatePetCover(currentPet.id, uri, currentPet.coverUrl)
                _petState.value = updatedPet
                _uiState.value = UpdatePetState.Success
            } catch (e: Exception) {
                _uiState.value = UpdatePetState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun resetUiState() {
        _uiState.value = UpdatePetState.Idle
    }

}

sealed class UpdatePetState {
    object Idle : UpdatePetState()
    data class Loading(val style: String) : UpdatePetState()
    object Success : UpdatePetState()
    data class Error(val message: String) : UpdatePetState()
}