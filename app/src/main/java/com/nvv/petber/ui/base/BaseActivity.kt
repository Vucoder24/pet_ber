package com.nvv.petber.ui.base

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.nvv.petber.utils.LocaleHelper
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = LocaleHelper.getLanguage(newBase)
        val context = LocaleHelper.setLocale(newBase, language)
        super.attachBaseContext(context)
    }
}