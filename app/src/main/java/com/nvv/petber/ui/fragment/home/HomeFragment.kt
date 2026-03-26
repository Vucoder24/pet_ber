package com.nvv.petber.ui.fragment.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentHomeBinding
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.adapter.StoryAdapter
import com.nvv.petber.ui.adapter.StoryRowAdapter
import com.nvv.petber.utils.AppEventManager
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var storyAdapter: StoryAdapter
    private lateinit var postAdapter: PostAdapter

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
            viewModel.loadPosts(refresh = true)
        }

        // set color scheme for swipe refresh layout
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.text_color,
        )
    }

    private fun setupAdapters() {
        storyAdapter = StoryAdapter(
            onStoryClick = { story ->
                // navigate to story viewer
                Toast.makeText(
                    requireContext(),
                    "Story: ${story.users?.fullName}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        )

        postAdapter = PostAdapter(
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                Toast.makeText(
                    requireContext(),
                    "Comments",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onShareClick = { },
            onProfileClick = { post ->
                Toast.makeText(
                    requireContext(),
                    "Profile: ${post.users?.username}",
                    Toast.LENGTH_SHORT
                )
                    .show()
            },
            onLoadMore = { viewModel.loadPosts(refresh = false) }
        )
    }

    private fun setupRecyclerView() {
        val concatAdapter = ConcatAdapter(
            StoryRowAdapter(storyAdapter),
            postAdapter
        )

        binding.rvFeed.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = concatAdapter
            setHasFixedSize(false)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Stories
                    Log.d("HomeFragment", "Stories: ${state.stories.size}")
                    storyAdapter.submitList(state.stories)

                    // Posts
                    postAdapter.submitList(state.posts)

                    // Loading
                    if (state.isLoadingPosts) {
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
                        state.isLoadingPosts && state.posts.isNotEmpty()

                    // Error
                    state.error?.let { error ->
                        Toast.makeText(
                            requireContext(),
                            error,
                            Toast.LENGTH_SHORT
                        ).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}