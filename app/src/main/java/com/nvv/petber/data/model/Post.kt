package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("pet_id")
    val petId: String? = null,

    val caption: String? = null,

    val location: String? = null,

    @SerialName("like_count")
    val likeCount: Int = 0,

    @SerialName("comment_count")
    val commentCount: Int = 0,

    val hashtags: String? = null,

    val users: User? = null,

    val pets: Pet? = null,

    @SerialName("post_media")
    val postMedia: List<PostMedia>? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    var isLiked: Boolean = false
)
