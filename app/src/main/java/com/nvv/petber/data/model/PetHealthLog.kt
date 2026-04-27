package com.nvv.petber.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PetHealthLog(
    @SerialName("id") val id: String = "",
    @SerialName("pet_id") val petId: String = "",
    @SerialName("post_id") val postId: String? = null,
    @SerialName("recorded_at") val recordedAt: String = "",
    @SerialName("weight") val weight: Double? = null,
    @SerialName("body_condition") val bodyCondition: String? = null,
    @SerialName("clinical_status") val clinicalStatus: String? = null,
    @SerialName("activity_and_mental_state") val activityAndMentalState: String? = null,
    @SerialName("preventive_status") val preventiveStatus: String? = null,
    @SerialName("is_neutered") val isNeutered: Boolean? = null
)