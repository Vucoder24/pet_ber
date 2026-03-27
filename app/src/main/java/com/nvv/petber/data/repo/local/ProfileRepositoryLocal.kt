package com.nvv.petber.data.repo.local

import android.util.Log
import com.nvv.petber.data.dao.ProfileDao
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.repo.remote.ProfileRepositoryRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ProfileRepositoryLocal @Inject constructor(
    private val profileDao: ProfileDao,
    private val profileRepositoryRemote: ProfileRepositoryRemote,
) {
    fun getLocalUser(id: String) = profileDao.getUser(id)
    fun getLocalPets(id: String) = profileDao.getPets(id)
    fun getLocalPosts(id: String) = profileDao.getPosts(id)

    suspend fun updatePost(post: Post){
        profileDao.updatePost(post)
    }

    suspend fun syncProfile(userId: String) = withContext(Dispatchers.IO) {
        try {
            // Fetch all from Remote
            var remoteUser = profileRepositoryRemote.getUser(userId)
            val remotePets = profileRepositoryRemote.getPets(userId)
            val remotePosts = profileRepositoryRemote.getPosts(userId)
            val userStats = profileRepositoryRemote.getUserStats(userId)
            Log.d("Stats", "$userStats")
            remoteUser = remoteUser?.copy(
                postCount = userStats.postCount,
                followerCount = userStats.followerCount,
                followingCount = userStats.followingCount
            )
            // save to Local
            remoteUser?.let {
                profileDao.insertUser(it)
            }
            profileDao.insertPets(remotePets)
            profileDao.insertPosts(remotePosts)
        }catch (_: Exception){ }
    }

}