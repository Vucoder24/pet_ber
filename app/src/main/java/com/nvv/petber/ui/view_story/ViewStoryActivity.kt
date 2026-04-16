package com.nvv.petber.ui.view_story

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.nvv.petber.R
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.databinding.ActivityViewStoryBinding
import com.nvv.petber.ui.adapter.StoryPagerAdapter
import com.nvv.petber.ui.view_story.fragment.StoryUserFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class ViewStoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityViewStoryBinding
    private var storyGroups: List<UserStoryGroup> = emptyList()
    private var currentPosition = -1

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
        getArg()
    }

    fun getArg(){
        val jsonStories = intent.getStringExtra(EXTRA_STORY_GROUPS)
        val initialPosition = intent.getIntExtra(EXTRA_INITIAL_POSITION, 0)

        if (!jsonStories.isNullOrEmpty()) {
            storyGroups = Json.Default.decodeFromString(jsonStories)
            setupViewPager(initialPosition)
        } else {
            finish()
        }
    }

    private fun setupViewPager(initialPosition: Int) {
        val adapter = StoryPagerAdapter(this, storyGroups)
        binding.viewPager.adapter = adapter
        binding.viewPager.offscreenPageLimit = 1
        binding.viewPager.setCurrentItem(initialPosition, false)

        binding.viewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

                // find fragment present
                val oldFragment =
                    supportFragmentManager.findFragmentByTag("f$currentPosition") as? StoryUserFragment
                val newFragment =
                    supportFragmentManager.findFragmentByTag("f$position") as? StoryUserFragment

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
        const val EXTRA_STORY_GROUPS = "EXTRA_STORY_GROUPS"
        const val EXTRA_INITIAL_POSITION = "EXTRA_INITIAL_POSITION"
    }
}