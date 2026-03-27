package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "post_media")
data class PostMedia(

    @PrimaryKey
    val id: String,

    @SerialName("post_id")
    val postId: String,

    @SerialName("media_url")
    val mediaUrl: String,

    @SerialName("media_type")
    val mediaType: String,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
enum class MediaType {
    IMAGE,
    VIDEO
}