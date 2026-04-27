package com.nvv.petber.utils

import com.nvv.petber.data.model.DiaryMonth
import com.nvv.petber.data.model.PetHealthLog
import com.nvv.petber.data.model.Post
import java.text.SimpleDateFormat
import java.util.Locale

object FilterPostUtils {

    fun groupPostsByMonth(
        posts: List<Post>,
        healthLogs: Map<String, PetHealthLog> = emptyMap()
    ): List<DiaryMonth> {
        return posts
            .groupBy { post ->
                post.createdAt?.substring(0, 7) ?: "Unknown"
            }
            .entries
            .sortedByDescending { it.key }
            .map { (yearMonth, monthPosts) ->
                DiaryMonth(
                    monthYear = formatDisplayMonth(yearMonth),
                    posts = monthPosts,
                    healthLog = healthLogs[yearMonth]
                )
            }
    }

    private fun formatDisplayMonth(yearMonth: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val date = sdf.parse(yearMonth)
            val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            date?.let { outputFormat.format(it) } ?: yearMonth
        } catch (_: Exception) {
            yearMonth
        }
    }
}