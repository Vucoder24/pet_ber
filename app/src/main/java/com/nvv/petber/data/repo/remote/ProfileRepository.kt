package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    suspend fun getUser(userId: String): User? {
        return supabase.from("users")
            .select {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingleOrNull()
    }

    suspend fun getPets(userId: String): List<Pet> {
        return supabase.from("pets")
            .select {
                filter {
                    eq("owner_id", userId)
                }
            }
            .decodeList()
    }

    suspend fun getPosts(userId: String): List<Post> {
        return supabase.from("posts")
            .select(
                Columns.raw("""*,users(*), pets(*), post_media(*)""")
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<Post>()
    }
}