package com.nvv.petber.data.converter

import androidx.room.TypeConverter
import com.nvv.petber.data.model.CommentLike
import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.PostLike
import com.nvv.petber.data.model.PostMedia
import com.nvv.petber.data.model.User
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DataConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromUser(user: User?): String? = user?.let { json.encodeToString(it) }

    @TypeConverter
    fun toUser(value: String?): User? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromPet(pet: Pet?): String? = pet?.let { json.encodeToString(it) }

    @TypeConverter
    fun toPet(value: String?): Pet? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromPostMediaList(list: List<PostMedia>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toPostMediaList(value: String?): List<PostMedia>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromPostLikeList(list: List<PostLike>?): String? = list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toPostLikeList(value: String?): List<PostLike>? = value?.let { json.decodeFromString(it) }

    @TypeConverter
    fun fromCommentLikeList(value: List<CommentLike>?): String? {
        return value?.let { Json.encodeToString(it) }
    }

    @TypeConverter
    fun toCommentLikeList(value: String?): List<CommentLike>? {
        return value?.let { Json.decodeFromString(it) }
    }

    @TypeConverter
    fun fromPetList(list: List<Pet>?): String? =
        list?.let { json.encodeToString(it) }

    @TypeConverter
    fun toPetList(value: String?): List<Pet>? =
        value?.let { json.decodeFromString(it) }
}