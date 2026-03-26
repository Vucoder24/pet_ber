package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.Pet
import com.nvv.petber.data.model.Post
import com.nvv.petber.data.model.User
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Count
import javax.inject.Inject

class ProfileRepositoryRemote @Inject constructor(
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
                Columns.raw(
                    """*, users(*), pets(*), post_media(*), 
                    |post_likes(*).filter(user_id.eq.$userId)""".trimMargin()
                )
            ) {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<Post>().map {
                it.apply { isLiked = !postLikes.isNullOrEmpty() }
            }
    }

    suspend fun getUserStats(userId: String): UserStats {
        return try {
            val postsResponse = supabase.from("posts")
                .select {
                    filter { eq("user_id", userId) }
                    count(Count.EXACT)
                }

            val followersResponse = supabase.from("follows")
                .select {
                    filter { eq("following_id", userId) }
                    count(Count.EXACT)
                }

            val followingResponse = supabase.from("follows")
                .select {
                    filter { eq("follower_id", userId) }
                    count(Count.EXACT)
                }

            UserStats(
                postCount = postsResponse.countOrNull() ?: 0,
                followerCount = followersResponse.countOrNull() ?: 0,
                followingCount = followingResponse.countOrNull() ?: 0
            )
        } catch (_: Exception) {
            UserStats()
        }
    }

    suspend fun logout(){
        return supabase.auth.signOut()
    }
}

data class UserStats(
    val postCount: Long = 0,
    val followerCount: Long = 0,
    val followingCount: Long = 0
)