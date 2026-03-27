package com.nvv.petber.utils.ext

import android.content.Context
import android.widget.Toast

fun Context.toast(vararg messages: Any?, duration: Int = Toast.LENGTH_SHORT) {
    val text = messages.joinToString(" ") { message ->
        when (message) {
            is Int -> try {
                this.getString(message)
            } catch (_: Exception) {
                message.toString()
            }
            else -> message?.toString() ?: ""
        }
    }
    Toast.makeText(this, text, duration).show()
}