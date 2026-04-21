package com.nvv.petber.utils

import com.nvv.petber.data.model.Notification

object NotificationGrouper {

    private const val GROUP_WINDOW_MS = 5 * 60 * 1000L

    fun group(notifications: List<Notification>): List<Notification> {
        val result = mutableListOf<Notification>()
        val usedIds = mutableSetOf<String>()

        for (notif in notifications) {
            if (notif.id in usedIds) continue

            if (notif.type != "story_reaction") {
                result.add(notif)
                continue
            }

            val duplicates = notifications.filter { other ->
                other.type == "story_reaction" &&
                        other.senderId == notif.senderId &&
                        other.storyId  == notif.storyId &&
                        kotlin.math.abs(
                            parseTime(other.createdAt) - parseTime(notif.createdAt)
                        ) <= GROUP_WINDOW_MS
            }

            usedIds.addAll(duplicates.map { it.id })
            val latest = duplicates.maxByOrNull { parseTime(it.createdAt) } ?: notif
            result.add(latest)
        }

        return result
    }

    private fun parseTime(createdAt: String?): Long {
        if (createdAt == null) return 0L
        return try {
            java.time.Instant.parse(createdAt).toEpochMilli()
        } catch (_: Exception) {
            0L
        }
    }
}