package com.nvv.petber.service

import com.nvv.petber.data.model.ConversationModel
import com.nvv.petber.data.model.MessageModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
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
    fun subscribeToNewMessages(conversationId: String): Flow<MessageModel> = callbackFlow {
        val channel = supabaseClient.channel("messages:$conversationId")

        val subscription = channel.postgresChangeFlow<PostgresAction.Insert>(
            schema = "public"
        ) {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }.onEach { action ->
            val newMessage = action.decodeRecord<MessageModel>()
            trySend(newMessage)
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

    fun subscribeToConversations(userId: String): Flow<ConversationModel> = callbackFlow {
        val ch1 = supabaseClient.channel("conv_user1:$userId")
        val ch2 = supabaseClient.channel("conv_user2:$userId")

        fun listenChannel(channel: RealtimeChannel, field: String) =
            channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "conversations"
                filter(field, FilterOperator.EQ, userId)
            }.onEach { action ->
                when (action) {
                    is PostgresAction.Insert, is PostgresAction.Update ->
                        trySend(action.decodeRecord<ConversationModel>())
                    else -> Unit
                }
            }.launchIn(CoroutineScope(Dispatchers.IO))

        val sub1 = listenChannel(ch1, "user1_id")
        val sub2 = listenChannel(ch2, "user2_id")

        launch {
            ch1.subscribe()
            ch2.subscribe()
        }

        awaitClose {
            sub1.cancel()
            sub2.cancel()
            launch {
                ch1.unsubscribe()
                ch2.unsubscribe()
            }
        }
    }
}