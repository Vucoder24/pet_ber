package com.nvv.petber.data.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    @SerialName("owner_id")
    val ownerId: String,

    @SerialName("avatar_url")
    val avatarUrl: String? = null,

    @SerialName("cover_url")
    val coverUrl: String? = null,

    val name: String,

    val breed: String? = null,

    val species: String? = null,

    val gender: String? = null,

    val weight: Double? = null,

    @SerialName("is_neutered")
    val isNeutered: Boolean? = false,

    val birthday: String? = null,

    val description: String? = null,

    @SerialName("body_condition")
    val bodyCondition: String? = null,

    @SerialName("clinical_status")
    val clinicalStatus: String? = null,

    @SerialName("activity_and_mental_state")
    val activityAndMentalState: String? = null,

    @SerialName("medical_history_and_treatment")
    val medicalHistoryAndTreatment: String? = null,

    @SerialName("preventive_status")
    val preventiveStatus: String? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("updated_at")
    val updatedAt: String? = null
): Parcelable

@Serializable
data class PetSearchResult(
    val pet: Pet,
    val isFollowing: Boolean
)

@Serializable
data class PetFollowRecord(
    @SerialName("id")
    val id: String,

    @SerialName("user_id")
    val userId: String,

    @SerialName("pet_id")
    val petId: String,

    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class PetFollowWithUser(
    @SerialName("users")
    val user: User? = null
)