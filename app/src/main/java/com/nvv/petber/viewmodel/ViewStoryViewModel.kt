package com.nvv.petber.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.UserStoryGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewStoryViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(StoryUiState())
    val uiState: StateFlow<StoryUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<StoryNavigationEvent>()
    val navigationEvent: SharedFlow<StoryNavigationEvent> = _navigationEvent.asSharedFlow()

    private var timerJob: Job? = null
    private val tickInterval = 16L

    fun initData(storyGroup: UserStoryGroup) {
        if (_uiState.value.storyGroup == null) {
            _uiState.update { it.copy(storyGroup = storyGroup) }
            loadStory(0)
        }
    }

    private fun loadStory(index: Int) {
        timerJob?.cancel()
        val group = _uiState.value.storyGroup ?: return
        val story = group.stories.getOrNull(index) ?: return

        val isVideo = story.mediaType.contains("video", ignoreCase = true)
        val duration = if (isVideo) -1L else 5000L

        _uiState.update {
            it.copy(
                currentIndex = index,
                currentProgress = 0L,
                currentDuration = duration,
                isPaused = false
            )
        }

        if (!isVideo) {
            startImageTimer()
        }
    }

    fun setVideoDurationAndStart(duration: Long) {
        _uiState.update { it.copy(currentDuration = duration) }
    }

    fun syncVideoProgress(progress: Long) {
        _uiState.update { it.copy(currentProgress = progress) }
    }

    private fun startImageTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            val startProgress = _uiState.value.currentProgress
            val duration = _uiState.value.currentDuration
            var lastRealTime = System.currentTimeMillis()
            var accumulatedProgress = startProgress

            while (accumulatedProgress < duration) {
                delay(tickInterval)
                if (!_uiState.value.isPaused) {
                    val now = System.currentTimeMillis()
                    val delta = now - lastRealTime
                    accumulatedProgress += delta
                    _uiState.update {
                        it.copy(currentProgress = accumulatedProgress.coerceAtMost(duration))
                    }
                    lastRealTime = now
                } else {
                    lastRealTime = System.currentTimeMillis()
                }
            }
            nextStory()
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isPaused = true) }
    }

    fun resumeTimer() {
        _uiState.update { it.copy(isPaused = false) }
        // Only restart Timer if it was canceled mid-stream AND viewing images
        val state = _uiState.value
        val story = state.storyGroup?.stories?.getOrNull(state.currentIndex)
        val isVideo = story?.mediaType?.contains("video", ignoreCase = true) == true

        if (!isVideo && timerJob?.isActive != true && state.currentProgress < state.currentDuration) {
            startImageTimer()
        }
    }

    fun nextStory() {
        timerJob?.cancel()
        val state = _uiState.value
        val group = state.storyGroup ?: return
        if (state.currentIndex < group.stories.size - 1) {
            loadStory(state.currentIndex + 1)
        } else {
            viewModelScope.launch { _navigationEvent.emit(StoryNavigationEvent.NEXT_USER) }
        }
    }

    fun previousStory() {
        timerJob?.cancel()
        val state = _uiState.value
        when {
            state.currentIndex > 0 -> loadStory(state.currentIndex - 1)
            else -> {
                viewModelScope.launch {
                    _navigationEvent.emit(StoryNavigationEvent.PREV_USER)
                }
            }
        }
    }

    private fun restartCurrentMedia() {
        timerJob?.cancel()
        _uiState.update { it.copy(currentProgress = 0L, isPaused = false) }

        val state = _uiState.value
        val story = state.storyGroup?.stories?.getOrNull(state.currentIndex)
        val isVideo = story?.mediaType?.contains("video", ignoreCase = true) == true

        if (!isVideo) {
            startImageTimer()
        }

        viewModelScope.launch {
            _navigationEvent.emit(StoryNavigationEvent.RESTART_CURRENT_STORY)
        }
    }
}

data class StoryUiState(
    val storyGroup: UserStoryGroup? = null,
    val currentIndex: Int = 0,
    val currentProgress: Long = 0L,
    val currentDuration: Long = 5000L,
    val isPaused: Boolean = false
)

enum class StoryNavigationEvent {
    NEXT_USER, PREV_USER, RESTART_CURRENT_STORY
}