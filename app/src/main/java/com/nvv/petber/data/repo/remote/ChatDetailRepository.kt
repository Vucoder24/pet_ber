package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.dao.ChatDao
import com.nvv.petber.data.mapper.toEntity
import com.nvv.petber.data.model.MessageModel
import com.nvv.petber.service.ChatRealtimeService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatDetailRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val realtimeService: ChatRealtimeService,
    private val chatDao: ChatDao,
) {

    fun observeLocalMessages(conversationId: String) = chatDao.observeMessages(conversationId)

    suspend fun fetchAndSaveMessages(conversationId: String, page: Int): Boolean {
        val remoteMessages = fetchMessages(conversationId, page)
        if (remoteMessages.isNotEmpty()) {
            chatDao.upsertMessages(remoteMessages.map { it.toEntity() })
            return true
        }
        return false
    }

    suspend fun fetchMessages(
        conversationId: String,
        page: Int,
        limit: Int = 20
    ): List<MessageModel> {
        return supabaseClient.postgrest["messages"].select(
            columns = Columns.raw(
                "*"
            )
        ) {
            filter { eq("conversation_id", conversationId) }
            order("created_at", Order.DESCENDING)
            range((page * limit).toLong(), ((page + 1) * limit - 1).toLong())
        }
            .decodeList<MessageModel>()
    }

    suspend fun sendMessage(message: MessageModel) {
        supabaseClient.postgrest["messages"].insert(message)
    }

    fun observeNewMessages(conversationId: String): Flow<PostgresAction> {
        return realtimeService.subscribeToNewMessages(conversationId)
    }

    suspend fun syncMessagesWithServer(conversationId: String): Boolean {
        val remoteMessages = fetchMessages(conversationId, 0, 50)

        if (remoteMessages.isNotEmpty()) {
            val remoteIds = remoteMessages.map { it.id }

            chatDao.upsertMessages(remoteMessages.map { it.toEntity() })

            chatDao.deleteRemovedMessages(conversationId, remoteIds)
            return true
        }
        return false
    }

    suspend fun deleteMessagePermanently(message: MessageModel) {
        if (!message.mediaUrl.isNullOrEmpty()) {
                val oldUrl = message.mediaUrl
                val bucket = "chat_media"
            val pathIdentifier = "/object/public/$bucket/"
            if (oldUrl.contains(pathIdentifier)) {
                val filePath = oldUrl.substringAfter(pathIdentifier)
                supabaseClient.storage[bucket].delete(filePath)
            }
        }
        supabaseClient.postgrest["messages"].delete {
            filter { eq("id", message.id) }
        }
    }

    suspend fun uploadMedia(byteArray: ByteArray, fileName: String): String {
        val bucket = supabaseClient.storage["chat_media"]
        bucket.upload(fileName, byteArray)
        return bucket.publicUrl(fileName)
    }


    suspend fun syncNewMessage(message: MessageModel) {
        chatDao.upsertMessages(listOf(message.toEntity()))
    }

    suspend fun deleteMessageLocally(messageId: String) {
        chatDao.deleteMessage(messageId)
    }

    suspend fun saveMessageToLocal(message: MessageModel) {
        chatDao.upsertMessages(listOf(message.toEntity()))
    }

}