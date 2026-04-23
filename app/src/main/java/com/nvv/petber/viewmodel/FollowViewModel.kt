package com.nvv.petber.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.R
import com.nvv.petber.data.model.FollowUserUI
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.toast
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


data class FollowUiState(
    val users: List<FollowUserUI> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class PetFollowUiState(
    val pets: List<Pet> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)


@HiltViewModel
class FollowViewModel @Inject constructor(
    private val repository: ProfileRepositoryRemote,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val currentUserId = SharePrefUtils.getCurrentUserId(context)

    private val _followersState = MutableStateFlow(FollowUiState())
    val followersState: StateFlow<FollowUiState> = _followersState.asStateFlow()

    private val _followingState = MutableStateFlow(FollowUiState())
    val followingState: StateFlow<FollowUiState> = _followingState.asStateFlow()

    private val _friendsState = MutableStateFlow(FollowUiState())
    val friendsState: StateFlow<FollowUiState> = _friendsState.asStateFlow()

    private val _petFollowingState = MutableStateFlow(PetFollowUiState())
    val petFollowingState: StateFlow<PetFollowUiState> = _petFollowingState.asStateFlow()

    fun loadData(userId: String, type: Int) {
        viewModelScope.launch {
            try {
                when (type) {
                    0 -> { // Pet Following
                        _petFollowingState.value = _petFollowingState.value.copy(isLoading = true)
                        val data = withContext(Dispatchers.IO) {
                            repository.getPetFollowing(userId)
                        }
                        _petFollowingState.value = PetFollowUiState(pets = data, isLoading = false)
                    }

                    1 -> { // Followers
                        _followersState.value = _followersState.value.copy(isLoading = true)
                        val data = withContext(Dispatchers.IO) {
                            repository.getFollowers(userId, currentUserId)
                        }
                        _followersState.value = FollowUiState(users = data, isLoading = false)
                    }

                    2 -> { // Following
                        _followingState.value = _followingState.value.copy(isLoading = true)
                        val data = withContext(Dispatchers.IO) {
                            repository.getFollowing(userId, currentUserId)
                        }
                        _followingState.value = FollowUiState(users = data, isLoading = false)
                    }

                    3 -> { // Friends
                        _friendsState.value = _friendsState.value.copy(isLoading = true)
                        val data = withContext(Dispatchers.IO) {
                            repository.getFriends(userId, currentUserId)
                        }
                        _friendsState.value = FollowUiState(users = data, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                when (type) {
                    0 -> _petFollowingState.value =
                        PetFollowUiState(isLoading = false, error = e.message)

                    1 -> _followersState.value = FollowUiState(isLoading = false, error = e.message)
                    2 -> _followingState.value = FollowUiState(isLoading = false, error = e.message)
                    3 -> _friendsState.value = FollowUiState(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun toggleFollow(targetUserId: String, isCurrentlyFollowing: Boolean, type: Int) {
        viewModelScope.launch {
            //Optimistic update
            val updateList = { state: MutableStateFlow<FollowUiState> ->
                val updated = state.value.users.map {
                    if (it.user.id == targetUserId) it.copy(isFollowing = !isCurrentlyFollowing) else it
                }
                state.value = state.value.copy(users = updated)
            }
            when (type) {
                1 -> updateList(_followersState)
                2 -> updateList(_followingState)
                3 -> updateList(_friendsState)
            }

            // call API
            val result = withContext(Dispatchers.IO) {
                repository.toggleFollow(currentUserId, targetUserId, isCurrentlyFollowing)
            }
            result.onFailure {
                // Return the old state if the API call fails
                val revertList = { state: MutableStateFlow<FollowUiState> ->
                    val reverted = state.value.users.map {
                        if (it.user.id == targetUserId) it.copy(isFollowing = isCurrentlyFollowing) else it
                    }
                    state.value = state.value.copy(
                        users = reverted,
                        error = context.getString(R.string.error_action)
                    )
                }
                when (type) {
                    1 -> revertList(_followersState)
                    2 -> revertList(_followingState)
                    3 -> revertList(_friendsState)
                }
            }
        }
    }

    fun unfollowPet(petId: String, targetUserId: String) {
        viewModelScope.launch {
            val currentList = _petFollowingState.value.pets

            _petFollowingState.value = _petFollowingState.value.copy(
                pets = currentList.filter { it.id != petId }
            )

            val result = withContext(Dispatchers.IO) {
                repository.unfollowPet(currentUserId, petId)
            }
            result.onFailure {
                loadData(targetUserId, 0)
                launch(Dispatchers.Main) {
                    context.toast(context.getString(R.string.error_action))
                }
            }
        }
    }
}