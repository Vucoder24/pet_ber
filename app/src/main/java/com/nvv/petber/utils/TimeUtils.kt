package com.nvv.petber.utils

import android.content.Context
import com.nvv.petber.R
import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun formatMessageTime(createdAt: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val date = sdf.parse(createdAt.substringBefore(".")) ?: return ""
            val now = Calendar.getInstance()
            val cal = Calendar.getInstance().apply { time = date }

            val isToday = now.get(Calendar.DATE) == cal.get(Calendar.DATE) &&
                    now.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                    now.get(Calendar.YEAR) == cal.get(Calendar.YEAR)

            val isYesterday = run {
                val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
                yesterday.get(Calendar.DATE) == cal.get(Calendar.DATE) &&
                        yesterday.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                        yesterday.get(Calendar.YEAR) == cal.get(Calendar.YEAR)
            }

            val timePart = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)

            when {
                isToday -> timePart
                isYesterday -> "Hôm qua $timePart"
                else -> SimpleDateFormat("d MMM $timePart", Locale.getDefault()).format(date)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun parseToMillis(createdAt: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(createdAt.substringBefore("."))?.time
        } catch (_: Exception) {
            null
        }
    }


    fun shouldShowTime(currentCreatedAt: String, previousCreatedAt: String?): Boolean {
        if (previousCreatedAt == null) return true
        val current = parseToMillis(currentCreatedAt) ?: return true
        val previous = parseToMillis(previousCreatedAt) ?: return true
        return (current - previous) >= 60_000L
    }
}