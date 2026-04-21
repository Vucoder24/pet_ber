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
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.data.model.UserStoryGroup
import com.nvv.petber.databinding.FragmentHomeBinding
import com.nvv.petber.ui.activity.MainActivity
import com.nvv.petber.ui.activity.PetProfileActivity
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.adapter.StoryAdapter
import com.nvv.petber.ui.adapter.StoryRowAdapter
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.ui.view_story.ViewStoryActivity
import com.nvv.petber.utils.AppEventManager
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.addFeedScrollListener
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter
    private var scrollListener: RecyclerView.OnScrollListener? = null

    @Inject
    lateinit var exoPlayer: ExoPlayer

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
        setupFragmentResultListeners()
    }

    private fun setupFragmentResultListeners() {
        childFragmentManager.setFragmentResultListener(
            "refresh_key",
            viewLifecycleOwner
        ) { _, bundle ->
            val isUpdated = bundle.getBoolean("bundle_is_updated", false)
            if (isUpdated) {
                viewModel.refreshData()
            }
        }
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
            viewModel.refreshData()
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

                ViewStoryActivity.startWithData(
                    requireContext(),
                    Json.encodeToString(groupedStories),
                    initialPosition
                )
            }
        )

        postAdapter = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                val bottomSheet = CommentBottomSheetFragment.newInstance(post.id, post.userId)
                bottomSheet.show(childFragmentManager, "CommentBottomSheet")
            },
            onShareClick = {
                val link = "https://project-ilyyx.vercel.app/post/${it.id}"

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, link)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_post)))
                viewModel.incrementShareCount(it.id)
            },
            onProfileClick = {
                if (it.userId == SharePrefUtils.getCurrentUserId(requireContext())) {
                    (activity as? MainActivity)?.selectProfileTab()
                } else {
                    UserProfileActivity.start(requireContext(), it.userId)
                }
            },
            onMoreOption = {
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(it)
                bottomSheet.show(childFragmentManager, "PostOptionsBottomSheet")
            },
            onTaggedPetClick = { pet ->
                PetProfileActivity.start(requireContext(), pet)
            }
        )
    }

    private fun setupRecyclerView() {
        val storyRowAdapter = StoryRowAdapter(storyAdapter) {
            viewModel.loadStories(refresh = false)
        }

        val concatAdapter = ConcatAdapter(
            storyRowAdapter,
            postAdapter
        )

        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.rvFeed.apply {
            layoutManager = linearLayoutManager
            adapter = concatAdapter
            setHasFixedSize(false)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy > 0) {
                        val layoutManager =
                            recyclerView.layoutManager as? LinearLayoutManager ?: return
                        val visibleItemCount = layoutManager.childCount
                        val totalItemCount = layoutManager.itemCount
                        val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            viewModel.loadPosts(refresh = false)
                        }
                    }
                }
            })


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
                    val item = currentList.getOrNull(index)
                    if (item is PostAdapter.PostItem.Data) {
                        item.post.postMedia?.firstOrNull()?.mediaUrl?.let { url ->
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
                    storyAdapter.submitStoryData(state.stories, state.isLoadingMoreStories)

                    // Posts
                    postAdapter.submitPostData(state.posts, state.isLoadingMore)

                    // init Loading
                    if (state.isInitialLoading) {
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
            postAdapter.pauseAllPlayers()
        }

        binding.rvFeed.adapter = null

        scrollListener?.let { binding.rvFeed.removeOnScrollListener(it) }
        _binding = null
    }
}