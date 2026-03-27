package com.nvv.petber.utils

import android.content.Context
import com.nvv.petber.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeUtils {
    fun formatTimeAgo(context: Context, createdAt: String): String {
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
                diffMin < 1 -> context.getString(R.string.time_just_now)
                diffMin < 60 -> context.getString(R.string.time_minutes_ago, diffMin)
                diffHours < 24 -> context.getString(R.string.time_hours_ago, diffHours)
                diffDays < 7 -> context.getString(R.string.time_days_ago, diffDays)
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            createdAt
        }
    }
}