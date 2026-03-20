package com.nvv.petber.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.remote.ProfileRepository
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepo: ProfileRepository,
    context: Context
) : ViewModel() {
    private val currentUserId = SharePrefUtils.getCurrentUserId(context)

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _pets = MutableLiveData<List<Pet>>()
    val pets: LiveData<List<Pet>> = _pets

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadProfile(currentUserId)
    }

    fun loadProfile(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.postValue(true)
                val user = profileRepo.getUser(userId)
                val pets = profileRepo.getPets(userId)
                val posts = profileRepo.getPosts(userId)

                _user.postValue(user)
                _pets.postValue(pets)
                _posts.postValue(posts)
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading profile", e)
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}

