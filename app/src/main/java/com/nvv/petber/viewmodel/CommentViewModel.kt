package com.nvv.petber.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Comment
import com.nvv.petber.data.model.CommentNode
import com.nvv.petber.data.model.CommentUI
import com.nvv.petber.data.repo.remote.CommentRepositoryRemote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val repository: CommentRepositoryRemote
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommentUiState())
    val uiState: StateFlow<CommentUiState> = _uiState.asStateFlow()

    fun loadComments(postId: String, userId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = withContext(Dispatchers.IO) {
                repository.fetchComments(postId, userId)
            }
            result.onSuccess { comments ->
                    _uiState.value = _uiState.value.copy(
                        rawComments = comments,
                        isLoading = false,
                        flatComments = buildFlatList(comments, _uiState.value.expandedIds)
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isLoading = false, error = error.message)
                }
        }
    }

    fun postComment(postId: String, userId: String, content: String) {
        if (content.isBlank()) return
        val parentId = _uiState.value.replyingTo?.id
        _uiState.value = _uiState.value.copy(isSending = true, sendSuccess = false, error = null)

        // Automatically expand parent comment if replying
        val newExpandedIds =
            if (parentId != null) _uiState.value.expandedIds + parentId else _uiState.value.expandedIds
        _uiState.value = _uiState.value.copy(expandedIds = newExpandedIds)

        viewModelScope.launch(Dispatchers.IO) {
            repository.addComment(postId, userId, content, parentId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        sendSuccess = true,
                        replyingTo = null
                    )
                    loadComments(postId, userId)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(isSending = false, error = error.message)
                }
        }
    }

    fun toggleLike(comment: Comment, currentUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Optimistic Update
            val updatedComments = _uiState.value.rawComments.map {
                if (it.id == comment.id) {
                    it.copy(
                        isLiked = !it.isLiked,
                        likeCount = if (it.isLiked) it.likeCount - 1 else it.likeCount + 1
                    )
                } else it
            }
            _uiState.value = _uiState.value.copy(
                rawComments = updatedComments,
                flatComments = buildFlatList(updatedComments, _uiState.value.expandedIds)
            )

            repository.toggleLikeCmt(comment.id, currentUserId)
        }
    }

    fun toggleExpand(commentId: String) {
        val currentExpanded = _uiState.value.expandedIds.toMutableSet()
        if (currentExpanded.contains(commentId)) {
            currentExpanded.remove(commentId)
        } else {
            currentExpanded.add(commentId)
        }
        _uiState.value = _uiState.value.copy(
            expandedIds = currentExpanded,
            flatComments = buildFlatList(_uiState.value.rawComments, currentExpanded)
        )
    }

    fun setReplyingTo(comment: Comment?) {
        _uiState.value = _uiState.value.copy(replyingTo = comment)
    }

    private fun buildFlatList(comments: List<Comment>, expandedIds: Set<String>): List<CommentUI> {
        val nodeMap =
            comments.associate {
                it.id to CommentNode(it, expandedIds.contains(it.id))
            }
        val rootNodes = mutableListOf<CommentNode>()

        // build tree
        comments.forEach { comment ->
            if (comment.parentCommentId == null) {
                rootNodes.add(nodeMap[comment.id]!!)
            } else {
                nodeMap[comment.parentCommentId]?.replies?.add(nodeMap[comment.id]!!)
            }
        }

        val flatList = mutableListOf<CommentUI>()

        // Recursive flat staging
        fun flatten(node: CommentNode, depth: Int) {
            flatList.add(
                CommentUI(
                    comment = node.comment,
                    depth = depth,
                    hasReplies = node.replies.isNotEmpty(),
                    isExpanded = node.isExpanded,
                    replyCount = node.replies.size
                )
            )
            if (node.isExpanded) {
                node.replies.forEach { flatten(it, depth + 1) }
            }
        }

        rootNodes.forEach { flatten(it, 0) }
        return flatList
    }

    fun deleteComment(commentId: String) {
        val previousRawComments = _uiState.value.rawComments
        val previousExpandedIds = _uiState.value.expandedIds

        val updatedRawComments = previousRawComments.filterNot {
            it.id == commentId || it.parentCommentId == commentId
        }

        _uiState.value = _uiState.value.copy(
            rawComments = updatedRawComments,
            flatComments = buildFlatList(updatedRawComments, previousExpandedIds)
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteComment(commentId)
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        rawComments = previousRawComments,
                        flatComments = buildFlatList(previousRawComments, previousExpandedIds),
                        error = "DELETE_FAILED"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class CommentUiState(
    val flatComments: List<CommentUI> = emptyList(),
    val rawComments: List<Comment> = emptyList(),
    val expandedIds: Set<String> = emptySet(),
    val replyingTo: Comment? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val sendSuccess: Boolean = false,
    val error: String? = null
)