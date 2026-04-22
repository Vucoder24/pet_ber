package com.nvv.petber.ui.view_story

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.nvv.petber.R
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.databinding.ActivityViewStoryBinding
import com.nvv.petber.ui.adapter.StoryPagerAdapter
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.StoryHostState
import com.nvv.petber.viewmodel.StoryHostViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ViewStoryActivity : BaseActivity() {
    private lateinit var binding: ActivityViewStoryBinding
    private var storyGroups: List<UserStoryGroup> = emptyList()
    private var currentPosition = -1
    private val hostViewModel: StoryHostViewModel by viewModels()
    private lateinit var currentUserId: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        currentUserId = SharePrefUtils.getCurrentUserId(this)

        observeHostState()
        handleIntent()
    }

    private fun handleIntent() {
        val mode = intent.getStringExtra(EXTRA_MODE)
        when (mode) {
            MODE_PRELOADED -> {
                val json = intent.getStringExtra(EXTRA_JSON_DATA) ?: return finish()
                val index = intent.getIntExtra(EXTRA_INITIAL_INDEX, 0)
                hostViewModel.loadFromData(json, index)
            }
            MODE_FETCH_API -> {
                val storyId = intent.getStringExtra(EXTRA_STORY_ID) ?: return finish()
                hostViewModel.loadFromApi(storyId)
            }
            else -> finish()
        }
    }

    private fun observeHostState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                hostViewModel.uiState.collect { state ->
                    when (state) {
                        is StoryHostState.Loading -> {
                            showLoading(true)
                        }
                        is StoryHostState.Success -> {
                            showLoading(false)
                            storyGroups = state.groups
                            setupViewPager(state.initialIndex)
                        }
                        is StoryHostState.Error -> {
                            showLoading(false)
                            toast(state.message)
                            finish()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.loadingContainer.visibility =
            if (isLoading) android.view.View.VISIBLE else android.view.View.GONE
        binding.viewPager.isUserInputEnabled = !isLoading
    }

    private fun setupViewPager(initialPosition: Int) {
        val adapter = StoryPagerAdapter(this, storyGroups, currentUserId)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.setCurrentItem(initialPosition, false)

        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // find fragment present
                val oldFragment =
                    supportFragmentManager.findFragmentByTag("f$currentPosition") as? IStoryPage
                val newFragment =
                    supportFragmentManager.findFragmentByTag("f$position") as? IStoryPage

                newFragment?.onUserSwipedTo()
                oldFragment?.onFragmentInactive()
                newFragment?.onFragmentActive()

                currentPosition = position
            }
        })
    }

    fun moveToNextUser() {
        val current = binding.viewPager.currentItem
        if (current < storyGroups.size - 1) {
            binding.viewPager.currentItem = current + 1
        } else {
            finish()
        }
    }

    fun moveToPreviousUser() {
        val current = binding.viewPager.currentItem
        if (current > 0) {
            binding.viewPager.currentItem = current - 1
        }
    }

    companion object{
        private const val EXTRA_MODE = "EXTRA_MODE"
        private const val MODE_PRELOADED = "MODE_PRELOADED"
        private const val MODE_FETCH_API = "MODE_FETCH_API"

        private const val EXTRA_JSON_DATA = "EXTRA_JSON_DATA"
        private const val EXTRA_INITIAL_INDEX = "EXTRA_INITIAL_INDEX"
        private const val EXTRA_STORY_ID = "EXTRA_STORY_ID"

        fun startWithData(context: Context, jsonGroups: String, initialIndex: Int) {
            val intent = Intent(context, ViewStoryActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_PRELOADED)
                putExtra(EXTRA_JSON_DATA, jsonGroups)
                putExtra(EXTRA_INITIAL_INDEX, initialIndex)
            }
            context.startActivity(intent)
        }

        fun startWithId(context: Context, storyId: String) {
            val intent = Intent(context, ViewStoryActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_FETCH_API)
                putExtra(EXTRA_STORY_ID, storyId)
            }
            context.startActivity(intent)
        }
    }
}