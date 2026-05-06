package com.nvv.petber.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.repo.remote.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(input: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                authRepository.loginWithPassword(input, password)
            }
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                _authState.value =
                    AuthState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error when logging in"
                    )
            }
        }
    }

    fun register(email: String, pass: String, displayName: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO){
                authRepository.register(email, pass, displayName)
            }
            if (result.isSuccess) {
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error(
                    result.exceptionOrNull()?.message ?: "Unknown error when registering"
                )
            }
        }
    }

    fun resetState(){
        _authState.value = AuthState.Idle
    }

}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val error: String) : AuthState()
}