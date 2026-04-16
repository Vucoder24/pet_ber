package com.nvv.petber.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.CreateContentRepository
import com.nvv.petber.ui.adapter.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateContentViewModel @Inject constructor(
    private val createContentRepository: CreateContentRepository
): ViewModel() {
    var currentEditPostId: String? = null
    private val deletedRemoteMediaIds = mutableListOf<String>()
    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Upload progress (0 - 100)
    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress

    // Success states
    private val _postSuccess = MutableLiveData(false)
    val postSuccess: LiveData<Boolean> = _postSuccess

    private val _storySuccess = MutableLiveData(false)
    val storySuccess: LiveData<Boolean> = _storySuccess

    // Error
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Pets
    private val _userPets = MutableLiveData<List<Pet>>()
    val userPets: LiveData<List<Pet>> = _userPets

    // Selected pet IDs
    private val _selectedPetIds = MutableLiveData<MutableSet<String>>(mutableSetOf())
    val selectedPetIds: LiveData<MutableSet<String>> = _selectedPetIds

    fun setEditMode(post: Post) {
        currentEditPostId = post.id
        post.petIds?.let { ids ->
            _selectedPetIds.value = ids.toMutableSet()
        }
    }

    fun markRemoteMediaAsDeleted(mediaId: String) {
        if (!deletedRemoteMediaIds.contains(mediaId)) {
            deletedRemoteMediaIds.add(mediaId)
        }
    }
    fun loadUserPets() {
        viewModelScope.launch {
            try {
                val pets = createContentRepository.getUserPets()
                _userPets.value = pets
            } catch (_: Exception) {
                // Ignore if user has no pets
            }
        }
    }

    fun togglePetTag(pet: Pet) {
        val current = _selectedPetIds.value ?: mutableSetOf()

        if (current.contains(pet.id)) {
            current.remove(pet.id)
        } else {
            current.add(pet.id)
        }

        _selectedPetIds.value = current
    }

    fun createPost(
        caption: String,
        location: String?,
        hashtags: String?,
        mediaUris: List<Uri>,
        petIds: List<String>
    ) {

        _isLoading.value = true
        _uploadProgress.value = 0

        viewModelScope.launch {

            try {

                createContentRepository.createPost(
                    caption = caption,
                    hashtags = hashtags,
                    location = location,
                    mediaUris = mediaUris,
                    petIds = petIds
                ).collect { progress ->

                    _uploadProgress.value = progress

                    if (progress == 100) {
                        _postSuccess.value = true
                    }
                }

            } catch (e: Exception) {

                _error.value = "Posting failed: ${e.localizedMessage}"

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun createStory(mediaUri: Uri) {

        _isLoading.value = true
        _uploadProgress.value = 0

        viewModelScope.launch {

            try {

                createContentRepository.createStory(mediaUri)
                    .collect { progress ->

                        _uploadProgress.value = progress

                        if (progress == 100) {
                            _storySuccess.value = true
                        }
                    }

            } catch (e: Exception) {

                _error.value = "Create story failed: ${e.localizedMessage}"
                Log.d("CreateContentViewModel", "Create story failed", e)

            } finally {

                _isLoading.value = false
            }
        }
    }

    fun removePetTag(petId: String) {
        val current = _selectedPetIds.value ?: mutableSetOf()
        current.remove(petId)
        _selectedPetIds.value = current
    }

    fun clearError() {
        _error.value = null
    }

    fun submitPost(
        caption: String,
        location: String?,
        hashtags: String?,
        allCurrentMedia: List<MediaItem>,
        petIds: List<String>
    ) {
        _isLoading.value = true
        _uploadProgress.value = 0

        val newMediaUris = allCurrentMedia.filter { !it.isFromRemote }.map { it.uri }

        viewModelScope.launch {
            try {
                val flow = if (currentEditPostId != null) {
                    createContentRepository.updatePost(
                        postId = currentEditPostId!!,
                        caption = caption,
                        hashtags = hashtags,
                        newMediaUris = newMediaUris,
                        deletedMediaIds = deletedRemoteMediaIds,
                        petIds = petIds
                    )
                } else {
                    createContentRepository.createPost(
                        caption = caption,
                        hashtags = hashtags,
                        location = location,
                        mediaUris = newMediaUris,
                        petIds = petIds
                    )
                }

                flow.collect { progress ->
                    _uploadProgress.value = progress
                    if (progress == 100) {
                        _postSuccess.value = true
                    }
                }
            } catch (e: Exception) {
                _error.value = "Posting failed: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetStorySuccess() {
        _storySuccess.value = false
    }
}