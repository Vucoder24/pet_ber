package com.nvv.petber.data.repo.local

import android.net.Uri
import android.util.Log
import com.nvv.petber.data.dao.ProfileDao
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileRepositoryLocal @Inject constructor(
    private val profileDao: ProfileDao,
    private val profileRepositoryRemote: ProfileRepositoryRemote,
) {
    fun getLocalUser(id: String) = profileDao.getUser(id)
    fun getLocalPets(id: String) = profileDao.getPets(id)
    fun getLocalPosts(id: String) = profileDao.getPosts(id)

    suspend fun deletePostById(postId: String) {
        profileDao.deletePostById(postId)
    }
    suspend fun updatePost(post: Post) {
        profileDao.updatePost(post)
    }

    suspend fun updateUser(user: User){
        profileDao.updateUser(user)
    }

    suspend fun syncUser(userId: String) = withContext(Dispatchers.IO) {
        try {
            var remoteUser = profileRepositoryRemote.getUser(userId)
            val userStats = profileRepositoryRemote.getUserStats(userId)
            remoteUser = remoteUser?.copy(
                friendsCount = userStats.friendsCount,
                petFollowingCount = userStats.petFollowingCount,
                followerCount = userStats.followerCount,
                followingCount = userStats.followingCount
            )
            remoteUser?.let {
                profileDao.insertUser(it)
            }
        } catch (e: Exception) {
            Log.e("ProfileRepoLocal", "Sync User Error: ${e.message}")
        }
    }

    suspend fun syncPets(userId: String) = withContext(Dispatchers.IO) {
        try {
            val remotePets = profileRepositoryRemote.getPets(userId)
            Log.d("ProfileRepoLocal", "$remotePets")
            profileDao.syncPetsData(userId, remotePets)
        } catch (e: Exception) {
            Log.e("ProfileRepoLocal", "Sync Pets Error: ${e.message}")
        }
    }

    suspend fun syncPosts(userId: String) = withContext(Dispatchers.IO) {
        try {
            val remotePosts = profileRepositoryRemote.getPosts(userId)
            profileDao.syncPostsData(userId, remotePosts)
        } catch (e: Exception) {
            Log.e("ProfileRepoLocal", "Sync Posts Error: ${e.message}")
        }
    }

    suspend fun syncProfile(userId: String) = withContext(Dispatchers.IO) {
        val userDeferred = async { syncUser(userId) }
        val petsDeferred = async { syncPets(userId) }
        val postsDeferred = async { syncPosts(userId) }

        awaitAll(userDeferred, petsDeferred, postsDeferred)
    }

    suspend fun updateAvatar(userId: String, uri: Uri, oldAvatarUrl: String?) =
        withContext(Dispatchers.IO) {
            val userData = profileRepositoryRemote.updateAvatar(userId, uri, oldAvatarUrl)
            updateUser(userData)
        }

    suspend fun updateCover(userId: String, uri: Uri, oldCoverUrl: String?) =
        withContext(Dispatchers.IO) {
            val userData = profileRepositoryRemote.updateCover(userId, uri, oldCoverUrl)
            updateUser(userData)
        }

    suspend fun updateProfile(user: User) = withContext(Dispatchers.IO){
        val userData = profileRepositoryRemote.updateProfile(user)
        updateUser(userData)
    }

}