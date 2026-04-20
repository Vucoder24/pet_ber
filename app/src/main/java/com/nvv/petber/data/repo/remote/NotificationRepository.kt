package com.nvv.petber.data.repo.remote

import com.nvv.petber.data.model.Notification
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.decodeRecord
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private var channel: RealtimeChannel? = null

    suspend fun setupChannel(userId: String) {
        if (channel != null) return
        channel = supabase.channel("notifications-$userId")
        supabase.realtime.connect()
    }

    fun listenToNewNotifications(userId: String): Flow<Notification> {
        val flow = channel!!
            .postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                filter("user_id", FilterOperator.EQ, userId)
            }
            .map {
                it.decodeRecord<Notification>()
            }

        return flow.onStart {
            channel?.subscribe()
        }
    }

    suspend fun getNotifications(userId: String, page: Int, limit: Int = 15): List<Notification> {
        val from = (page - 1) * limit
        val to = from + limit - 1

        return supabase.from("notifications")
            .select {
                filter { eq("user_id", userId) }
                order("created_at", order = Order.DESCENDING)
                range(from.toLong(), to.toLong())
            }.decodeList<Notification>()
    }

    suspend fun getNewNotificationCount(userId: String, lastSeen: String): Int {
        return supabase.from("notifications")
            .select {
                filter {
                    eq("user_id", userId)
                    gt("created_at", lastSeen)
                }
            }
            .decodeList<Notification>()
            .size
    }


    suspend fun disconnect() {
        channel?.unsubscribe()
        channel = null
    }

    suspend fun markAsRead(notificationId: String) {
        supabase.from("notifications")
            .update(mapOf("is_read" to true)) {
                filter { eq("id", notificationId) }
            }
    }
}