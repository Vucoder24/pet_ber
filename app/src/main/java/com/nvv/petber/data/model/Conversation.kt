package com.nvv.petber.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "conversations")
@Serializable
data class Conversation(
    @PrimaryKey
    @SerialName("id")
    val conversationId: String,

    @SerialName("user1_id")
    val user1Id: String = "",

    @SerialName("user2_id")
    val user2Id: String = "",

    val otherUserId: String = "",
    val otherUserName: String? = null,
    val otherUserAvatar: String? = null,

    @SerialName("last_message_content")
    val lastMessageContent: String? = null,

    @SerialName("last_message_media_type")
    val lastMessageMediaType: String? = null,

    @SerialName("last_message_at")
    val lastMessageAt: String? = null,

    @SerialName("last_message_sender_id")
    val lastMessageSenderId: String? = null,

    val isSeen: Boolean = false
)