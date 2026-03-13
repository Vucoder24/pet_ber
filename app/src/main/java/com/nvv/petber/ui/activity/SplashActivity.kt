package com.nvv.petber.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivitySplashBinding
import com.nvv.petber.ui.auth.login.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    @Inject
    lateinit var supabaseClient: SupabaseClient
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
                    val session = supabaseClient.auth.currentSessionOrNull()
                    delayJob.join()

                    if (session != null) {
                        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                    } else {
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    }
                    finish()
                } catch (e: Exception) {
                    delayJob.join()
                    Toast.makeText(this@SplashActivity, "Error checking session: ${e.message}", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }
}