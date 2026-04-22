package com.nvv.petber.ui.fragment.notification

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentNotificationBinding
import com.nvv.petber.ui.activity.MainActivity
import com.nvv.petber.ui.activity.PetProfileActivity
import com.nvv.petber.ui.activity.PostDetailActivity
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.NotificationAdapter
import com.nvv.petber.ui.view_story.ViewStoryActivity
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.showNotificationOptionBottomSheet
import com.nvv.petber.utils.ext.toast
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
    private lateinit var linearLayoutManager: LinearLayoutManager

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

        currentUserId = SharePrefUtils.getCurrentUserId(requireContext())
        mainViewModel.fetchNotifications(currentUserId, isRefresh = true)
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter(
            onClick = {notification ->
                if (!notification.isRead) {
                    mainViewModel.markAsRead(notification.id)
                }

                when (notification.type) {
                    "post_like", "comment", "comment_reply", "comment_like" -> {
                        notification.postId?.let { postId ->
                             val intent = Intent(requireContext(), PostDetailActivity::class.java)
                             intent.putExtra(PostDetailActivity.EXTRA_POST_ID, postId)
                             startActivity(intent)
                        }
                    }
                    "story_reaction" -> {
                        notification.storyId?.let { storyId ->
                            ViewStoryActivity.startWithId(requireContext(), storyId)
                        }
                    }
                    "user_follow" -> {
                        notification.senderId?.let { senderId ->
                            UserProfileActivity.start(requireContext(), senderId)
                        }
                    }
                    "pet_follow" -> {
                        notification.petId?.let { petId ->
                             PetProfileActivity.start(requireContext(), petId)
                        }
                    }
                }
            },
            onMoreClick = { notification ->
                requireContext().showNotificationOptionBottomSheet(
                    onDelete = {
                        mainViewModel.deleteNotification(notification.id)
                    }
                )
            }
        )
        linearLayoutManager = LinearLayoutManager(requireContext())

        binding.rvNotifications.apply {
            adapter = notificationAdapter
            this.layoutManager = this@NotificationFragment.linearLayoutManager

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy > 0) {
                        val visibleItemCount = linearLayoutManager.childCount
                        val totalItemCount = linearLayoutManager.itemCount
                        val pastVisibleItems = linearLayoutManager.findFirstVisibleItemPosition()

                        if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                            mainViewModel.fetchNotifications(currentUserId, isRefresh = false)
                        }
                    }
                }
            })
        }
    }

    private fun setupSwipeRefresh() {
        binding.root.setOnRefreshListener {
            val userId = SharePrefUtils.getCurrentUserId(requireContext())
            mainViewModel.fetchNotifications(userId, isRefresh = true)
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    mainViewModel.isLoading.collect { updateUiState() }
                }
                launch {
                    mainViewModel.isLoadMore.collect { isLoadMore ->
                        notificationAdapter.isLoadMore = isLoadMore
                    }
                }

                launch {
                    mainViewModel.notifications.collect { list ->
                        notificationAdapter.submitList(list)
                        updateUiState()
                        binding.root.isRefreshing = false
                        if (!mainViewModel.isLoading.value && list.isNotEmpty()) {
                            hideNotificationsBadge()
                        }
                    }
                }

                launch {
                    mainViewModel.toastEvent.collect { message ->
                       requireContext().toast(message)
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

    private fun hideNotificationsBadge(){
        val now = java.time.Instant.now().toString()
        SharePrefUtils.saveLastSeenNotificationTime(requireContext(), now)
        (activity as? MainActivity)?.hideBadge(R.id.navigation_notifications)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}