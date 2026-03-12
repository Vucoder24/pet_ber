package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "stories")
data class Story(
    @PrimaryKey
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("image_url")
    val imageUrl: String,
    val users: User? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null
)
