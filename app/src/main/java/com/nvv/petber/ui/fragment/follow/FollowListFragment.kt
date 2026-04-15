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

    private var targetUserId: String = ""
    private var listType: Int = 0 // 0: Followers, 1: Following, 2: Friends

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        adapter = FollowUserAdapter(
            currentUserId = SharePrefUtils.getCurrentUserId(requireContext()),
            onFollowClick = { uiModel ->
                viewModel.toggleFollow(uiModel.user.id, uiModel.isFollowing, listType)
            },
            onItemClick = { userId ->
                // Chuyển sang profile user khác
            }
        )
        binding.rvList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvList.adapter = adapter
    }

    private fun observeData() {
        val stateFlow = when (listType) {
            0 -> viewModel.followersState
            1 -> viewModel.followingState
            else -> viewModel.friendsState
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                stateFlow.collect { state ->
                    binding.swipeRefresh.isRefreshing = false

                    if (state.isLoading && state.users.isEmpty()) {
                        binding.shimmerViewContainer.visible()
                        binding.shimmerViewContainer.startShimmer()
                        binding.rvList.gone()
                        binding.tvEmpty.gone()
                    } else {
                        binding.shimmerViewContainer.stopShimmer()
                        binding.shimmerViewContainer.gone()

                        if (state.users.isEmpty()) {
                            binding.tvEmpty.visible()
                            binding.rvList.gone()
                        } else {
                            binding.tvEmpty.gone()
                            binding.rvList.visible()
                            adapter.submitList(state.users)
                        }
                    }

                    state.error?.let {
                        requireContext().toast(it)
                    }
                }
            }
        }
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