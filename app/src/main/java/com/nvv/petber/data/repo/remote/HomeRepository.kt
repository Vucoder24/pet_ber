package com.nvv.petber.data.repo.remote

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.PostLike
import com.nvv.petber.data.model.Story
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject


class HomeRepository @Inject constructor(
    supabaseClient: SupabaseClient
) {

    private val db = supabaseClient.postgrest

    // Fetch active stories (not expired), limited to followed users + self
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun fetchStories(): Result<List<Story>> {
        return try {
            val stories = db["stories"]
                .select(
                    columns = Columns.raw(
                        "id, user_id, image_url, created_at, expires_at, users(id, username, avatar_url)"
                    )
                ) {
//                    filter {
//                        gte("expires_at", java.time.Instant.now().toString())
//                    }
                    order("created_at", Order.DESCENDING)
                    limit(10)
                }
                .decodeList<Story>()

            Log.d("HomeRepository", "Fetched $stories stories")
            Result.success( stories)
        } catch (e: Exception) {
            Log.e("HomeRepository", "fetchStories error: ${e.message}")
            Result.failure(e)
        }
    }

    // Fetch posts feed (from all users, ordered by newest)
    suspend fun fetchPosts(
        currentUserId: String,
        page: Int = 0,
        pageSize: Int = 10
    ): Result<List<Post>> {

        return try {

            val from = page * pageSize
            val to = from + pageSize - 1

            val response = db["posts"]
                .select(
                    columns = Columns.raw(
                        """
                    id, user_id, pet_id, caption, location,
                    like_count, comment_count, hashtags, created_at,
                    users(id, username, avatar_url),
                    post_media(id, post_id, media_url, media_type)
                    """.trimIndent()
                    )
                ) {
                    order("created_at", Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<Post>()

            val postIds = response.map { it.id }

            val likedPostIds =
                if (postIds.isNotEmpty())
                    fetchLikedPostIds(currentUserId, postIds)
                else emptySet()

            val posts = response.map { p ->

                p.copy(
                    isLiked = likedPostIds.contains(p.id)
                )

            }

            Result.success(posts)

        } catch (e: Exception) {
            Log.e("HomeRepository", "fetchPosts error: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun fetchLikedPostIds(userId: String, postIds: List<String>): Set<String> {
        return try {
            val response = db["post_likes"]
                .select {
                    filter {
                        eq("user_id", userId)
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<PostLike>()
            response.map { it.postId }.toSet()
        } catch (_: Exception) {
            emptySet()
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
}