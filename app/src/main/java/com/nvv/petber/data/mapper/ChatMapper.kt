package com.nvv.petber.data.mapper

import com.nvv.petber.data.model.ConversationEntity
import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.data.model.MessageEntity
import com.nvv.petber.data.model.MessageModel

fun ConversationModel.toEntity(currentUserId: String) = ConversationEntity(
    conversationId       = conversationId,
    otherUserId          = if (user1Id == currentUserId) user2Id else user1Id,
    otherUserName        = null,
    otherUserAvatar      = null,
    lastMessageContent   = lastMessageContent,
    lastMessageMediaType = lastMessageMediaType,
    lastMessageAt        = lastMessageAt,
    lastMessageSenderId = lastMessageSenderId
)

fun ConversationEntity.toModel() = ConversationModel(
    conversationId       = conversationId,
    user1Id              = otherUserId,
    user2Id              = otherUserId,
    lastMessageContent   = lastMessageContent,
    lastMessageMediaType = lastMessageMediaType,
    lastMessageAt        = lastMessageAt,
)

fun MessageModel.toEntity() = MessageEntity(
    id, conversationId, senderId, content, mediaUrl, mediaType, isDeleted, createdAt
)

fun MessageEntity.toModel() = MessageModel(
    id, conversationId, senderId, content, mediaUrl, mediaType, isDeleted, createdAt
)