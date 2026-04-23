package com.nvv.petber.utils

import android.content.Context
import com.nvv.petber.R
import com.nvv.petber.data.model.Notification

object NotificationHelper {

    fun getMessageText(context: Context, notification: Notification): String {
        val senderName = notification.nameSender ?: context.getString(R.string.petber_user)

        return when (notification.type) {
            "post_like" -> context.getString(R.string.notif_type_post_like, senderName)
            "comment" -> context.getString(R.string.notif_type_comment, senderName)
            "comment_reply" -> context.getString(R.string.notif_type_comment_reply, senderName)
            "comment_like" -> context.getString(R.string.notif_type_comment_like, senderName)
            "story_reaction" -> {
                val reaction = notification.message ?: ""
                context.getString(R.string.notif_type_story_reaction, senderName, reaction)
            }
            "user_follow" -> context.getString(R.string.notif_type_user_follow, senderName)
            "pet_follow" -> context.getString(R.string.notif_type_pet_follow, senderName)
            "grouped" -> notification.message.toString()
            "new_post" -> context.getString(R.string.notif_type_new_post, senderName)
            "new_story" -> context.getString(R.string.notif_type_new_story, senderName)
            "pet_tagged" -> context.getString(R.string.notif_type_pet_tagged, senderName)
            else -> notification.message ?: context.getString(R.string.new_notifications)
        }
    }
}