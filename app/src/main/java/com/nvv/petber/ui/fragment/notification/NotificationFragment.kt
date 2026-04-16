package com.nvv.petber.ui.fragment.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nvv.petber.databinding.FragmentNotificationBinding
import com.nvv.petber.ui.adapter.NotificationAdapter
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationFragment : Fragment() {
    private var _binding: FragmentNotificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var currentUserId: String
    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var notificationAdapter: NotificationAdapter
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentNotificationBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSwipeRefresh()
        observeData()

        // Load data lần đầu
        currentUserId = SharePrefUtils.getCurrentUserId(requireContext())
        mainViewModel.fetchNotifications(currentUserId)
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onClick = {},
            onMoreClick = {

            }
        )
        binding.rvNotifications.adapter = notificationAdapter
    }

    private fun setupSwipeRefresh() {
        binding.root.setOnRefreshListener {
            val userId = SharePrefUtils.getCurrentUserId(requireContext())
            mainViewModel.fetchNotifications(userId)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.isLoading.collect { updateUiState() }
                }

                launch {
                    mainViewModel.notifications.collect { list ->
                        notificationAdapter.submitList(list)
                        updateUiState()
                        binding.root.isRefreshing = false
                    }
                }
            }
        }
    }

    private fun updateUiState() {
        val list = mainViewModel.notifications.value
        val isLoading = mainViewModel.isLoading.value

        if (isLoading) {
            if (!binding.root.isRefreshing) {
                binding.shimmerNotification.visible()
                binding.shimmerNotification.startShimmer()
                binding.rvNotifications.gone()
                binding.layoutEmpty.root.gone()
            }
        } else {
            binding.shimmerNotification.stopShimmer()
            binding.shimmerNotification.gone()
            binding.root.isRefreshing = false

            if (list.isEmpty()) {
                binding.rvNotifications.gone()
                binding.layoutEmpty.root.visible()
            } else {
                binding.rvNotifications.visible()
                binding.layoutEmpty.root.gone()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}