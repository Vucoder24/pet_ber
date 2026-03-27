package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class SearchRepository @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val db = supabaseClient.postgrest

    suspend fun searchUsers(query: String): List<User> {
        val rawQuery = "%$query%"
        return try {
            db["users"].select {
                if (query.isNotEmpty()) filter {
                    or {
                        ilike("username", rawQuery)
                        ilike("full_name", rawQuery)
                    }
                }
                limit(15)
            }.decodeList<User>()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun searchPets(query: String): List<Pet> {
        val rawQuery = "%$query%"
        return try {
            db["pets"].select {
                if (query.isNotEmpty()) filter { ilike("name", rawQuery) }
                limit(15)
            }.decodeList<Pet>()
        } catch (e: Exception) { emptyList() }
    }

    suspend fun searchPostsByHashtag(query: String, currentUserId: String): List<Post> {
        val rawQuery = "%$query%"
        return try {
            db["posts"].select(
                Columns.raw("*, users(*), post_media(*), " +
                    "post_likes(*).filter(user_id.eq.$currentUserId)"
                )) {
                if (query.isNotEmpty()) filter { ilike("hashtags", rawQuery) }
                limit(15)
            }.decodeList<Post>().map {
                it.apply { isLiked = !postLikes.isNullOrEmpty() }
            }
        } catch (e: Exception) { emptyList() }
    }
}