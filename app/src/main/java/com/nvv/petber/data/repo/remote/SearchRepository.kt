package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.FollowRecord
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.PetFollowRecord
import com.nvv.petber.data.model.PetSearchResult
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class UserSearchResult(
    val user: User,
    val isFollowing: Boolean
)

class SearchRepository @Inject constructor(
    supabaseClient: SupabaseClient
) {
    private val db = supabaseClient.postgrest

    suspend fun searchUsers(
        query: String,
        currentUserId: String
    ): List<UserSearchResult> {
        val rawQuery = "%$query%"

        return try {
            val users = db["users"].select {
                filter {
                    neq("id", currentUserId)
                    if (query.isNotEmpty()) {
                        or {
                            ilike("username", rawQuery)
                            ilike("full_name", rawQuery)
                        }
                    }
                }
                limit(15)
            }.decodeList<User>()

            val followingIds = db["follows"].select {
                filter { eq("follower_id", currentUserId) }
            }.decodeList<FollowRecord>()
                .map { it.followingId }
                .toSet()

            users.map { user ->
                UserSearchResult(
                    user = user,
                    isFollowing = followingIds.contains(user.id)
                )
            }
        } catch (e: Exception) {
            println("SearchUsersError: ${e.message}")
            emptyList()
        }
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String, isFollowing: Boolean): Boolean {
        return try {
            if (isFollowing) {
                // Unfollow
                db["follows"].delete {
                    filter {
                        eq("follower_id", currentUserId)
                        eq("following_id", targetUserId)
                    }
                }
            } else {
                // Follow
                db["follows"].insert(mapOf(
                    "follower_id" to currentUserId,
                    "following_id" to targetUserId
                ))
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun searchPets(query: String, currentUserId: String): List<PetSearchResult> {
        val rawQuery = "%$query%"
        return try {
            val pets = db["pets"].select {
                filter {
                    neq("owner_id", currentUserId)

                    if (query.isNotEmpty()) {
                        ilike("name", rawQuery)
                    }
                }
                limit(15)
            }.decodeList<Pet>()

            val followingPetIds = db["pet_follows"].select {
                filter { eq("user_id", currentUserId) }
            }.decodeList<PetFollowRecord>()
                .map { it.petId }
                .toSet()

            pets.map { pet ->
                PetSearchResult(
                    pet = pet,
                    isFollowing = followingPetIds.contains(pet.id)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun toggleFollowPet(currentUserId: String, petId: String, isFollowing: Boolean): Boolean {
        return try {
            if (isFollowing) {
                db["pet_follows"].delete {
                    filter {
                        eq("user_id", currentUserId)
                        eq("pet_id", petId)
                    }
                }
            } else {
                db["pet_follows"].insert(mapOf("user_id" to currentUserId, "pet_id" to petId))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun searchPostsByHashtag(query: String, currentUserId: String): List<Post> {
        val rawQuery = "%$query%"
        return try {
            val posts = db["posts"].select(
                Columns.raw("*, users(*), post_media(*), " +
                        "post_likes(*)"
                )) {
                filter {
                    neq("user_id", currentUserId)

                    if (query.isNotEmpty())  {
                        ilike("hashtags", rawQuery)
                    }
                    eq("post_likes.user_id", currentUserId)
                }
                limit(15)
            }.decodeList<Post>()

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            posts.map { post ->
                post.apply {
                    isLiked = !postLikes.isNullOrEmpty()
                    taggedPets = petsList.filter { pet -> petIds?.contains(pet.id) == true }
                }
            }
        } catch (_: Exception) { emptyList() }
    }
}