package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.mapper.toModel
import com.nvv.petber.data.model.MessageModel
import com.nvv.petber.data.repo.remote.ChatDetailRepository
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val repository: ChatDetailRepository,
    private val profileRepositoryRemote: ProfileRepositoryRemote,
    @ApplicationContext val ctx: Context,
) : ViewModel() {
    private var cachedConversationId: String? = null
    private var realtimeJob: Job? = null

    val currentUserId: String = SharePrefUtils.getCurrentUserId(ctx)
    private val _currentConvId = MutableStateFlow("")

    private var targetUserId: String? = null
    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false
    private var isLoadingMore = false

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages = _currentConvId.flatMapLatest { id ->
        repository.observeLocalMessages(id).map { entityList ->
        entityList.map { it.toModel() }.sortedBy { it.createdAt }
    }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())


    fun setTargetUser(userId: String?, convId: String?) {
        this.targetUserId = userId
        if (!convId.isNullOrBlank()) {
            setConversation(convId)
        }
    }

    private suspend fun ensureConversationId(): String {
        if (!_currentConvId.value.isBlank()) return _currentConvId.value

        val conversation = profileRepositoryRemote.getOrCreateConversation(
            currentUserId,
            targetUserId ?: ""
        )

        val newId = conversation.conversationId
        _currentConvId.value = newId
        subscribeToRealtime(newId)
        return newId
    }

    fun loadHistoryMessages() {
        val convId = _currentConvId.value
        if (isLoadingMore || isLastPage || convId.isEmpty()) return

        viewModelScope.launch {
            isLoadingMore = true
            try {
                val hasData = repository.fetchAndSaveMessages(convId, currentPage)
                if (!hasData) {
                    isLastPage = true
                } else {
                    currentPage++
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                isLoadingMore = false
            }
        }
    }

    private fun subscribeToRealtime(id: String) {
        realtimeJob?.cancel()

        realtimeJob = repository.observeNewMessages(id)
            .onEach {newMessage ->
                repository.syncNewMessage(newMessage)
            }
            .catch { e -> _error.value = "Realtime error: ${e.message}" }
            .launchIn(viewModelScope)
    }

    fun setConversation(id: String) {
        if (id.isBlank() || _currentConvId.value == id) return

        _currentConvId.value = id
        cachedConversationId = id

        currentPage = 0
        isLastPage = false

        subscribeToRealtime(id)
        loadHistoryMessages()
    }


    fun sendTextMessage(content: String) {
        if (content.isBlank()) return

        viewModelScope.launch {
            try {
                val convId = withContext(Dispatchers.IO){
                    ensureConversationId()
                }

                val tempId = UUID.randomUUID().toString()
                val tempMessage = MessageModel(
                    id = tempId,
                    conversationId = convId,
                    senderId = currentUserId,
                    content = content.trim(),
                    createdAt = java.time.Instant.now().toString()
                )

                repository.saveMessageToLocal(tempMessage)
                repository.sendMessage(tempMessage)

            } catch (e: Exception) {
                _error.value = ctx.getString(R.string.cannot_send_message)
            }
        }
    }

    fun sendMediaMessage(
        byteArray: ByteArray,
        fileName: String,
        isVideo: Boolean
    ) {
        viewModelScope.launch {

            try {
                val convId = ensureConversationId()
                val tempId = UUID.randomUUID().toString()

                val tempMessage = MessageModel(
                    id = tempId,
                    conversationId = convId,
                    senderId = currentUserId,
                    mediaType = if (isVideo) "video" else "image",
                    createdAt = java.time.Instant.now().toString()
                )

                repository.saveMessageToLocal(tempMessage)

                val mediaUrl = withContext(Dispatchers.IO) {
                    repository.uploadMedia(byteArray, fileName)
                }

                val finalMessage = tempMessage.copy(mediaUrl = mediaUrl)
                repository.sendMessage(finalMessage)
                repository.saveMessageToLocal(finalMessage)

            } catch (e: Exception) {
                _error.value = ctx.getString(R.string.cannot_send_message)
            }
        }
    }

    fun resetConversation() {
        cachedConversationId = null
        _currentConvId.value = ""
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMessagePermanently(messageId)
                repository.deleteMessageLocally(messageId)
            } catch (_: Exception) {
                _error.value = "Cannot delete message"
            }
        }
    }
}