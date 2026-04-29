package com.nvv.petber.data.repo.remote

import android.util.Log
import com.nvv.petber.R
import com.nvv.petber.data.dao.ChatDao
import com.nvv.petber.data.model.Conversation
import com.nvv.petber.data.model.User
import com.nvv.petber.service.ChatRealtimeService
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val realtimeService: ChatRealtimeService,
    @ApplicationContext private val context: android.content.Context
) {

    fun observeLocalConversations() = chatDao.observeConversations()

    suspend fun refreshConversations(page: Int, limit: Int = 20, currentUserId: String): List<Conversation> = withContext(
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
            .decodeList<Conversation>()

        if (list.isEmpty()) return@withContext emptyList()

        val otherUserIds = list.mapNotNull { conv ->
            when {
                conv.user1Id == currentUserId -> conv.user2Id
                conv.user2Id == currentUserId -> conv.user1Id
                else -> null
            }
        }.distinct()

        if (otherUserIds.isEmpty()) return@withContext emptyList()

        val users = supabaseClient.postgrest["users"]
            .select {
                filter { isIn("id", otherUserIds) }
            }
            .decodeList<User>()
            .associateBy { it.id }

        val enriched = list.mapNotNull { conv ->
            val otherId = when {
                conv.user1Id == currentUserId -> conv.user2Id
                conv.user2Id == currentUserId -> conv.user1Id
                else -> return@mapNotNull null
            }

            val user = users[otherId]

            conv.copy(
                otherUserId = otherId,
                otherUserName = user?.fullName ?: context.getString(R.string.petber_user),
                otherUserAvatar = user?.avatarUrl
            )
        }

        chatDao.upsertConversations(enriched)
        enriched
    }

    fun syncConversationsRealtime(userId: String): Flow<Unit> =
        realtimeService.subscribeToConversations(userId).onEach {
            Log.d("ChatRepository", "Realtime update for conversation ${it.conversationId}")
            refreshConversations(0, 20, userId)
        }.map { }

    suspend fun markAsReadLocal(conversationId: String){
        try {
            chatDao.markAsRead(conversationId)
        }catch (_: Exception){}
    }

    suspend fun searchConversations(query: String): List<Conversation> = withContext(Dispatchers.IO) {
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
            .decodeList<Conversation>()
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