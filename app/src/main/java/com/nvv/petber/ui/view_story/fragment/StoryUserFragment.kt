package com.nvv.petber.ui.view_story.fragment

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.nvv.petber.R
import com.nvv.petber.data.model.Story
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.databinding.FragmentStoryUserBinding
import com.nvv.petber.ui.view_story.ViewStoryActivity
import com.nvv.petber.utils.TimeUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.loadMediaCoverWithExtremeGradient
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.StoryNavigationEvent
import com.nvv.petber.viewmodel.StoryUiState
import com.nvv.petber.viewmodel.ViewStoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class StoryUserFragment : Fragment() {
    private var _binding: FragmentStoryUserBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ViewStoryViewModel by viewModels()
    private val progressBars = mutableListOf<ProgressBar>()
    private var videoSyncJob: Job? = null
    private var exoPlayer: ExoPlayer? = null
    private var isHolding = false
    private var lastRenderedIndex = -1

    companion object {
        private const val ARG_STORY_GROUP = "arg_story_group"
        fun newInstance(jsonGroup: String) = StoryUserFragment().apply {
            arguments = Bundle().apply { putString(ARG_STORY_GROUP, jsonGroup) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_STORY_GROUP)?.let {
            val group = Json.decodeFromString<UserStoryGroup>(it)
            viewModel.initData(group)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoryUserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializePlayer()
        setupUI()
        observeViewModel()
        setupTouchListener()
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        binding.videoView.player = exoPlayer

        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        showLoading()
                    }
                    Player.STATE_READY -> {
                        hideLoading()
                        val duration = exoPlayer?.duration ?: 0L
                        if (duration > 0) {
                            viewModel.setVideoDurationAndStart(duration)
                            startVideoSync()
                        }
                    }
                    Player.STATE_ENDED -> {
                        viewModel.nextStory()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                viewModel.nextStory()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isHolding) viewModel.resumeTimer()
        if (binding.videoView.isVisible && !isHolding) {
            exoPlayer?.play()
            startVideoSync()
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseTimer()
        exoPlayer?.pause()
        stopVideoSync()
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            requireActivity().finish()
        }
    }

    private fun showLoading() {
        binding.pbLoading.visible()
        viewModel.pauseTimer()
    }

    private fun hideLoading() {
        binding.pbLoading.gone()
        if (!isHolding) {
            viewModel.resumeTimer()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        handleUiState(state)
                    }
                }
                launch {
                    viewModel.navigationEvent.collect { event ->
                        when (event) {
                            StoryNavigationEvent.NEXT_USER -> {
                                (requireActivity() as? ViewStoryActivity)?.moveToNextUser()
                            }

                            StoryNavigationEvent.PREV_USER -> {
                                (requireActivity() as? ViewStoryActivity)?.moveToPreviousUser()
                            }

                            StoryNavigationEvent.RESTART_CURRENT_STORY -> {
                                if (binding.videoView.isVisible) {
                                    exoPlayer?.seekTo(0L)
                                    exoPlayer?.play()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleUiState(state: StoryUiState) {
        val group = state.storyGroup ?: return

        if (progressBars.isEmpty() && group.stories.isNotEmpty()) {
            setupProgressBars(group.stories.size)
            binding.tvFullName.text = group.user?.fullName ?: getString(R.string.petber_user)
            binding.ivAvatar.loadAvatar(group.user?.avatarUrl)
        }

        // Load new content if index changes
        if (state.currentIndex != lastRenderedIndex) {
            lastRenderedIndex = state.currentIndex
            loadMediaContent(group.stories[state.currentIndex])
            // preload content
            preloadNextMedia(group, state.currentIndex)
        }

        // Update progress bar status
        updateProgressBars(state)

        // update UI pause
        if (isHolding) {
            binding.groupUserInfo.gone()
        } else {
            binding.groupUserInfo.visible()
        }
    }

    private fun setupProgressBars(count: Int) {
        binding.llProgressContainer.removeAllViews()
        progressBars.clear()
        val layoutParams =
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                setMargins(4, 0, 4, 0)
            }
        for (i in 0 until count) {
            val pb = ProgressBar(
                requireContext(),
                null,
                android.R.attr.progressBarStyleHorizontal
            ).apply {
                this.layoutParams = layoutParams
                max = 10000
                progress = 0
                progressDrawable =
                    AppCompatResources.getDrawable(requireContext(), R.drawable.bg_story_progress)
            }
            binding.llProgressContainer.addView(pb)
            progressBars.add(pb)
        }
    }

    private fun loadMediaContent(story: Story) {
        story.createdAt?.let {
            binding.tvTime.text = TimeUtils.formatTimeAgo(requireContext(), it)
        }
        val isVideo = story.mediaType.contains("video", ignoreCase = true)

        val uri = story.mediaUrl.toUri()

        binding.ivMedia.loadMediaCoverWithExtremeGradient(
            uri = uri,
            isVideo = isVideo,
            backgroundView = binding.storyBackground,
            scope = viewLifecycleOwner.lifecycleScope,
            ctx = requireContext()
        )

        exoPlayer?.stop()
        stopVideoSync()
        showLoading()

        if (story.mediaType.contains("video", ignoreCase = true)) {
            binding.ivMedia.gone()
            binding.videoView.visible()

            val mediaItem = MediaItem.fromUri(story.mediaUrl)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.play()

        } else {
            binding.videoView.gone()
            binding.ivMedia.visible()

            Glide.with(this)
                .load(story.mediaUrl)
                .listener(object : RequestListener<Drawable> {

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<Drawable?>,
                        isFirstResource: Boolean
                    ): Boolean {
                        hideLoading()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<Drawable?>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        hideLoading()
                        return false
                    }
                })
                .into(binding.ivMedia)
        }
    }

    private fun updateProgressBars(state: StoryUiState) {
        if (progressBars.isEmpty() || state.currentDuration == 0L) return
        for (i in progressBars.indices) {
            when {
                i < state.currentIndex -> {
                    progressBars[i].progress = 10000
                }

                i == state.currentIndex -> {
                    progressBars[i].progress =
                        ((state.currentProgress.toFloat() / state.currentDuration) * 10000).toInt()
                }

                i > state.currentIndex -> {
                    progressBars[i].progress = 0
                }
            }
        }
    }

    private fun preloadNextMedia(group: UserStoryGroup, currentIndex: Int) {
        val nextIndex = currentIndex + 1
        if (nextIndex < group.stories.size) {
            val nextStory = group.stories[nextIndex]
            if (!nextStory.mediaType.contains("video", ignoreCase = true)) {
                Glide.with(requireContext().applicationContext)
                    .load(nextStory.mediaUrl)
                    .preload()
            }
        }
    }

    private fun startVideoSync() {
        videoSyncJob?.cancel()
        videoSyncJob = viewLifecycleOwner.lifecycleScope.launch {
            while (exoPlayer?.isPlaying == true) {
                viewModel.syncVideoProgress(exoPlayer?.currentPosition ?: 0L)
                delay(50L)
            }
        }
    }

    private fun stopVideoSync() {
        videoSyncJob?.cancel()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        var downTime = 0L
        binding.vTouchOverlay.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downTime = System.currentTimeMillis()
                    isHolding = true
                    binding.groupUserInfo.gone()
                    viewModel.pauseTimer()
                    if (binding.videoView.isVisible) {
                        exoPlayer?.pause()
                    }
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val duration = System.currentTimeMillis() - downTime
                    isHolding = false
                    binding.groupUserInfo.visible()

                    viewModel.resumeTimer()
                    if (binding.videoView.isVisible && exoPlayer?.playbackState == Player.STATE_READY) {
                        exoPlayer?.play()
                        startVideoSync()
                    }

                    if (duration < 200 && event.action == MotionEvent.ACTION_UP) {
                        val x = event.x
                        if (x < v.width * 0.3f) {
                            viewModel.previousStory()
                        } else {
                            viewModel.nextStory()
                        }
                    }
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopVideoSync()
        exoPlayer?.release()
        exoPlayer = null
        _binding = null
    }
}