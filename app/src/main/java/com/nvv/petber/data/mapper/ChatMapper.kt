package com.nvv.petber.data.mapper

import com.nvv.petber.data.model.ConversationEntity
import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.data.model.MessageEntity
import com.nvv.petber.data.model.MessageModel

fun ConversationModel.toEntity() = ConversationEntity(
    conversationId, otherUserId, otherUserName, otherUserAvatar,
    lastMessageContent, lastMessageMediaType, lastMessageTime
)

fun ConversationEntity.toModel() = ConversationModel(
    conversationId = conversationId,
    otherUserId = otherUserId,
    otherUserName = otherUserName,
    otherUserAvatar = otherUserAvatar,
    lastMessageContent = lastMessageContent,
    lastMessageMediaType = lastMessageMediaType,
    lastMessageTime = lastMessageTime
)

fun MessageModel.toEntity() = MessageEntity(
    id, conversationId, senderId, content, mediaUrl, mediaType, isDeleted, createdAt
)

fun MessageEntity.toModel() = MessageModel(
    id, conversationId, senderId, content, mediaUrl, mediaType, isDeleted, createdAt
)