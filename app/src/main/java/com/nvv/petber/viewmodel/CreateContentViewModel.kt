package com.nvv.petber.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.repo.remote.CreateContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateContentViewModel @Inject constructor(
    private val createContentRepository: CreateContentRepository
): ViewModel() {

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
        mediaUris: List<Uri>
    ) {

        _isLoading.value = true
        _uploadProgress.value = 0

        viewModelScope.launch {

            try {

                val petId = _selectedPetIds.value?.firstOrNull()

                createContentRepository.createPost(
                    caption = caption,
                    hashtags = hashtags,
                    location = location,
                    mediaUris = mediaUris,
                    petId = petId
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

    fun clearError() {
        _error.value = null
    }

    fun resetPostSuccess() {
        _postSuccess.value = false
    }

    fun resetStorySuccess() {
        _storySuccess.value = false
    }
}