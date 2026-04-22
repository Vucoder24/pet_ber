package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    @SerialName("user_id")
    val userId: String,

    @SerialName("pet_id")
    val petIds: List<String>? = emptyList(),

    val caption: String? = null,

    val location: String? = null,

    @SerialName("like_count")
    val likeCount: Int = 0,

    @SerialName("comment_count")
    val commentCount: Int = 0,

    @SerialName("share_count")
    val shareCount: Int = 0,

    val hashtags: String? = null,

    val users: User? = null,

    val pets: Pet? = null,

    @SerialName("post_media")
    val postMedia: List<PostMedia>? = null,

    @SerialName("post_likes")
    val postLikes: List<PostLike>? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    var isLiked: Boolean = false,

    var taggedPets: List<Pet> = emptyList(),

    @SerialName("deleted_at")
    val deletedAt: String? = null
)


data class DiaryMonth(
    val monthYear: String,
    val posts: List<Post>
)
