package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageModel(
    @SerialName("id") val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("content") val content: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("is_deleted") val isDeleted: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ConversationModel(
    @SerialName("id")
    val conversationId: String,
    @SerialName("user1_id")
    val user1Id: String,
    @SerialName("user2_id")
    val user2Id: String,
    @SerialName("last_message_content")
    val lastMessageContent: String? = null,
    @SerialName("last_message_media_type")
    val lastMessageMediaType: String? = null,
    @SerialName("last_message_at")
    val lastMessageAt: String? = null,
    @SerialName("last_message_sender_id")
    val lastMessageSenderId: String? = null,
) {
    fun otherUserId(currentUserId: String) =
        if (user1Id == currentUserId) user2Id else user1Id
}

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val otherUserId: String,
    val otherUserName: String?,
    val otherUserAvatar: String?,
    val lastMessageContent: String?,
    val lastMessageMediaType: String?,
    val lastMessageAt: String?,
    val lastMessageSenderId: String?
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String?,
    val mediaUrl: String?,
    val mediaType: String?,
    val isDeleted: Boolean = false,
    val createdAt: String
)