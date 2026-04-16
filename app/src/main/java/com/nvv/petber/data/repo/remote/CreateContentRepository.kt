package com.nvv.petber.data.repo.remote

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.PostMedia
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
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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

        hashtags?.split(" ")?.filter { it.startsWith("#") }?.forEach { tag ->
            val hJson = buildJsonObject {
                put("name", tag.lowercase())
            }
            db["hashtags"].upsert(hJson) {
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

    fun createStory(mediaUri: Uri) = flow {
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

    fun updatePost(
        postId: String,
        caption: String,
        hashtags: String?,
        newMediaUris: List<Uri>,
        deletedMediaIds: List<String>,
        petIds: List<String>
    ) = flow {
        val userId = supabaseClient.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("User is not logged in")

        val updateJson = buildJsonObject {
            put("caption", caption)
            put("hashtags", hashtags)
            putJsonArray("pet_id") {
                petIds.forEach { id ->
                    add(id)
                }
            }
        }

        db["posts"].update(updateJson) {
            filter { eq("id", postId) }
        }

        // 2. Xử lý Hashtags (Tương tự Create)
        hashtags?.split(" ")?.filter { it.startsWith("#") }?.forEach { tag ->
            val hJson = buildJsonObject {
                put("name", tag.lowercase())
            }
            db["hashtags"].upsert(hJson) {
                onConflict = "name"
                ignoreDuplicates = true
            }
        }

        emit(30) // Tiến trình tượng trưng

        // 3. Xóa các Media bị loại bỏ
        if (deletedMediaIds.isNotEmpty()) {
            val mediaToDelete = db["post_media"].select {
                filter { isIn("id", deletedMediaIds) }
            }.decodeList<PostMedia>()

            db["post_media"].delete {
                filter { isIn("id", deletedMediaIds) }
            }

            val fileNames = mediaToDelete.map {
                it.mediaUrl.substringAfterLast("/").substringBefore("?")
            }.map { "$userId/$it" }

            if (fileNames.isNotEmpty()) {
                supabaseClient.storage["posts"].delete(fileNames)
            }
        }

        emit(50)

        // 4. Upload Media mới (nếu có)
        if (newMediaUris.isEmpty()) {
            emit(100)
            return@flow
        }

        val total = newMediaUris.size
        var uploaded = 0

        newMediaUris.forEach { uri ->
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

            val progress = 50 + ((uploaded * 50) / total)
            emit(progress)
        }
    }.flowOn(Dispatchers.IO)
}