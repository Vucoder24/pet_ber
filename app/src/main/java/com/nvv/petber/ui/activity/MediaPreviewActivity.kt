package com.nvv.petber.ui.activity

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityMediaPreviewBinding
import com.nvv.petber.ui.adapter.MediaItem

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

        val media = intent.getParcelableExtra<MediaItem>(EXTRA_MEDIA)

        media?.let {
            if (it.isVideo) {
                binding.videoView.visibility = View.VISIBLE
                binding.videoView.setVideoURI(it.uri)
                binding.videoView.start()
            } else {
                binding.imageView.visibility = View.VISIBLE
                binding.imageView.setImageURI(it.uri)
            }
        }

        binding.btnClose.setOnClickListener {
            finish()
        }
    }

    companion object{
        const val EXTRA_MEDIA = "extra_media"
    }
}