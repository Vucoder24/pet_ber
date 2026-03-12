package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey
    val id: String,

    @SerialName("owner_id")
    val ownerId: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    val name: String,

    val breed: String? = null,

    val species: String? = null,

    val age: Int? = null,

    val location: String? = null,

    val description: String? = null,

    @SerialName("health_status")
    val healthStatus: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
)