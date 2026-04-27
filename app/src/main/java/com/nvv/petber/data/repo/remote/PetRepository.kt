package com.nvv.petber.data.repo.remote

import android.content.Context
import android.net.Uri
import android.util.Log
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.PetFollowRecord
import com.nvv.petber.data.model.PetFollowWithUser
import com.nvv.petber.data.model.PetHealthLog
import com.nvv.petber.data.model.Post
import com.nvv.petber.utils.TranslationUtils
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.storage.storage
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
                uploadImage(it, "covers")
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

    suspend fun getAllPetDiaryPosts(petId: String): List<Post> {
        return try {
            supabase.from("posts").select(columns = Columns.raw("*, post_media(*)")) {
                filter {
                    contains("pet_id", listOf(UUID.fromString(petId)))
                    filter("deleted_at", FilterOperator.IS, null)
                }
                order("created_at", order = Order.DESCENDING)
            }.decodeList<Post>()
        } catch (e: Exception) {
            Log.e("PetRepository", "Error fetching pet diary posts: ${e.message}")
            emptyList()
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

    suspend fun checkIsFollowingPet(userId: String, petId: String): Boolean {
        return try {
            val result = supabase.from("pet_follows").select {
                filter {
                    eq("user_id", userId)
                    eq("pet_id", petId)
                }
            }.decodeList<PetFollowRecord>()
            result.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun toggleFollowPet(
        userId: String,
        petId: String,
        isCurrentlyFollowing: Boolean
    ): Boolean {
        return try {
            if (isCurrentlyFollowing) {
                supabase.from("pet_follows").delete {
                    filter {
                        eq("user_id", userId)
                        eq("pet_id", petId)
                    }
                }
            } else {
                supabase.from("pet_follows").insert(mapOf("user_id" to userId, "pet_id" to petId))
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getPetFollowers(petId: String): Result<List<com.nvv.petber.data.model.User>> {
        return try {
            val result = supabase.from("pet_follows")
                .select(columns = Columns.raw("*, users(*)")) {
                    filter {
                        eq("pet_id", petId)
                    }
                }
                .decodeList<PetFollowWithUser>()
                .mapNotNull { it.user }

            Result.success(result)
        } catch (e: Exception) {
            Log.e("Error", e.message.toString())
            Result.failure(e)
        }
    }
    suspend fun getPetFollowerCount(petId: String): Result<Long> {
        return try {
            val count = supabase.from("pet_follows")
                .select {
                    filter { eq("pet_id", petId) }
                    count(Count.EXACT)
                }.countOrNull() ?: 0L
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun deletePet(petId: String): Result<Unit> {
        return try {
            supabase.postgrest.rpc(
                function = "delete_pet_cascade",
                parameters = buildJsonObject {
                    put("input_pet_id", petId)
                }
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLatestHealthLogInMonth(
        petId: String,
        yearMonth: String
    ): PetHealthLog? {
        return try {
            val log = supabase.from("pet_health_logs")
                .select {
                    filter {
                        eq("pet_id", petId)
                        gte("recorded_at", "$yearMonth-01")
                        lt("recorded_at", nextMonth(yearMonth))
                    }
                    order("recorded_at", order = Order.DESCENDING)
                    limit(1)
                }
                .decodeSingleOrNull<PetHealthLog>()
            log?.let { translateHealthLog(it) }
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun translateHealthLog(log: PetHealthLog): PetHealthLog {
        val translated = TranslationUtils.translateAll(
            log.bodyCondition,
            log.clinicalStatus,
            log.activityAndMentalState,
            log.preventiveStatus
        )
        return log.copy(
            bodyCondition = translated[0],
            clinicalStatus = translated[1],
            activityAndMentalState = translated[2],
            preventiveStatus = translated[3]
        )
    }

    private fun nextMonth(yearMonth: String): String {
        val (year, month) = yearMonth.split("-").map { it.toInt() }
        return if (month == 12) "${year + 1}-01"
        else "$year-${(month + 1).toString().padStart(2, '0')}"
    }
}