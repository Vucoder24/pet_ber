package com.nvv.petber.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    fun formatTimeAgo(createdAt: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(createdAt.substringBefore(".")) ?: return createdAt
            val now = Date()
            val diffMs = now.time - date.time
            val diffMin = diffMs / 60000
            val diffHours = diffMin / 60
            val diffDays = diffHours / 24
            when {
                diffMin < 1 -> "Just now"
                diffMin < 60 -> "${diffMin}m ago"
                diffHours < 24 -> "${diffHours}h ago"
                diffDays < 7 -> "${diffDays}d ago"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            createdAt
        }
    }
}