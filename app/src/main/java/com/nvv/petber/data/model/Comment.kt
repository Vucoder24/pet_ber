package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey
    val id: String,

    @SerialName("post_id")
    val postId: String,

    @SerialName("user_id")
    val userId: String,

    val users: User? = null,

    val content: String,

    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,

    @SerialName("media_url")
    val mediaUrl: String? = null,

    @SerialName("like_count")
    val likeCount: Int = 0,

    @SerialName("is_edited")
    val isEdited: Boolean = false,

    @SerialName("is_deleted")
    val isDeleted: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    @SerialName("comment_likes")
    val commentLikes: List<CommentLike>? = null,

    var isLiked: Boolean = false
)

@Serializable
data class CommentLike(
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String
)

data class CommentUI(
    val comment: Comment,
    val depth: Int,
    val hasReplies: Boolean,
    val isExpanded: Boolean,
    val replyCount: Int
)

class CommentNode(
    val comment: Comment,
    var isExpanded: Boolean = false,
    val replies: MutableList<CommentNode> = mutableListOf()
)