package com.nvv.petber.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.tabs.TabLayoutMediator
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityViewFollowsBinding
import com.nvv.petber.ui.adapter.FollowPagerAdapter
import com.nvv.petber.ui.base.BaseActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ViewFollowsActivity : BaseActivity() {
    private lateinit var binding: ActivityViewFollowsBinding
    private var userId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewFollowsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = intent.getStringExtra(EXTRA_USER_ID) ?: return finish()
        val initialTab = intent.getIntExtra(EXTRA_TAB, 0)
        val username = intent.getStringExtra(EXTRA_USERNAME) ?: ""

        binding.tvUserName.text = username
        binding.btnBack.setOnClickListener { finish() }

        val adapter = FollowPagerAdapter(this, userId!!)
        binding.viewPager.adapter = adapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.pet_following)
                1 -> getString(R.string.followers)
                2 -> getString(R.string.following)
                3 -> getString(R.string.friends)
                else -> ""
            }
        }.attach()

        binding.viewPager.setCurrentItem(initialTab, false)
    }

    companion object {
        fun newIntent(
            context: Context,
            userId: String,
            extraTab: Int,
            userName: String
        ): Intent {
            return Intent(context, ViewFollowsActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
                putExtra(EXTRA_USERNAME, userName)
                putExtra(EXTRA_TAB, extraTab)
            }
        }
        const val EXTRA_USER_ID = "extra_user_id"
        const val EXTRA_TAB = "extra_tab"
        const val EXTRA_USERNAME = "extra_username"
    }
}