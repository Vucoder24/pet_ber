package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "post_hashtags")
data class PostHashtag(
    @PrimaryKey
    val id: String,

    @SerialName("post_id")
    val postId: String,

    @SerialName("hashtag_id")
    val hashtagId: String
)