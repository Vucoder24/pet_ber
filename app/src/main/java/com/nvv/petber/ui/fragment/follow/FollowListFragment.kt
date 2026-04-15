package com.nvv.petber.ui.fragment.follow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.databinding.FragmentFollowListBinding
import com.nvv.petber.ui.adapter.FollowPetAdapter
import com.nvv.petber.ui.adapter.FollowUserAdapter
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.FollowViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FollowListFragment : Fragment() {
    private var _binding: FragmentFollowListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: FollowViewModel by viewModels()
    private lateinit var adapter: FollowUserAdapter
    private lateinit var petAdapter: FollowPetAdapter

    private var targetUserId: String = ""
    private var listType: Int = 0 // 0: Followers, 1: Following, 2: Friends

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFollowListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        targetUserId = arguments?.getString(ARG_USER_ID) ?: return
        listType = arguments?.getInt(ARG_TYPE) ?: 0

        setupRecyclerView()
        observeData()

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadData(targetUserId, listType)
        }

        viewModel.loadData(targetUserId, listType)
    }

    private fun setupRecyclerView() {
        if (listType == 0) {
            petAdapter = FollowPetAdapter(
                onItemClick = { petId -> /* sang profile pet */ },
                onUnfollowClick = { petId ->
                    viewModel.unfollowPet(petId)
                }
            )
            binding.rvList.adapter = petAdapter
        } else {
            adapter = FollowUserAdapter(
                currentUserId = SharePrefUtils.getCurrentUserId(requireContext()),
                onFollowClick = { uiModel ->
                    viewModel.toggleFollow(uiModel.user.id, uiModel.isFollowing, listType)
                },
                onItemClick = { userId ->
                    // Chuyển sang profile user khác
                }
            )
            binding.rvList.adapter = adapter
        }
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (listType == 0) {
                    viewModel.petFollowingState.collect { state ->
                        updateUI(state.isLoading, state.pets, state.error) {
                            if (::petAdapter.isInitialized) {
                                petAdapter.submitList(state.pets)
                            }
                        }
                    }
                } else {
                    val stateFlow = when (listType) {
                        1 -> viewModel.followersState
                        2 -> viewModel.followingState
                        3 -> viewModel.friendsState
                        else -> null
                    }
                    stateFlow?.collect { state ->
                        updateUI(state.isLoading, state.users, state.error) {
                            if (::adapter.isInitialized) {
                                adapter.submitList(state.users)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun <T> updateUI(
        isLoading: Boolean,
        data: List<T>,
        error: String?,
        onDataLoaded: () -> Unit
    ) {
        binding.swipeRefresh.isRefreshing = false
        if (isLoading && data.isEmpty()) {
            binding.shimmerViewContainer.visible(); binding.shimmerViewContainer.startShimmer()
            binding.rvList.gone(); binding.tvEmpty.gone()
        } else {
            binding.shimmerViewContainer.stopShimmer(); binding.shimmerViewContainer.gone()
            if (data.isEmpty()) {
                binding.tvEmpty.visible(); binding.rvList.gone()
            } else {
                binding.tvEmpty.gone(); binding.rvList.visible()
                onDataLoaded()
            }
        }
        error?.let { requireContext().toast(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_USER_ID = "arg_user_id"
        private const val ARG_TYPE = "arg_type"

        fun newInstance(userId: String, type: Int): FollowListFragment {
            return FollowListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ID, userId)
                    putInt(ARG_TYPE, type)
                }
            }
        }
    }
}