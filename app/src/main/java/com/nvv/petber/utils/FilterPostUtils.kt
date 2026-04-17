package com.nvv.petber.utils

import com.nvv.petber.data.model.DiaryMonth
import com.nvv.petber.data.model.Post
import java.text.SimpleDateFormat
import java.util.Locale

object FilterPostUtils {
    fun groupPostsByMonth(posts: List<Post>): List<DiaryMonth> {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMMM yyyy", Locale.US)

        val groupedMap = posts.groupBy { post ->
            try {
                val date = post.createdAt?.let { inputFormat.parse(it) }
                date?.let { outputFormat.format(it) } ?: "Unknown"
            } catch (_: Exception) {
                "Unknown"
            }
        }

        return groupedMap.map { DiaryMonth(it.key, it.value) }
    }

}