package com.nvv.petber.data.repo.remote

import android.util.Log
import com.nvv.petber.data.dao.PostDao
import com.nvv.petber.data.dao.StoryDao
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.Story
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class UserIdResponse(val user_id: String)

class HomeRepository @Inject constructor(
    val supabaseClient: SupabaseClient,
    private val postDao: PostDao,
    private val storyDao: StoryDao
) {

    private val db = supabaseClient.postgrest

    fun observeOfflinePosts(): Flow<List<Post>> = postDao.getPostsFlow()

    fun observeOfflineStories(): Flow<List<Story>> = storyDao.getStoriesFlow()

    // Fetch active stories (not expired), limited to followed users + self
    suspend fun fetchStories(page: Int = 0, userLimit: Int = 10): Result<List<Story>> {
        return try {
            val offset = page * userLimit

            val userIdsResponse = db.rpc(
                function = "get_active_story_users",
                parameters = mapOf("p_offset" to offset, "p_limit" to userLimit)
            ).decodeList<UserIdResponse>()

            val userIds = userIdsResponse.map { it.user_id }

            if (userIds.isEmpty()) {
                return Result.success(emptyList())
            }

            val stories = db["stories"]
                .select(
                    columns = Columns.raw("*, users(id, username, full_name, avatar_url)")
                ) {
                    filter {
                        eq("is_expired", false)
                        isIn("user_id", userIds)
                    }
                }
                .decodeList<Story>()

            val grouped = stories.groupBy { it.userId }

            val sortedStories = userIds.flatMap { userId ->
                grouped[userId]
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()
            }

            if (page == 0) {
                storyDao.clearAllStories()
            }
            storyDao.insertStories(sortedStories)

            Result.success(sortedStories)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fetch posts feed (from all users, ordered by newest)
    suspend fun fetchPosts(
        currentUserId: String,
        page: Int = 0,
        pageSize: Int = 15,
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
                    post_likes(*)
                    """.trimIndent()
                    )
                ) {
                    order("created_at", Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                    filter {
                        filter("deleted_at", FilterOperator.IS, null)
                        eq("post_likes.user_id", currentUserId)
                    }
                }
                .decodeList<Post>().map {
                    it.apply { isLiked = !postLikes.isNullOrEmpty() }
                }

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            // 3. Map dữ liệu
            posts.forEach { post ->
                post.taggedPets = petsList.filter { pet -> post.petIds?.contains(pet.id) == true }
            }

            if (page == 0) {
                postDao.clearAllPosts()
            }
            postDao.insertPosts(posts)

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

    suspend fun toggleLike(
        postId: String,
        userId: String,
        isCurrentlyLiked: Boolean,
        currentLikeCount: Int
    ): Result<Boolean> {
        val newIsLiked = !isCurrentlyLiked
        val newLikeCount = if (newIsLiked) currentLikeCount + 1 else currentLikeCount - 1

        postDao.updatePostLike(postId, newIsLiked, newLikeCount)

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
            postDao.updatePostLike(postId, isCurrentlyLiked, currentLikeCount)
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
                    post_likes(*)
                    """.trimIndent()
                    )
                ) {
                    filter {
                        eq("id", postId)
                        filter("deleted_at", FilterOperator.IS, null)
                        eq("post_likes.user_id", currentUserId)
                    }
                }
                .decodeSingle<Post>()

            val petIds = post.petIds ?: emptyList()
            val petsList = if (petIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", petIds) } }.decodeList<Pet>()
            } else emptyList()

            post.isLiked = !post.postLikes.isNullOrEmpty()
            post.taggedPets = petsList

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

    suspend fun fetchSavedPosts(
        userId: String,
        page: Int = 0,
        pageSize: Int = 10
    ): Result<List<Post>> {
        return try {
            val from = page * pageSize
            val to = from + pageSize - 1

            val response = db["saved_posts"]
                .select(
                    columns = Columns.raw(
                        """post_id,
                                posts!inner(
                                    *,
                                    users(id, username, full_name, avatar_url),
                                    post_media(id, post_id, media_url, media_type),
                                    post_likes(*)
                                )""".trimIndent()
                    )
                ) {
                    filter {
                        eq("user_id", userId)
                        filter("deleted_at", FilterOperator.IS, null)
                        eq("posts.post_likes.user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<SavedPostResponse>()

            val posts = response.map {
                it.post.apply {
                    isLiked = !postLikes.isNullOrEmpty()
                }
            }

            val allPetIds = posts.flatMap { it.petIds ?: emptyList() }.distinct()
            val petsList = if (allPetIds.isNotEmpty()) {
                db["pets"].select { filter { isIn("id", allPetIds) } }.decodeList<Pet>()
            } else emptyList()

            posts.forEach { post ->
                post.taggedPets = petsList.filter { pet -> post.petIds?.contains(pet.id) == true }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Log.e("HomeRepository", "fetchSavedPosts error: ${e.message}")
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

    suspend fun softDeletePost(postId: String): Result<Unit> {
        return try {
            val currentTime = java.time.Instant.now().toString()
            db["posts"].update(mapOf("deleted_at" to currentTime)) {
                filter {
                    eq("id", postId)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("HomeRepository", "softDeletePost error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchDeletedPosts(
        userId: String,
        page: Int = 0,
        pageSize: Int = 10
    ): Result<List<Post>> {
        return try {
            val from = page * pageSize
            val to = from + pageSize - 1

            val posts = db["posts"]
                .select(columns = Columns.raw("*, users(*), post_media(*), post_likes(*)")) {
                    filter {
                        eq("user_id", userId)
                        filterNot("deleted_at", FilterOperator.IS, "null")
                        eq("post_likes.user_id", userId)
                    }
                    order("deleted_at", Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<Post>().map {
                    it.apply { isLiked = !postLikes.isNullOrEmpty() }
                }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun restorePost(postId: String): Result<Unit> {
        return try {
            db["posts"].update(mapOf("deleted_at" to null)) {
                filter { eq("id", postId) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hardDeletePostComplete(post: Post): Result<Unit> {
        return try {
            val mediaUrls = post.postMedia?.map { it.mediaUrl } ?: emptyList()

            db.rpc("hard_delete_post_v2", mapOf("p_post_id" to post.id))

            if (mediaUrls.isNotEmpty()) {
                deletePostMediaFromStorage(mediaUrls)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("HomeRepository", "Lỗi xóa vĩnh viễn: ${e.message}")
            Result.failure(e)
        }
    }

    private fun extractFilePathFromUrl(url: String, bucket: String): String? {
        val lookFor = "/object/public/$bucket/"
        return if (url.contains(lookFor)) {
            url.substringAfter(lookFor)
        } else null
    }

    suspend fun deletePostMediaFromStorage(urls: List<String>) {
        urls.forEach { url ->
            val path = extractFilePathFromUrl(url, "posts")
            if (path != null) {
                try {
                    supabaseClient.storage.from("posts").delete(path)
                } catch (e: Exception) {
                    Log.e("HomeRepository", "Lỗi xóa file storage: ${e.message}")
                }
            }
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