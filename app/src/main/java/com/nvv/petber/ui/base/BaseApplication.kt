package com.nvv.petber.ui.base

import android.app.Application
import android.content.Context
import com.nvv.petber.utils.LocaleHelper
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class BaseApplication : Application(){
    override fun onCreate() {
        super.onCreate()
        EmojiManager.install(GoogleEmojiProvider())
    }

    override fun attachBaseContext(base: Context) {
        val language = LocaleHelper.getLanguage(base)
        super.attachBaseContext(LocaleHelper.setLocale(base, language))
    }
}