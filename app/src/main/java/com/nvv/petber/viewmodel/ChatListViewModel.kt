package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.Conversation
import com.nvv.petber.data.repo.remote.ChatRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val repository: ChatRepository,
    @ApplicationContext val ctx: Context
) : ViewModel() {

    val currentUserId = SharePrefUtils.getCurrentUserId(ctx)
    val conversations = repository.observeLocalConversations()
        .stateIn(
            viewModelScope, SharingStarted.Lazily, emptyList()
        )

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Conversation>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private var currentPage = 0
    private var isLastPage = false

    init {
        repository.syncConversationsRealtime(currentUserId)
            .launchIn(viewModelScope)
        loadConversations()
    }

    fun loadConversations(isRefresh: Boolean = false) {
        if (_isLoading.value || (isLastPage && !isRefresh)) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (isRefresh) {
                    currentPage = 0
                    isLastPage = false
                }

                val hasMore = repository.refreshConversations(currentPage, 20, currentUserId)

                if (hasMore.isEmpty()) {
                    isLastPage = true
                } else {
                    currentPage++
                }
            } catch (_: Exception) {
                _error.emit(ctx.getString(R.string.error_fetch_data))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    repository.searchConversations(query)
                }
                _searchResults.value = results
            } catch (_: Exception) {
                _searchResults.value = emptyList()
            }
        }
    }

    fun markAsRead(conversationId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markAsReadLocal(conversationId)
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteConversationRpc(conversationId)
                }
            } catch (_: Exception) {
                _error.emit(ctx.getString(R.string.error_delete_conversation))
                // Handle error
            }
        }
    }
}