package com.nvv.petber.data.repo.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.http.ContentType
import java.util.UUID
import javax.inject.Inject

class ProfileRepositoryRemote @Inject constructor(
    private val supabase: SupabaseClient,
    private val context: Context
) {

    suspend fun getUser(userId: String): User? {
        return supabase.from("users")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull()
    }

    suspend fun getPets(userId: String): List<Pet> {
        return supabase.from("pets")
            .select {
                filter {
                    eq("owner_id", userId)
                }
            }
            .decodeList()
    }

    suspend fun getPosts(userId: String): List<Post> {
        return supabase.from("posts")
            .select(
                Columns.raw(
                    """*, users(*), pets(*), post_media(*), 
                    |post_likes(*).filter(user_id.eq.$userId)""".trimMargin()
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<Post>().map {
                it.apply { isLiked = !postLikes.isNullOrEmpty() }
            }
    }

    suspend fun getUserStats(userId: String): UserStats {
        return try {
            val postsResponse = supabase.from("posts")
                .select {
                    filter { eq("user_id", userId) }
                    count(Count.EXACT)
                }

            val followersResponse = supabase.from("follows")
                .select {
                    filter { eq("following_id", userId) }
                    count(Count.EXACT)
                }

            val followingResponse = supabase.from("follows")
                .select {
                    filter { eq("follower_id", userId) }
                    count(Count.EXACT)
                }

            UserStats(
                postCount = postsResponse.countOrNull() ?: 0,
                followerCount = followersResponse.countOrNull() ?: 0,
                followingCount = followingResponse.countOrNull() ?: 0
            )
        } catch (_: Exception) {
            UserStats()
        }
    }

    suspend fun updateAvatar(userId: String, uri: Uri, oldAvatarUrl: String?): User {
        val avatarUrl = uploadMedia(uri, userId, "user")

        val userData = supabase.from("users").update(
            {
                set("avatar_url", avatarUrl)
            }
        ) {
            filter { eq("id", userId) }
            select()
        }.decodeSingle<User>()

        deleteOldMedia(oldAvatarUrl, "user")
        return userData
    }

    suspend fun updateCover(userId: String, uri: Uri, oldCoverUrl: String?): User {
        val coverUrl = uploadMedia(uri, userId, "user")

        val userData = supabase.from("users").update(
            {
                set("cover_url", coverUrl)
            }
        ) {
            filter { eq("id", userId) }
            select()
        }.decodeSingle<User>()

        deleteOldMedia(oldCoverUrl, "user")
        return userData
    }

    private suspend fun uploadMedia(
        uri: Uri,
        userId: String,
        bucket: String
    ): String {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val fileName = "$userId/${UUID.randomUUID()}.$extension"

        supabase.storage[bucket].upload(
            path = fileName,
            uri = uri
        ) {
            contentType = ContentType.parse(mimeType)
            upsert = false
        }

        return supabase.storage[bucket].publicUrl(fileName)
    }

    private suspend fun deleteOldMedia(oldUrl: String?, bucket: String) {
        if (oldUrl.isNullOrEmpty()) return
        try {
            val pathIdentifier = "/object/public/$bucket/"
            if (oldUrl.contains(pathIdentifier)) {
                val filePath = oldUrl.substringAfter(pathIdentifier)
                supabase.storage[bucket].delete(filePath)
            }
        } catch (e: Exception) {
            Log.e("ProfileRepositoryRemote", "Failed to delete old media: ${e.message}")
        }
    }

    suspend fun updateProfile(user: User) : User{
        return supabase.from("users").update(
            {
                set("full_name", user.fullName)
                set("username", user.username)
                set("bio", user.bio)
                set("address", user.address)
                set("phone", user.phone)
                set("gender", user.gender)
                set("birthday", user.birthday)
                set("hobbies", user.hobbies)
            }
        ) {
            filter {
                eq("id", user.id)
            }
            select()
        }.decodeSingle<User>()
    }
}

data class UserStats(
    val postCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0
)