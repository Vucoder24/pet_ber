package com.nvv.petber.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.repo.remote.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetFollowersViewModel @Inject constructor(
    private val petRepo: PetRepository
) : ViewModel() {

    private val _followers = MutableStateFlow<List<com.nvv.petber.data.model.User>>(emptyList())
    val followers: StateFlow<List<com.nvv.petber.data.model.User>> = _followers.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadFollowers(petId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.emit(true)
            petRepo.getPetFollowers(petId)
                .onSuccess { list -> _followers.emit(list) }
                .onFailure { }
            _isLoading.emit(false)
        }
    }
}