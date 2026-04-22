package com.nvv.petber.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivitySettingsBinding
import com.nvv.petber.ui.auth.login.LoginActivity
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.showLogoutConfirmDialog
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        currentUserId = SharePrefUtils.getCurrentUserId(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.apply {
            btnPostSaved.setOnClickListener {
                ViewPostSavedActivity.start(this@SettingsActivity, currentUserId)
            }

            btnLanguage.setOnClickListener {
                val intent = Intent(this@SettingsActivity, SetLanguageActivity::class.java)
                startActivity(intent)
            }

            btnBack.setOnClickListener {
                finish()
            }
            btnRecentDeletedPost.setOnClickListener {
                RecentDeletedPostActivity.start(this@SettingsActivity, currentUserId)
            }


            btnLogout.setOnClickListener {
                showLogoutConfirmDialog {
                    viewModel.logout(
                        onSuccess = {
                            val intent = Intent(this@SettingsActivity, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                            startActivity(intent)
                            finish()
                        },
                        onError = { message ->
                            toast(message)
                        }
                    )
                }
            }
        }
    }
}