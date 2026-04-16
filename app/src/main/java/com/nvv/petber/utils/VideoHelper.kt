package com.nvv.petber.utils

import android.content.Context
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

fun getVideoDuration(
    context: Context,
    uri: Uri,
    onResult: (Long) -> Unit
) {
    val player = ExoPlayer.Builder(context).build()

    val mediaItem = androidx.media3.common.MediaItem.fromUri(uri)
    player.setMediaItem(mediaItem)
    player.prepare()

    player.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                val duration = player.duration
                onResult(duration)
                player.release()
            }
        }
    })
}