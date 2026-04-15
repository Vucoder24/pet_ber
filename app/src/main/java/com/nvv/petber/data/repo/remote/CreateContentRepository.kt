package com.nvv.petber.data.repo.remote

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.Story
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

class CreateContentRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val context: Context
) {
    private val db = supabaseClient.postgrest

    fun createPost(
        caption: String,
        hashtags: String?,
        location: String?,
        mediaUris: List<Uri>,
        petIds: List<String>
    ) = flow {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User is not logged in")


        val post = Post(
            userId = userId,
            petIds = petIds,
            caption = caption,
            location = location,
            hashtags = hashtags,
            likeCount = 0,
            commentCount = 0
        )

        // 2. Insert post
        val insertedPost = db["posts"]
            .insert(post) {
                select()
            }
            .decodeSingle<Post>()

        // Insert hashtags table
        hashtags?.forEach { tag ->

            val hJson = buildJsonObject {
                put("name", tag.lowercase())
            }

            db["hashtags"]
                .upsert(hJson) {
                    onConflict = "name"
                    ignoreDuplicates = true
                }
        }

        if (mediaUris.isEmpty()) {
            emit(100)
            return@flow
        }

        val postId = insertedPost.id

        val total = mediaUris.size
        var uploaded = 0

        // 3. Upload media + insert post_media
        mediaUris.forEach { uri ->

            val mediaUrl = uploadMedia(uri, userId, bucket = "posts")

            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

            val mediaType = if (mimeType.startsWith("video")) "video" else "image"

            val mediaJson = buildJsonObject {
                put("post_id", postId)
                put("media_url", mediaUrl)
                put("media_type", mediaType)
            }

            db["post_media"].insert(mediaJson)
            uploaded++

            val progress = (uploaded * 100) / total
            emit(progress)
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun uploadMedia(
        uri: Uri,
        userId: String,
        bucket: String
    ): String {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mimeType) ?: "jpg"

        val fileName = "$userId/${UUID.randomUUID()}.$extension"

        supabaseClient.storage[bucket].upload(
            path = fileName,
            uri = uri
        ) {
            contentType = ContentType.parse(mimeType)
            upsert = false
        }

        return supabaseClient.storage[bucket].publicUrl(fileName)
    }

    fun createStory(mediaUri: Uri) = flow<Int> {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User is not logged in")

        emit(10)
        var mediaUrl: String? = null
        emit(20)

        try {

            // upload
            emit(30)
            emit(40)
            emit(50)
            emit(60)
            emit(70)
             mediaUrl = uploadMedia(mediaUri, userId, "stories")

            emit(80)

            val mimeType = context.contentResolver.getType(mediaUri) ?: "image/jpeg"
            val mediaType = if (mimeType.startsWith("video")) "video" else "image"

            val expiresAt = Instant.now()
                .plusSeconds(86400)
                .toString()

            val story = Story(
                userId = userId,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                expiresAt = expiresAt
            )
            emit(90)

            db["stories"].insert(story)

            emit(100)

        } catch (e: Exception) {

            // rollback storage
            mediaUrl?.let {
                supabaseClient.storage["stories"].delete(listOf(it))
            }

            throw e
        }
    }.flowOn(Dispatchers.IO)

    suspend fun getUserPets(): List<Pet> {
        val userId = supabaseClient.auth.currentUserOrNull()?.id ?: return emptyList()

        return db["pets"]
            .select {
                filter { eq("owner_id", userId) }
            }
            .decodeList<Pet>()
    }
}