package com.nvv.petber.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AppEventManager {
    private val _refreshStoriesEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val refreshStoriesEvent = _refreshStoriesEvent.asSharedFlow()

    fun triggerRefreshStories() {
        _refreshStoriesEvent.tryEmit(Unit)
    }
}