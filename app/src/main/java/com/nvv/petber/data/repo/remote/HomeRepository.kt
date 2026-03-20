package com.nvv.petber.data.repo.remote

import android.util.Log
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
                        "id," +
                                " user_id, media_url, media_type, created_at, " +
                                "expires_at, users(id, username, full_name, avatar_url)"
                    )
                ) {
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
                    users(id, username, avatar_url),
                    pets(id, name, breed, owner_id),
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
}