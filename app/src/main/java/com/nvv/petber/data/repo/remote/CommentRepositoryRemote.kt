package com.nvv.petber.data.repo.remote

import android.util.Log
import com.nvv.petber.data.model.Comment
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject


@Serializable
data class CommentInsertRequest(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null
)

class CommentRepositoryRemote @Inject constructor(
    supabaseClient: SupabaseClient
) {
    private val db = supabaseClient.postgrest

    suspend fun fetchComments(postId: String, currentUserId: String): Result<List<Comment>> {
        return try {
            val comments = db["comments"]
                .select(
                    columns = Columns.raw(
                        "*, users(id, username, full_name, avatar_url), comment_likes(*)"
                    )
                ) {
                    filter {
                        eq("post_id", postId)
                        eq("is_deleted", false)
                    }
                    order("created_at", Order.ASCENDING)
                }
                .decodeList<Comment>()
                .map { comment ->
                    comment.apply {
                        isLiked = commentLikes?.any { it.userId == currentUserId } == true
                    }
                }

            Result.success(comments)
        } catch (e: Exception) {
            Log.e("Repository", "fetchComments error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun toggleLikeCmt(commentId: String, userId: String): Result<Boolean> {
        return try {
            val isLiked = db.rpc(
                "toggle_comment_like",
                mapOf("p_comment_id" to commentId, "p_user_id" to userId)
            ).decodeAs<Boolean>()
            Result.success(isLiked)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // add cmt
    suspend fun addComment(
        postId: String,
        userId: String,
        content: String,
        parentCommentId: String? = null
    ): Result<Boolean> {
        return try {
            val newComment = CommentInsertRequest(
                postId = postId,
                userId = userId,
                content = content,
                parentCommentId = parentCommentId
            )
            db["comments"].insert(newComment)

            Result.success(true)
        } catch (e: Exception) {
            Log.e("Repository", "addComment error: ${e.message}")
            Result.failure(e)
        }
    }
}