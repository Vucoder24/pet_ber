package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.repo.remote.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ForgotPasswordState {
    data object Idle : ForgotPasswordState()
    data object Loading : ForgotPasswordState()
    data class OtpSent(val email: String) : ForgotPasswordState()
    data class OtpVerified(val email: String) : ForgotPasswordState()
    data object PasswordUpdated : ForgotPasswordState()
    data class Error(val message: String) : ForgotPasswordState()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow<ForgotPasswordState>(ForgotPasswordState.Idle)
    val state: StateFlow<ForgotPasswordState> = _state

    fun sendOtp(email: String) {
        _state.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            authRepository.sendResetPasswordOtp(email)
                .onSuccess {
                    _state.value = ForgotPasswordState.OtpSent(email)
                }
                .onFailure {
                    _state.value = ForgotPasswordState.Error(
                        it.toFriendlyMessage(default = context.getString(R.string.error_send_otp))
                    )
                }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _state.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            authRepository.verifyResetPasswordOtp(email, otp)
                .onSuccess {
                    _state.value = ForgotPasswordState.OtpVerified(email)
                }
                .onFailure {
                    _state.value = ForgotPasswordState.Error(
                        it.toFriendlyMessage(default = context.getString(R.string.otp_incorrect_or_expired))
                    )
                }
        }
    }

    fun updatePassword(newPassword: String) {
        _state.value = ForgotPasswordState.Loading
        viewModelScope.launch {
            authRepository.updatePassword(newPassword)
                .onSuccess {
                    _state.value = ForgotPasswordState.PasswordUpdated
                }
                .onFailure {
                    _state.value = ForgotPasswordState.Error(
                        it.toFriendlyMessage(default = context.getString(R.string.error_update_password))
                    )
                }
        }
    }

    fun reset() {
        _state.value = ForgotPasswordState.Idle
    }

    private fun Throwable.toFriendlyMessage(default: String): String {
        val raw = message.orEmpty().lowercase()

        return when {
            "otp_expired" in raw -> context.getString(R.string.otp_expired)
            "otp_invalid" in raw || "invalid otp" in raw -> context.getString(R.string.otp_invalid)
            "email not confirmed" in raw || "email_not_confirmed" in raw -> context.getString(R.string.email_not_confirmed)
            "invalid login credentials" in raw || "invalid_credentials" in raw -> context.getString(R.string.invalid_credentials)
            "too many requests" in raw -> context.getString(R.string.error_too_many_requests)
            else -> message ?: default
        }
    }
}