package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.Notification
import com.nvv.petber.data.repo.remote.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: NotificationRepository,
    @ApplicationContext val context: Context
) : ViewModel() {
    private var isListening = false
    private val notificationMap = LinkedHashMap<String, Notification>()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications = _notifications

    private val _newNotificationEvent = MutableSharedFlow<Notification>()
    val newNotificationEvent = _newNotificationEvent.asSharedFlow()
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isLoadMore = MutableStateFlow(false)
    val isLoadMore = _isLoadMore.asStateFlow()

    private var debounceJob: Job? = null
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    private val pendingNotifications = mutableListOf<Notification>()

    private var currentPage = 1
    private var isLastPage = false
    private val pageSize = 15

    fun fetchNotifications(userId: String, isRefresh: Boolean = false) {
        if (_isLoading.value || _isLoadMore.value || (isLastPage && !isRefresh)) return
        viewModelScope.launch {
            if (isRefresh) {
                currentPage = 1
                isLastPage = false
                _isLoading.value = true
            } else {
                _isLoadMore.value = true
            }

            try {
                val newItems = withContext(Dispatchers.IO) {
                    repository.getNotifications(userId, currentPage, pageSize)
                }

                if (newItems.isEmpty() || newItems.size < pageSize) {
                    isLastPage = true
                }

                mergeNotifications(newItems, isRefresh)

                if (newItems.isNotEmpty()) {
                    currentPage++
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
                _isLoadMore.value = false
            }
        }
    }

    private fun mergeNotifications(newList: List<Notification>, isRefresh: Boolean) {
        if (isRefresh) {
            notificationMap.clear()
        }

        newList.forEach { notif ->
            val existing = notificationMap[notif.id]

            notificationMap[notif.id] = when {
                existing == null -> notif
                else -> notif.copy(
                    isRead = notif.isRead || existing.isRead
                )
            }
        }

        _notifications.value = notificationMap.values
            .sortedByDescending { it.createdAt }
    }

    fun fetchNewCount(userId: String, lastSeen: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = repository.getNewNotificationCount(userId, lastSeen)
                _unreadCount.emit(count)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startListeningRealtime(userId: String) {
        if (isListening) return
        isListening = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.setupChannel(userId)
                repository.listenToNewNotifications(userId)
                    .collect { notif ->
                        mergeNotifications(listOf(notif), isRefresh = false)
                        if (!notif.isRead) {
                            _unreadCount.value += 1
                        }
                        _newNotificationEvent.emit(notif)
                        handleDebounce()
                    }
            }catch (_: Exception){}
        }
    }


    private fun handleDebounce() {
        debounceJob?.cancel()

        debounceJob = viewModelScope.launch {
            delay(500)

            val list = pendingNotifications.toList()
            pendingNotifications.clear()

            if (list.isNotEmpty()) {
                _newNotificationEvent.emit(
                    if (list.size == 1) list.first()
                    else Notification(
                        id = UUID.randomUUID().toString(),
                        message = context.getString(R.string.n_new_notifications, list.size),
                        userId = "",
                        type = "grouped"
                    )
                )
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

    fun markAsRead(notificationId: String) {
        val oldMap = HashMap(notificationMap)

        notificationMap[notificationId]?.let {
            notificationMap[notificationId] = it.copy(isRead = true)
        }

        _notifications.value = notificationMap.values
            .sortedByDescending { it.createdAt }

        _unreadCount.value = (_unreadCount.value - 1).coerceAtLeast(0)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.markAsRead(notificationId)
            } catch (_: Exception) {
                notificationMap.clear()
                notificationMap.putAll(oldMap)

                _notifications.value = notificationMap.values
                    .sortedByDescending { it.createdAt }

                _unreadCount.value += 1
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        val oldMap = HashMap(notificationMap)

        notificationMap.remove(notificationId)

        _notifications.value = notificationMap.values
            .sortedByDescending { it.createdAt }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteNotification(notificationId)
                _toastEvent.emit(context.getString(R.string.notifi_deleted))
            } catch (e: Exception) {
                e.printStackTrace()
                _toastEvent.emit(context.getString(R.string.error_action))
                notificationMap.clear()
                notificationMap.putAll(oldMap)

                _notifications.value = notificationMap.values
                    .sortedByDescending { it.createdAt }
            }
        }
    }
}