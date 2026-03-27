package com.nvv.petber.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityMediaPreviewBinding
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.utils.ext.loadMediaCoverWithExtremeGradient
import com.nvv.petber.utils.ext.visible

class MediaPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMediaPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMediaPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        @Suppress("DEPRECATION")
        val media = intent.getParcelableExtra<MediaItem>(EXTRA_MEDIA)

        media?.let {
            if (it.isVideo) {
                binding.videoView.visible()
                binding.videoView.setVideoURI(it.uri)
                binding.videoView.start()
            } else {
                binding.imageView.visible()
                binding.imageView.setImageURI(it.uri)
            }
            binding.imageView.loadMediaCoverWithExtremeGradient(
                uri = it.uri,
                isVideo = false,
                backgroundView = binding.storyBackground,
                scope = lifecycleScope,
                ctx = this
            )

        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    companion object{
        const val EXTRA_MEDIA = "extra_media"
    }
}