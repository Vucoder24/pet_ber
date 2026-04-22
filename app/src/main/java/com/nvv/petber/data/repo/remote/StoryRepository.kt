package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.Story
import com.nvv.petber.data.model.StoryReactionDetail
import com.nvv.petber.data.model.UserStoryGroup
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

    suspend fun getStoryGroupByStoryId(storyId: String): StoryGroupResult {
        val targetStory = supabase.postgrest["stories"]
            .select(Columns.raw("*, users(*)")) {
                filter { eq("id", storyId) }
            }
            .decodeSingleOrNull<Story>()

        if (targetStory == null || targetStory.isExpired) {
            throw Exception("STORY_UNAVAILABLE")
        }

        val targetUserId = targetStory.userId

        val userActiveStories = supabase.postgrest["stories"]
            .select(Columns.raw("*, users(id, username, full_name, avatar_url)")) {
                filter {
                    eq("user_id", targetUserId)
                    eq("is_expired", false)
                }
            }
            .decodeList<Story>()

        val group = UserStoryGroup(
            userId = targetUserId,
            user = targetStory.users,
            stories = userActiveStories.sortedBy { it.createdAt }
        )

        val initialIndex = group.stories.indexOfFirst { it.id == storyId }

        return StoryGroupResult(
            initialIndex = if (initialIndex != -1) initialIndex else 0,
            groups = listOf(group)
        )
    }

    suspend fun getStoryReactionDetails(storyId: String): List<StoryReactionDetail> {
        return try {
            val result = supabase.postgrest["story_reactions"]
                .select(Columns.raw("*, users(id, username, full_name, avatar_url)")) {
                    filter { eq("story_id", storyId) }
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<StoryReactionDetail>()
            return result
        }catch (_: Exception){
             emptyList()
        }
    }
}

data class StoryGroupResult(
    val initialIndex: Int,
    val groups: List<UserStoryGroup>
)