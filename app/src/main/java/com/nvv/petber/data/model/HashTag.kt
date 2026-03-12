package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "hashtags")
data class Hashtag(
    @PrimaryKey
    val id: String,

    val name: String,

    @SerialName("created_at")
    val createdAt: String? = null
)