package com.nvv.petber.data.repo.remote

import android.content.Context
import android.net.Uri
import com.nvv.petber.data.model.Pet
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class PetRepository @Inject constructor(
    private val supabase: SupabaseClient,
    context: Context,
    private val profileRepositoryRemote: ProfileRepositoryRemote
) {
    private val appContext = context

    suspend fun createPet(
        pet: Pet,
        avatarUri: Uri?,
        coverUri: Uri?
    ): Result<Unit> {
        return try {

            val avatarUrl = avatarUri?.let {
                uploadImage(it, "avatars")
            }

            val coverUrl = coverUri?.let {
                uploadImage( it, "covers")
            }

            val data = buildJsonObject {
                put("owner_id", pet.ownerId)
                put("name", pet.name)
                put("breed", pet.breed)
                put("species", pet.species)
                put("gender", pet.gender)
                put("weight", pet.weight)
                put("is_neutered", pet.isNeutered)
                put("birthday", pet.birthday)
                put("description", pet.description)
                put("body_condition", pet.bodyCondition)
                put("clinical_status", pet.clinicalStatus)
                put("activity_and_mental_state", pet.activityAndMentalState)
                put("preventive_status", pet.preventiveStatus)
                put("avatar_url", avatarUrl)
                put("cover_url", coverUrl)
            }

            supabase.from("pets").insert(data)

            Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun uploadImage(
        uri: Uri,
        folder: String
    ): String {

        val bytes = appContext.contentResolver.openInputStream(uri)?.readBytes()
            ?: throw Exception("Cannot read image")

        val fileName = "${UUID.randomUUID()}.jpg"

        supabase.storage.from("pet-media")
            .upload("$folder/$fileName", bytes)

        return supabase.storage.from("pet-media")
            .publicUrl("$folder/$fileName")
    }

    suspend fun updatePetAvatar(petId: String, uri: Uri, oldAvatarUrl: String?): Pet {
        val avatarUrl = profileRepositoryRemote.uploadMedia(uri, petId, "pet-media")

        val petData = supabase.from("pets").update(
            { set("avatar_url", avatarUrl) }
        ) {
            filter { eq("id", petId) }
            select()
        }.decodeSingle<Pet>()

        profileRepositoryRemote.deleteOldMedia(oldAvatarUrl, "pet-media")
        return petData
    }

    suspend fun updatePetCover(petId: String, uri: Uri, oldCoverUrl: String?): Pet {
        val coverUrl = profileRepositoryRemote.uploadMedia(uri, petId, "pet-media")

        val petData = supabase.from("pets").update(
            { set("cover_url", coverUrl) }
        ) {
            filter { eq("id", petId) }
            select()
        }.decodeSingle<Pet>()

        profileRepositoryRemote.deleteOldMedia(oldCoverUrl, "pet-media")
        return petData
    }

    suspend fun updatePet(pet: Pet): Result<Unit> {
        return try {
            val data = buildJsonObject {
                put("name", pet.name)
                put("breed", pet.breed)
                put("species", pet.species)
                put("gender", pet.gender)
                put("weight", pet.weight)
                put("is_neutered", pet.isNeutered)
                put("birthday", pet.birthday)
                put("description", pet.description)
                put("body_condition", pet.bodyCondition)
                put("clinical_status", pet.clinicalStatus)
                put("activity_and_mental_state", pet.activityAndMentalState)
                put("preventive_status", pet.preventiveStatus)
            }

            supabase.from("pets").update(data) {
                filter { eq("id", pet.id) }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}