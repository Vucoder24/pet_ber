package com.nvv.petber.service

import com.nvv.petber.data.model.Conversation
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChatRealtimeService @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    fun subscribeToNewMessages(conversationId: String): Flow<PostgresAction> = callbackFlow {
        val channel = supabaseClient.channel("messages:$conversationId")

        val subscription = channel.postgresChangeFlow<PostgresAction>(
            schema = "public"
        ) {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }.onEach { action ->
            trySend(action)
        }.launchIn(CoroutineScope(Dispatchers.IO))

        launch {
            channel.subscribe()
        }

        awaitClose {
            launch {
                channel.unsubscribe()
            }
            subscription.cancel()
        }
    }

    fun subscribeToConversations(userId: String): Flow<Conversation> = callbackFlow {

        val channel = supabaseClient.channel("conversations_channel:$userId")

        val subscription = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "conversations"
        }.onEach { action ->

            when (action) {
                is PostgresAction.Insert, is PostgresAction.Update -> {
                    val conversation = action.decodeRecord<Conversation>()
                    if (conversation.user1Id == userId || conversation.user2Id == userId) {
                        trySend(conversation)
                    }
                }
                else -> Unit
            }
        }.launchIn(CoroutineScope(Dispatchers.IO))

        launch {
            channel.subscribe()
        }

        awaitClose {
            subscription.cancel()
            launch {
                channel.unsubscribe()
            }
        }
    }
}