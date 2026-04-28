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
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("other_user_id")
    val otherUserId: String,
    @SerialName("other_user_name")
    val otherUserName: String? = null,
    @SerialName("other_user_avatar")
    val otherUserAvatar: String? = null,
    @SerialName("last_message_content")
    val lastMessageContent: String? = null,
    @SerialName("last_message_media_type")
    val lastMessageMediaType: String? = null,
    @SerialName("last_message_time")
    val lastMessageTime: String? = null,
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val conversationId: String,
    val otherUserId: String,
    val otherUserName: String?,
    val otherUserAvatar: String?,
    val lastMessageContent: String?,
    val lastMessageMediaType: String?,
    val lastMessageTime: String?
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