package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "pet_images")
data class PetImage(
    @PrimaryKey
    val id: String,

    @SerialName("pet_id")
    val petId: String,

    @SerialName("image_url")
    val imageUrl: String,

    @SerialName("created_at")
    val createdAt: String? = null
)