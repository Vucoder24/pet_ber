package com.nvv.petber.data.mapper

import com.nvv.petber.data.model.MessageEntity
import com.nvv.petber.data.model.MessageModel

fun MessageModel.toEntity() = MessageEntity(
    id, conversationId, senderId, content, mediaUrl, mediaType, isDeleted, createdAt
)

fun MessageEntity.toModel() = MessageModel(
    id, conversationId, senderId, content, mediaUrl, mediaType, isDeleted, createdAt
)