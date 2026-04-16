package com.nvv.petber.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Notification
import com.nvv.petber.data.repo.remote.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {
    private var isListening = false

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications

    private val _newNotificationEvent = MutableSharedFlow<Notification>()
    val newNotificationEvent = _newNotificationEvent.asSharedFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun fetchNotifications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getNotifications(userId)
                _notifications.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun startListeningRealtime(userId: String) {
        if (isListening) return
        isListening = true

        viewModelScope.launch {
            repository.setupChannel(userId)
            repository.listenToNewNotifications(userId)
                .collect { notif ->
                    _notifications.value = listOf(notif) + _notifications.value
                    _newNotificationEvent.emit(notif)
                    Log.d("Realtime", "notification received: $notif")
                }
        }
    }

    fun stopRealtime() {
        viewModelScope.launch(Dispatchers.IO){
            try {
                repository.disconnect()
            }catch (_:Exception){}
        }
    }
}