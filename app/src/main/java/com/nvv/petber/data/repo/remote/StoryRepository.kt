package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.StoryReactionCount
import com.nvv.petber.data.model.UserRecentReaction
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class StoryRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    suspend fun getReactionCounts(storyId: String): List<StoryReactionCount> {
        return supabase.postgrest["view_story_reaction_counts"]
            .select { filter { eq("story_id", storyId) } }
            .decodeList<StoryReactionCount>()
    }

    suspend fun getMyRecentReactions(storyId: String, userId: String): List<String> {
        return supabase.postgrest["story_reactions"]
            .select(columns = Columns.list("reaction_type")) {
                filter {
                    eq("story_id", storyId)
                    eq("user_id", userId)
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<UserRecentReaction>()
            .map { it.reactionType }
    }

    suspend fun sendReaction(storyId: String, userId: String, type: String) {
        supabase.postgrest.rpc(
            function = "handle_story_reaction",
            parameters = buildJsonObject {
                put("p_story_id", storyId)
                put("p_user_id", userId)
                put("p_reaction_type", type)
            }
        )
    }
}