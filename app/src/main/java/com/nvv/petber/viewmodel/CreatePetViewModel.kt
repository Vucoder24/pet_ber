package com.nvv.petber.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.repo.remote.PetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CreatePetViewModel @Inject constructor(
    private val repository: PetRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreatePetState>(CreatePetState.Idle)
    val uiState: StateFlow<CreatePetState> = _uiState.asStateFlow()

    fun createPet(
        pet: Pet,
        avatarUri: Uri?, coverUri: Uri?
    ) {
        viewModelScope.launch {
            _uiState.value = CreatePetState.Loading

            val result = withContext(Dispatchers.IO) {
                repository.createPet(pet, avatarUri, coverUri)
            }

            _uiState.value = if (result.isSuccess) {
                CreatePetState.Success
            } else {
                CreatePetState.Error(result.exceptionOrNull()?.message ?: "Error")
            }
        }
    }

    fun resetState() {
        _uiState.value = CreatePetState.Idle
    }

    fun savePet(pet: Pet) {
        viewModelScope.launch {
            _uiState.value = CreatePetState.Loading

            val result = withContext(Dispatchers.IO) {
                repository.updatePet(pet)
            }
            _uiState.value = if (result.isSuccess) {
                CreatePetState.Success
            } else {
                CreatePetState.Error(result.exceptionOrNull()?.message ?: "Error")
            }
        }
    }
}

sealed class CreatePetState {
    object Idle : CreatePetState()
    object Loading : CreatePetState()
    object Success : CreatePetState()
    data class Error(val message: String) : CreatePetState()
}