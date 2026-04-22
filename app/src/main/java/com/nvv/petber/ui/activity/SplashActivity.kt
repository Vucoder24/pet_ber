package com.nvv.petber.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivitySplashBinding
import com.nvv.petber.ui.auth.login.LoginActivity
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.toast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // run animation for logo
        val animation = AnimationUtils.loadAnimation(this, R.anim.splash_anim)
        binding.logoContainer.startAnimation(animation)

        lifecycleScope.launch {

            val delayJob = launch { delay(2000) }

            // check Session
            val sessionResult = launch {
                try {
                    val currentUserId = SharePrefUtils.getCurrentUserId(this@SplashActivity)
                    delayJob.join()

                    if (currentUserId.isNotEmpty()) {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                    finish()
                } catch (e: Exception) {
                    delayJob.join()
                    toast(getString(R.string.error_checking_session, e.message))
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}