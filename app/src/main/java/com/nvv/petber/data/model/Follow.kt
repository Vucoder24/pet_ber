package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "follows")
data class Follow(
    @PrimaryKey
    val id: String,

    @SerialName("follower_id")
    val followerId: String,

    @SerialName("following_id")
    val followingId: String,

    @SerialName("created_at")
    val createdAt: String? = null
)