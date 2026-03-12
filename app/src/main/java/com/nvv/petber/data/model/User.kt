package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String,

    val email: String? = null,

    val username: String? = null,

    @SerialName("full_name")
    val fullName: String? = null,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    val bio: String? = null,

    val phone: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)
