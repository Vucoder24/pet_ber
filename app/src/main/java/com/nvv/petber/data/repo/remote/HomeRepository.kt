package com.nvv.petber.data.repo.remote

import android.util.Log
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.Story
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject


class HomeRepository @Inject constructor(
    supabaseClient: SupabaseClient
) {

    private val db = supabaseClient.postgrest

    // Fetch active stories (not expired), limited to followed users + self
    suspend fun fetchStories(): Result<List<Story>> {
        return try {
            val stories = db["stories"]
                .select(
                    columns = Columns.raw(
                        "*, users(id, username, full_name, avatar_url)"
                    )
                ) {
                    filter {
                        eq("is_expired", false)
                    }
                    order("created_at", Order.DESCENDING)
                    limit(30)
                }
                .decodeList<Story>()

            Log.d("HomeRepository", "Fetched $stories stories")
            Result.success(stories)
        } catch (e: Exception) {
            Log.e("HomeRepository", "fetchStories error: ${e.message}")
            Result.failure(e)
        }
    }

    // Fetch posts feed (from all users, ordered by newest)
    suspend fun fetchPosts(
        currentUserId: String,
        page: Int = 0,
        pageSize: Int = 30,
    ): Result<List<Post>> {
        return try {
            val from = page * pageSize
            val to = from + pageSize - 1

            val posts = db["posts"]
                .select(
                    columns = Columns.raw(
                        """
                    *,
                    users(id, username, full_name, avatar_url),
                    post_media(id, post_id, media_url, media_type),
                    post_likes(*).filter(user_id.eq.$currentUserId)
                    """.trimIndent()
                    )
                ) {
                    order("created_at", Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<Post>().map {
                    it.apply { isLiked = !postLikes.isNullOrEmpty() }
                }

            // 2. Lấy danh sách Pet liên quan
            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            // 3. Map dữ liệu
            posts.forEach { post ->
                post.isLiked = !post.postLikes.isNullOrEmpty()
                post.taggedPets = petsList.filter { pet -> post.petIds?.contains(pet.id) == true }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Log.e("HomeRepository", "fetchPosts error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun toggleLike(
        postId: String,
        userId: String,
        isCurrentlyLiked: Boolean
    ): Result<Boolean> {
        return try {
            if (isCurrentlyLiked) {
                // Unlike: delete the row
                db["post_likes"].delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                // Decrement like_count using RPC or manual update
                db.rpc("decrement_like_count", mapOf("p_post_id" to postId))
            } else {
                // Like: insert row
                db["post_likes"].insert(mapOf("post_id" to postId, "user_id" to userId))
                // Increment like_count
                db.rpc("increment_like_count", mapOf("p_post_id" to postId))
            }
            Result.success(!isCurrentlyLiked)
        } catch (e: Exception) {
            Log.e("HomeRepository", "toggleLike error: ${e.message}")
            Result.failure(e)
        }
    }

    @OptIn(SupabaseExperimental::class)
    fun getStoriesFlow(): Flow<List<Story>> {
        return db.from("stories")
            .selectAsFlow(
                primaryKey = Story::id,
            )
    }

    suspend fun fetchPostById(postId: String, currentUserId: String): Result<Post> {
        return try {
            val post = db["posts"]
                .select(
                    columns = Columns.raw(
                        """
                    *,
                    users(id, username, full_name, avatar_url),
                    post_media(id, post_id, media_url, media_type),
                    post_likes(*).filter(user_id.eq.$currentUserId)
                    """.trimIndent()
                    )
                ) {
                    filter {
                        eq("id", postId)
                    }
                }
                .decodeSingle<Post>()

            val petIds = post.petIds ?: emptyList()
            val petsList = if (petIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", petIds) } }.decodeList<Pet>()
            } else emptyList()

            post.isLiked = !post.postLikes.isNullOrEmpty()
            post.taggedPets = petsList

            post.isLiked = !post.postLikes.isNullOrEmpty()

            Result.success(post)
        } catch (e: Exception) {
            Log.e("HomeRepository", "fetchPostById error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun toggleSavePost(
        postId: String,
        userId: String,
        isCurrentlySaved: Boolean
    ): Result<Boolean> {
        return try {
            if (isCurrentlySaved) {
                db["saved_posts"].delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
            } else {
                db["saved_posts"].insert(mapOf("post_id" to postId, "user_id" to userId))
            }
            Result.success(!isCurrentlySaved)
        } catch (e: Exception) {
            Log.e("HomeRepository", "toggleSavePost error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchSavedPosts(userId: String): Result<List<Post>> {
        return try {
            val response = db["saved_posts"]
                .select(columns = Columns.raw("post_id, posts(*)")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<SavedPostResponse>()

            val posts = response.map { it.post }

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            posts.forEach { post ->
                post.taggedPets = petsList.filter { pet -> post.petIds?.contains(pet.id) == true }
            }

            Result.success(response.map { it.post })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isPostSaved(postId: String, userId: String): Result<Boolean> {
        return try {
            val result = db["saved_posts"]
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeList<Map<String, String>>()
            Result.success(result.isNotEmpty())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun incrementShareCount(postId: String): Result<Unit> {
        return try {
            db.rpc("increment_share_count", mapOf("p_post_id" to postId))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("HomeRepository", "incrementShareCount error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkFollowStatus(followerId: String, followingId: String): Result<Boolean> {
        return try {
            val result = db["follows"]
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
                .decodeList<Map<String, String>>()
            Result.success(result.isNotEmpty())
        } catch (e: Exception) {
            Log.e("HomeRepository", "checkFollowStatus error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun toggleFollowUser(
        followerId: String,
        followingId: String,
        isCurrentlyFollowing: Boolean
    ): Result<Boolean> {
        return try {
            if (isCurrentlyFollowing) {
                db["follows"].delete {
                    filter {
                        eq("follower_id", followerId)
                        eq("following_id", followingId)
                    }
                }
            } else {
                db["follows"].insert(
                    mapOf(
                        "follower_id" to followerId,
                        "following_id" to followingId
                    )
                )
            }
            Result.success(!isCurrentlyFollowing)
        } catch (e: Exception) {
            Log.e("HomeRepository", "toggleFollowUser error: ${e.message}")
            Result.failure(e)
        }
    }
}

@Serializable
data class SavedPostResponse(
    @SerialName("post_id")
    val postId: String,

    @SerialName("posts")
    val post: Post
)