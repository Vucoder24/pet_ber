package com.nvv.petber.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext

@Module
@InstallIn(ActivityComponent::class)
object PlayerModule {

    @OptIn(UnstableApi::class)
    @Provides
    fun provideExoPlayer(
        @ActivityContext context: Context
    ): ExoPlayer {

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000, // min buffer
                2000, // max buffer
                500,  // start playback
                1000  // rebuffer
            )
            .build()

        return ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
    }
}