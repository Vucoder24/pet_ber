package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.data.repo.remote.StoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class StoryHostViewModel @Inject constructor(
    private val repository: StoryRepository,
    @ApplicationContext val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<StoryHostState>(StoryHostState.Idle)
    val uiState = _uiState.asStateFlow()

    fun loadFromData(jsonStories: String, initialPosition: Int) {
        viewModelScope.launch {
            try {
                val groups = Json.decodeFromString<List<UserStoryGroup>>(jsonStories)
                _uiState.value = StoryHostState.Success(groups, initialPosition)
            } catch (_: Exception) {
                _uiState.value = StoryHostState.Error("Invalid Data")
            }
        }
    }

    fun loadFromApi(storyId: String) {
        viewModelScope.launch {
            _uiState.value = StoryHostState.Loading
            try {
                val result = repository.getStoryGroupByStoryId(storyId)
                _uiState.value = StoryHostState.Success(result.groups, result.initialIndex)
            } catch (e: Exception) {
                if (e.message == "STORY_UNAVAILABLE") {
                    _uiState.value = StoryHostState.Error(context.getString(R.string.story_unavailable))
                } else {
                    _uiState.value = StoryHostState.Error(context.getString(R.string.something_went_wrong))
                }
            }
        }
    }
}

sealed class StoryHostState {
    object Idle : StoryHostState()
    object Loading : StoryHostState()
    data class Success(val groups: List<UserStoryGroup>, val initialIndex: Int) : StoryHostState()
    data class Error(val message: String) : StoryHostState()
}