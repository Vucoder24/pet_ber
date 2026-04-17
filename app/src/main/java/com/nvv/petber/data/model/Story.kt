package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "stories")
data class Story(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),

    @SerialName("user_id")
    val userId: String,

    @SerialName("media_url")
    val mediaUrl: String,

    @SerialName("media_type")
    val mediaType: String,

    val users: User? = null,

    @SerialName("created_at")
    val createdAt: String? = null,

    @SerialName("expires_at")
    val expiresAt: String? = null,

    @SerialName("is_expired")
    val isExpired: Boolean = false
)



@Serializable
data class UserStoryGroup(
    val userId: String,
    val user: User?,
    val stories: List<Story>
)

@Serializable
data class StoryReactionCount(
    @SerialName("story_id") val storyId: String,
    @SerialName("reaction_type") val reactionType: String,
    @SerialName("total_count") val totalCount: Int
)

@Serializable
data class UserRecentReaction(
    @SerialName("reaction_type") val reactionType: String
)

data class ReactionSummary(
    val counts: Map<String, Int> = mapOf("paw" to 0, "cat" to 0, "fish" to 0, "yarn" to 0),
    val myRecentReactions: List<String> = emptyList()
)
