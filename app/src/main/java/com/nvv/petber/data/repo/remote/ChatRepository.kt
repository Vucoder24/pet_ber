package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.dao.ChatDao
import com.nvv.petber.data.model.ConversationEntity
import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.data.model.User
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

    suspend fun refreshConversations(page: Int, limit: Int = 20, currentUserId: String): List<ConversationEntity> = withContext(
        Dispatchers.IO) {
        val list = supabaseClient.postgrest["conversations"]
            .select {
                filter {
                    or {
                        eq("user1_id", currentUserId)
                        eq("user2_id", currentUserId)
                    }
                }
                order("last_message_at", Order.DESCENDING)
                range((page * limit).toLong(), ((page + 1) * limit - 1).toLong())
            }
            .decodeList<ConversationModel>()

        if (list.isNotEmpty()) {
            val enriched = enrichWithUserInfo(list, currentUserId)
            chatDao.upsertConversations(enriched)
            return@withContext enriched
        }else{
            return@withContext emptyList()
        }
    }

    fun syncConversationsRealtime(userId: String): Flow<Unit> =
        realtimeService.subscribeToConversations(userId).onEach {
            refreshConversations(0, 20, userId)
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

    private suspend fun enrichWithUserInfo(
        conversations: List<ConversationModel>,
        currentUserId: String
    ): List<ConversationEntity> {
        val otherUserIds = conversations.map { it.otherUserId(currentUserId) }.distinct()

        val users = supabaseClient.postgrest["users"]
            .select {
                filter { isIn("id", otherUserIds) }
            }
            .decodeList<User>()
            .associateBy { it.id }

        return conversations.map { conv ->
            val otherId = conv.otherUserId(currentUserId)
            val user = users[otherId]
            ConversationEntity(
                conversationId       = conv.conversationId,
                otherUserId          = otherId,
                otherUserName        = user?.username,
                otherUserAvatar      = user?.avatarUrl,
                lastMessageContent   = conv.lastMessageContent,
                lastMessageMediaType = conv.lastMessageMediaType,
                lastMessageAt        = conv.lastMessageAt,
            )
        }
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