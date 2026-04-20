package com.nvv.petber.data.repo.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.nvv.petber.data.model.Follow
import com.nvv.petber.data.model.FollowUserUI
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.http.ContentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
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
        return try {
            val posts = supabase.from("posts")
                .select(
                    Columns.raw(
                        """*, users(*), post_media(*), 
                        post_likes(*)""".trimMargin()
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("post_likes.user_id", userId)
                    }
                }
                .decodeList<Post>()

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                supabase.from("pets").select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            posts.map { post ->
                post.apply {
                    isLiked = !postLikes.isNullOrEmpty()
                    taggedPets = petsList.filter { pet -> petIds?.contains(pet.id) == true }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileRepo", "Error getPosts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPosts(
        userId: String,
        offset: Int,
        limit: Int = 10,
        currentUserId: String
    ): List<Post> {
        return try {

            val posts = supabase.from("posts")
                .select(
                    Columns.raw(
                        """*, users(*), post_media(*), 
                    post_likes(*)""".trimMargin()
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        eq("post_likes.user_id", currentUserId)
                    }
                    order("created_at", Order.DESCENDING)
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Post>()

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                supabase.from("pets").select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            posts.map { post ->
                post.apply {
                    isLiked = !postLikes.isNullOrEmpty()
                    taggedPets = petsList.filter { pet -> petIds?.contains(pet.id) == true }
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileRepo", "Error getPosts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getUserStats(userId: String): UserStats {
        return try {
            val petFollowingResponse = supabase.from("pet_follows")
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

            val friendsCount = supabase.postgrest.rpc(
                function = "get_friends_count",
                parameters = mapOf("user_uuid" to userId)
            ).data.toLong()

            UserStats(
                friendsCount = friendsCount,
                petFollowingCount = petFollowingResponse.countOrNull() ?: 0,
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

    suspend fun uploadMedia(
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

    suspend fun deleteOldMedia(oldUrl: String?, bucket: String) {
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

    suspend fun updateProfile(user: User): User {
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

    suspend fun getFollowers(userId: String, currentUserId: String): List<FollowUserUI> {
        val relations = supabase.from("follows").select(
            Columns.raw("*, follower:users!follows_follower_id_fkey(*)")
        ) {
            filter { eq("following_id", userId) }
        }.decodeList<Follow>()

        val myFollowingIds = getMyFollowingIds(currentUserId)

        return relations.mapNotNull { relation ->
            relation.follower?.let { user ->
                FollowUserUI(user, myFollowingIds.contains(user.id))
            }
        }
    }

    suspend fun getFollowing(userId: String, currentUserId: String): List<FollowUserUI> {
        val relations = supabase.from("follows").select(
            Columns.raw("*, following:users!follows_following_id_fkey(*)")
        ) {
            filter { eq("follower_id", userId) }
        }.decodeList<Follow>()

        val myFollowingIds = getMyFollowingIds(currentUserId)

        return relations.mapNotNull { relation ->
            relation.following?.let { user ->
                FollowUserUI(user, myFollowingIds.contains(user.id))
            }
        }
    }

    suspend fun getFriends(userId: String, currentUserId: String): List<FollowUserUI> {
        val followers = getFollowers(userId, currentUserId).map { it.user }
        val following = getFollowing(userId, currentUserId).map { it.user }

        val mutualUsers = followers.intersect(following.toSet()).toList()
        val myFollowingIds = getMyFollowingIds(currentUserId)

        return mutualUsers.map { user ->
            FollowUserUI(user, myFollowingIds.contains(user.id))
        }
    }

    private suspend fun getMyFollowingIds(currentUserId: String): Set<String> {
        return try {
            supabase.from("follows").select(Columns.list("following_id")) {
                filter { eq("follower_id", currentUserId) }
            }.decodeList<Map<String, String>>().mapNotNull { it["following_id"] }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    suspend fun toggleFollow(
        followerId: String,
        followingId: String,
        isFollowing: Boolean
    ): Result<Boolean> {
        return try {
            if (isFollowing) {
                supabase.from("follows").delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
            } else {
                supabase.from("follows")
                    .insert(mapOf("follower_id" to followerId, "following_id" to followingId))
            }
            Result.success(!isFollowing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPetById(petId: String): Pet? {
        return try {
            supabase.from("pets")
                .select {
                    filter {
                        eq("id", petId)
                    }
                }
                .decodeSingleOrNull<Pet>()
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getPetPosts(
        petId: String,
        offset: Int,
        limit: Int = 10,
        currentUserId: String? = null
    ): List<Post> {
        return try {
            val likeFilter = currentUserId?.let { ".filter(user_id.eq.$it)" } ?: ""

            val posts = supabase.from("posts")
                .select(
                    Columns.raw(
                        """*, users(*), post_media(*), 
                        |post_likes(*)$likeFilter""".trimMargin()
                    )
                ) {
                    filter {
                        contains("pet_id", listOf(petId))
                    }
                    range(offset.toLong(), (offset + limit - 1).toLong())
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<Post>()

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                supabase.from("pets").select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            posts.map { post ->
                post.apply {
                    isLiked = !postLikes.isNullOrEmpty()
                    taggedPets = petsList.filter { pet -> petIds?.contains(pet.id) == true }
                }
            }
        } catch (e: Exception) {
            Log.d("ProfileRepositoryRemote", "Failed to fetch pet posts: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPetFollowing(userId: String): List<Pet> {
        return try {
            val response = supabase.from("pet_follows")
                .select(Columns.raw("*, pet:pets(*)")) {
                    filter {
                        eq("user_id", userId)
                    }
                }

            val joinedData = response.decodeList<PetFollowJoin>()
            joinedData.map { it.pet }

        } catch (e: Exception) {
            Log.e("ProfileRepo", "Error fetching pet following: ${e.message}")
            emptyList()
        }
    }

    suspend fun unfollowPet(userId: String, petId: String): Result<Unit> {
        return try {
            supabase.from("pet_follows").delete {
                filter {
                    eq("user_id", userId)
                    eq("pet_id", petId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Serializable
private data class PetFollowJoin(
    @SerialName("pet")
    val pet: Pet
)


data class UserStats(
    val petFollowingCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val friendsCount: Long = 0
)