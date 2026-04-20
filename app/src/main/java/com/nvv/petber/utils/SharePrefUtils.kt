package com.nvv.petber.utils

import android.content.Context
import androidx.core.content.edit

object SharePrefUtils {

    private const val PREF_NAME = "petber_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"

    fun saveCurrentUserId(context: Context, userId: String) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit { putString(KEY_CURRENT_USER_ID, userId) }
    }

    fun getCurrentUserId(context: Context): String {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getString(KEY_CURRENT_USER_ID, "") ?: ""
    }

    fun clear(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit { clear() }
    }

    fun saveLastSeenNotificationTime(context: Context, time: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit { putString("last_seen_notification", time) }
    }

    fun getLastSeenNotificationTime(context: Context): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("last_seen_notification", "1970-01-01T00:00:00Z")!!
    }
}