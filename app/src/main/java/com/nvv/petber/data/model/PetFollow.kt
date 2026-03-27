package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "pet_follows")
data class PetFollow(
    @PrimaryKey
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("pet_id")
    val petId: String,

    @SerialName("created_at")
    val createdAt: String? = null
)