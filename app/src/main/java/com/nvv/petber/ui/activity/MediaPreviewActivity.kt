package com.nvv.petber.ui.activity

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityMediaPreviewBinding
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.utils.ext.loadMediaCoverWithExtremeGradient
import com.nvv.petber.utils.ext.visible
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaPreviewActivity : BaseActivity() {
    private lateinit var binding: ActivityMediaPreviewBinding
    @Inject lateinit var exoPlayer: ExoPlayer

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
                binding.playerView.visible()
                binding.playerView.player = exoPlayer

                val mediaItem = androidx.media3.common.MediaItem.fromUri(it.uri)
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.play()

                // get duration
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) {
                            val duration = exoPlayer.duration
                            it.duration = duration
                        }
                    }
                })
            } else {
                binding.imageView.visible()
                Glide.with(this).load(it.uri).into(binding.imageView)
            }
            binding.imageView.loadMediaCoverWithExtremeGradient(
                uri = it.uri,
                isVideo = it.isVideo,
                backgroundView = binding.storyBackground,
                scope = lifecycleScope,
                ctx = this
            )

        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
    override fun onPause() {
        super.onPause()
        exoPlayer.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
        binding.playerView.player = null
    }



    companion object{
        const val EXTRA_MEDIA = "extra_media"
    }
}