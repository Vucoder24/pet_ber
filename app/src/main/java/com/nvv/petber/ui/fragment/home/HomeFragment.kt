package com.nvv.petber.ui.fragment.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.databinding.FragmentHomeBinding
import com.nvv.petber.ui.view_story.ViewStoryActivity
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.adapter.StoryAdapter
import com.nvv.petber.ui.adapter.StoryRowAdapter
import com.nvv.petber.utils.AppEventManager
import com.nvv.petber.utils.ext.addFeedScrollListener
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter
    private var scrollListener: RecyclerView.OnScrollListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        setupAdapters()
        setupRecyclerView()
        observeUiState()
        observeEventBus()
    }

    private fun observeEventBus() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                AppEventManager.refreshStoriesEvent.collect {
                    viewModel.loadPosts(refresh = true)
                }
            }
        }
    }

    private fun initViews() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadInitialData()
        }

        // set color scheme for swipe refresh layout
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.text_color,
        )
    }

    private fun setupAdapters() {
        storyAdapter = StoryAdapter(
            onStoryClick = { story ->
                val allStories = storyAdapter.originalStories
                val groupedStories = allStories.groupBy { it.userId }.map { entry ->
                    UserStoryGroup(
                        userId = entry.key,
                        user = entry.value.first().users,
                        stories = entry.value
                    )
                }

                val initialPosition = groupedStories.indexOfFirst { it.userId == story.userId }

                val intent = Intent(requireContext(), ViewStoryActivity::class.java).apply {
                    putExtra(ViewStoryActivity.EXTRA_STORY_GROUPS, Json.encodeToString(groupedStories))
                    putExtra(ViewStoryActivity.EXTRA_INITIAL_POSITION, initialPosition)
                }
                startActivity(intent)
            }
        )

        postAdapter = PostAdapter(
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->

            },
            onShareClick = { },
            onProfileClick = { post ->

            },
            onLoadMore = { viewModel.loadPosts(refresh = false) },
            onSaveClick = {

            }
        )
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter(
            StoryRowAdapter(storyAdapter),
            postAdapter
        )

        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.rvFeed.apply {
            layoutManager = linearLayoutManager
            adapter = concatAdapter
            setHasFixedSize(false)

            scrollListener = addFeedScrollListener(
                layoutManager = linearLayoutManager,
                headerCount = 1,
                preloadCount = 3,
                onPauseItem = { viewHolder ->
                    if (viewHolder is PostAdapter.PostViewHolder) {
                        viewHolder.pausePlayer()
                    }
                },
                onPreloadItem = { index ->
                    val currentList = postAdapter.currentList
                    if (index < currentList.size) {
                        currentList.getOrNull(index)?.postMedia?.firstOrNull()?.mediaUrl?.let { url ->
                            com.bumptech.glide.Glide.with(requireContext())
                                .load(url)
                                .preload()
                        }
                    }
                }
            )
        }
        scrollListener?.let { binding.rvFeed.addOnScrollListener(it) }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Stories
                    storyAdapter.submitList(state.stories)

                    // Posts
                    postAdapter.submitList(state.posts)

                    // Loading
                    if (state.isLoadingPosts || state.isLoadingStories) {
                        binding.shimmerViewContainer.visible()
                        binding.shimmerViewContainer.startShimmer()
                        binding.dataContainer.gone()
                    } else {
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.gone()
                        binding.dataContainer.visible()
                    }

                    // Swipe refresh
                    binding.swipeRefreshLayout.isRefreshing =
                        state.isRefreshing

                    // Error
                    state.error?.let { error ->
                        requireContext().toast(R.string.error_fetch_data)
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::postAdapter.isInitialized) {
            postAdapter.pauseAllPlayers()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        if (::postAdapter.isInitialized) {
            postAdapter.releaseAllPlayers()
        }
        _binding = null
    }
}