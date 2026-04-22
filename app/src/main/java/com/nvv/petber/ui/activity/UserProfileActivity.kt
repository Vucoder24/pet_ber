package com.nvv.petber.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityUserProfileBinding
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.fragment.profile.OtherUserProfileFragment
import com.nvv.petber.ui.fragment.profile.ProfileFragment
import com.nvv.petber.utils.SharePrefUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UserProfileActivity : BaseActivity() {
    private lateinit var binding: ActivityUserProfileBinding

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: String) {
            val intent = Intent(context, UserProfileActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val targetUserId = intent.getStringExtra(EXTRA_USER_ID) ?: return finish()

        val currentUserId = SharePrefUtils.getCurrentUserId(this)

        if (savedInstanceState == null) {
            val fragment = if (targetUserId == currentUserId) {
                ProfileFragment()
            } else {
                OtherUserProfileFragment.newInstance(targetUserId)
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.main, fragment)
                .commit()
        }
    }

}