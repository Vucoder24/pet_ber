package com.nvv.petber.utils

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

@UnstableApi
object VideoCache {

    private var simpleCache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "video_cache")

            @Suppress("DEPRECATION")
            simpleCache = SimpleCache(
                cacheDir,
                LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024) // 200MB
            )
        }
        return simpleCache!!
    }
}

@OptIn(UnstableApi::class)
fun buildCacheDataSource(context: Context): DataSource.Factory {
    val cache = VideoCache.getCache(context)

    val upstreamFactory = DefaultHttpDataSource.Factory()

    return CacheDataSource.Factory()
        .setCache(cache)
        .setUpstreamDataSourceFactory(upstreamFactory)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
}