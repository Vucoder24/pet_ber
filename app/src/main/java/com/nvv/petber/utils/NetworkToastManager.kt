package com.nvv.petber.utils

import android.content.Context
import com.nvv.petber.R
import com.nvv.petber.utils.ext.toast

object NetworkToastManager {
    private var lastTime = 0L

    fun show(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastTime > 3000) {
            context.toast(context.getString(R.string.error_connection_internet))
            lastTime = now
        }
    }
}