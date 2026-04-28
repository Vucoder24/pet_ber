package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.dao.ChatDao
import com.nvv.petber.data.mapper.toEntity
import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.service.ChatRealtimeService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val chatDao: ChatDao,
    private val realtimeService: ChatRealtimeService
) {

    fun observeLocalConversations() = chatDao.observeConversations()

    suspend fun refreshConversations(page: Int, limit: Int = 20): List<ConversationModel> = withContext(
        Dispatchers.IO) {
        val remoteData = supabaseClient.postgrest["conversation_list_view"]
            .select {
                order("last_message_time", Order.DESCENDING)
                range((page * limit).toLong(), ((page + 1) * limit - 1).toLong())
            }
            .decodeList<ConversationModel>()

        if (remoteData.isNotEmpty()) {
            chatDao.upsertConversations(remoteData.map { it.toEntity() })
        }

        return@withContext remoteData
    }

    fun syncConversationsRealtime(userId: String): Flow<Unit> =
        realtimeService.subscribeToConversations(userId).onEach {
            refreshConversations(0, 20)
        }.map { }


    suspend fun searchConversations(query: String): List<ConversationModel> = withContext(Dispatchers.IO) {
        return@withContext supabaseClient.postgrest["conversation_list_view"]
            .select {
                filter {
                    or {
                        ilike("other_user_name", "%$query%")
                        ilike("last_message_content", "%$query%")
                    }
                }
                order("last_message_time", Order.DESCENDING)
            }
            .decodeList<ConversationModel>()
    }


    suspend fun deleteConversationRpc(conversationId: String) = withContext(Dispatchers.IO) {
        supabaseClient.postgrest.rpc(
            function = "delete_conversation",
            parameters = mapOf("p_conversation_id" to conversationId)
        )
        chatDao.deleteConversation(conversationId)
        chatDao.deleteMessagesByConversation(conversationId)
    }
}