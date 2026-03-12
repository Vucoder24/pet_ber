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

    val content: String,

    @SerialName("created_at")
    val createdAt: String? = null
)