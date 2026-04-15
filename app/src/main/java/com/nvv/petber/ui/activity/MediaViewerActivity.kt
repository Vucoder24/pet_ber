package com.nvv.petber.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nvv.petber.R
import com.nvv.petber.data.model.PostMedia
import com.nvv.petber.databinding.ActivityMediaViewerBinding
import com.nvv.petber.ui.adapter.MediaPagerAdapter

class MediaViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaViewerBinding
    private var mediaList: List<PostMedia> = emptyList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMediaViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initVewPager()
    }

    private fun initVewPager() {
        val jsonMedia = intent.getStringExtra(EXTRA_MEDIA_LIST)
        val startIndex = intent.getIntExtra(EXTRA_START_INDEX, 0)

        binding.btnClose.setOnClickListener {
            finish()
        }

        if (jsonMedia != null) {
            val type = object : TypeToken<List<PostMedia>>() {}.type
            mediaList = Gson().fromJson(jsonMedia, type)

            binding.tabLayoutDots.isVisible = mediaList.size > 1

            val adapter = MediaPagerAdapter(mediaList)
            binding.viewPagerMedia.adapter = adapter
            binding.viewPagerMedia.setCurrentItem(startIndex, false)
            TabLayoutMediator(binding.tabLayoutDots, binding.viewPagerMedia) { _, _ -> }.attach()
        }

        binding.viewPagerMedia.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
            }
        })
    }

    override fun onPause() {
        super.onPause()
        (binding.viewPagerMedia.adapter as? MediaPagerAdapter)?.pauseAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        (binding.viewPagerMedia.adapter as? MediaPagerAdapter)?.releaseAll()
    }

    companion object{
        const val EXTRA_MEDIA_LIST = "EXTRA_MEDIA_LIST"
        const val EXTRA_START_INDEX = "EXTRA_START_INDEX"
    }
}