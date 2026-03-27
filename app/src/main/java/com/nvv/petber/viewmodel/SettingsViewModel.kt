package com.nvv.petber.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.repo.remote.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    fun logout(onSuccess: () -> Unit, onError: (String) -> Unit){
        viewModelScope.launch(Dispatchers.IO){
            try {
                authRepository.logout()
                onSuccess()
            }catch (e: Exception){
                onError(e.localizedMessage ?: "Error logout")
            }
        }
    }
}