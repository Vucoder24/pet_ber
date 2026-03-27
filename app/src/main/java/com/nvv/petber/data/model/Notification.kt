package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("sender_id")
    val senderId: String? = null,

    val type: String? = null,

    @SerialName("post_id")
    val postId: String? = null,

    @SerialName("comment_id")
    val commentId: String? = null,

    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,

    @SerialName("pet_id")
    val petId: String? = null,

    @SerialName("is_read")
    val isRead: Boolean = false,

    val message: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null
)