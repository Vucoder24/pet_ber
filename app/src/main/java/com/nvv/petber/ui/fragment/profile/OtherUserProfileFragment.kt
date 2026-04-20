package com.nvv.petber.ui.fragment.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.data.model.User
import com.nvv.petber.databinding.FragmentOtherUserProfileBinding
import com.nvv.petber.ui.activity.MediaPreviewActivity
import com.nvv.petber.ui.activity.PetProfileActivity
import com.nvv.petber.ui.activity.ViewFollowsActivity
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.ui.adapter.PetProfileAdapter
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.utils.DateTimeUtils
import com.nvv.petber.utils.ext.formatSocialCount
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.loadImage
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.UserProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OtherUserProfileFragment : Fragment() {
    private var _binding: FragmentOtherUserProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UserProfileViewModel by viewModels()

    private lateinit var historyPostAdapter: PostAdapter
    private lateinit var petProfileAdapter: PetProfileAdapter
    private var userData: User? = null
    private lateinit var targetUserId: String

    @Inject
    lateinit var exoPlayer: ExoPlayer

    companion object {
        private const val ARG_USER_ID = "arg_user_id"

        fun newInstance(userId: String) = OtherUserProfileFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_USER_ID, userId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOtherUserProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetUserId = arguments?.getString(ARG_USER_ID) ?: return requireActivity().finish()

        setupFragmentResultListener()
        viewModel.loadUserProfile(targetUserId)

        initView()
        setupListener()
        observerData()
    }

    private fun setupFragmentResultListener() {
        childFragmentManager.setFragmentResultListener("refresh_key", viewLifecycleOwner) { _, bundle ->
            val isUpdated = bundle.getBoolean("bundle_is_updated", false)
            if (isUpdated) {
                viewModel.refreshProfile()
            }
        }
    }

    private fun initView() {
        historyPostAdapter = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post -> viewModel.toggleLike(post) },
            onCommentClick = { post ->
                val bottomSheet = CommentBottomSheetFragment.newInstance(post.id, post.userId)
                bottomSheet.show(childFragmentManager, "CommentBottomSheet")
            },
            onShareClick = { post ->
                val link = "https://project-ilyyx.vercel.app/post/${post.id}"
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, link)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_post)))
                viewModel.incrementShareCount(post.id)
            },
            onProfileClick = {
                binding.dataContainer.smoothScrollTo(0, 0)
            },
            onMoreOption = { post ->
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(post)
                bottomSheet.show(childFragmentManager, "PostOptionsBottomSheet")
            }
        )

        petProfileAdapter = PetProfileAdapter(
            isOwner = false,
            onClick = { pet -> PetProfileActivity.start(requireContext(), pet) },
            onAddClick = { }
        )

        binding.rvPets.apply {
            adapter = petProfileAdapter
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        binding.rvPosts.apply {
            adapter = historyPostAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        binding.dataContainer.setOnScrollChangeListener { v: NestedScrollView, _, scrollY, _, oldScrollY ->
            checkVideoVisibility()

            if (scrollY > oldScrollY) {
                val childHeight = v.getChildAt(0).measuredHeight
                val scrollHeight = v.measuredHeight

                if (scrollY >= childHeight - scrollHeight - 200) {
                    viewModel.loadMorePosts()
                }
            }
        }
    }

    private fun checkVideoVisibility() {
        val scrollRect = android.graphics.Rect()
        binding.dataContainer.getGlobalVisibleRect(scrollRect)

        for (i in 0 until binding.rvPosts.childCount) {
            val child = binding.rvPosts.getChildAt(i)
            val viewHolder = binding.rvPosts.getChildViewHolder(child)

            if (viewHolder is PostAdapter.PostViewHolder) {
                val childRect = android.graphics.Rect()
                child.getGlobalVisibleRect(childRect)

                if (!android.graphics.Rect.intersects(scrollRect, childRect)) {
                    viewHolder.pausePlayer()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observerData() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                userData = it
                bindUserToUI(it)
            }
        }
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            historyPostAdapter.submitPostData(
                list = posts,
                isLoadingMore = viewModel.isLoadMore.value ?: false
            )
        }

        viewModel.isLoadMore.observe(viewLifecycleOwner) { isLoadMore ->
            historyPostAdapter.submitPostData(
                list = viewModel.posts.value ?: emptyList(),
                isLoadingMore = isLoadMore
            )
        }

        viewModel.pets.observe(viewLifecycleOwner) {
            petProfileAdapter.submitPets(it)
        }

        viewModel.isFollowing.observe(viewLifecycleOwner) { isFollowing ->
            updateFollowButtonUI(isFollowing)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            if (loading && viewModel.user.value == null) {
                showShimmer()
            } else hideShimmer()
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.root.isRefreshing = refreshing
        }
    }

    private fun updateFollowButtonUI(isFollowing: Boolean) {
        binding.btnFollow.apply {
            if (isFollowing) {
                text = getString(R.string.following)
                setBackgroundColor(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.gray_light
                    )
                )
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                setIconTintResource(R.color.black)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_following)
            } else {
                text = getString(R.string.follow)
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.bg_btn))
                setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                setIconTintResource(R.color.white)
                icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_follow)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindUserToUI(user: User) {
        user.let {
            binding.tvFullName.text = if (!it.fullName.isNullOrEmpty()) it.fullName
            else getString(R.string.petber_user)

            binding.tvFriendsCount.text = it.friendsCount.formatSocialCount()
            binding.tvPetFollowingCount.text = it.petFollowingCount.formatSocialCount()
            binding.tvFollowerCount.text = it.followerCount.formatSocialCount()
            binding.tvFollowingCount.text = it.followingCount.formatSocialCount()

            binding.tvUserName.text = "@${it.username}"
            binding.tvBio.text = if (!it.bio.isNullOrEmpty()) it.bio
            else getString(R.string.add_bio)

            binding.tvAddress.text = if (!it.address.isNullOrEmpty()) it.address
            else getString(R.string.add_address)

            binding.tvPhone.text = if (!it.phone.isNullOrEmpty()) it.phone
            else getString(R.string.add_phone_number)

            binding.tvGender.text = if (!it.gender.isNullOrEmpty()) it.gender
            else getString(R.string.add_gender)

            binding.tvBirthday.text =
                if (!it.birthday.isNullOrEmpty()) DateTimeUtils.formatToDisplay(it.birthday)
                else getString(R.string.add_birthday)

            binding.tvHobbies.text = if (!it.hobbies.isNullOrEmpty()) it.hobbies
            else getString(R.string.add_hobbies)

            binding.imgCover.loadImage(it.coverUrl)
            binding.avatar.loadAvatar(it.avatarUrl)
        }
    }

    private fun showShimmer() {
        binding.shimmerView.startShimmer()
        binding.shimmerView.visible()
        binding.dataContainer.gone()
    }

    private fun hideShimmer() {
        binding.shimmerView.stopShimmer()
        binding.shimmerView.gone()
        binding.dataContainer.visible()
    }

    private fun setupListener() {
        binding.apply {
            root.setOnRefreshListener {
                viewModel.refreshProfile()
            }

            btnFollow.setOnClickListener {
                viewModel.toggleFollow()
            }

            imgCover.setOnClickListener {
                val currentCoverUrl = userData?.coverUrl
                if (!currentCoverUrl.isNullOrEmpty()) {
                    val mediaItem =
                        MediaItem(uri = currentCoverUrl.toUri(), isVideo = false, duration = 0L)
                    startActivity(
                        Intent(
                            requireContext(),
                            MediaPreviewActivity::class.java
                        ).apply {
                            putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                        })
                }
            }

            avatar.setOnClickListener {
                val currentAvatarUrl = userData?.avatarUrl
                if (!currentAvatarUrl.isNullOrEmpty()) {
                    val mediaItem =
                        MediaItem(uri = currentAvatarUrl.toUri(), isVideo = false, duration = 0L)
                    startActivity(
                        Intent(
                            requireContext(),
                            MediaPreviewActivity::class.java
                        ).apply {
                            putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                        })
                }
            }

            tvBio.setOnClickListener {
                tvBio.maxLines = if (tvBio.maxLines == 2) Int.MAX_VALUE else 2
            }
            tvAddress.setOnClickListener {
                tvAddress.maxLines = if (tvAddress.maxLines == 1) Int.MAX_VALUE else 1
            }
            tvHobbies.setOnClickListener {
                tvHobbies.maxLines = if (tvHobbies.maxLines == 1) Int.MAX_VALUE else 1
            }

            itemPetFollowing.setOnClickListener { handleViewList(targetUserId, 0) }
            itemFollowers.setOnClickListener { handleViewList(targetUserId, 1) }
            itemFollowing.setOnClickListener { handleViewList(targetUserId, 2) }
            itemFriends.setOnClickListener { handleViewList(targetUserId, 3) }

            // Xử lý nút back
            btnBack.setOnClickListener { requireActivity().finish() }
        }
    }

    private fun handleViewList(userId: String, extraTab: Int) {
        val userName = userData?.username ?: return
        startActivity(ViewFollowsActivity.newIntent(requireContext(), userId, extraTab, userName))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::historyPostAdapter.isInitialized) {
            historyPostAdapter.pauseAllPlayers()
        }
        binding.rvPosts.adapter = null
        binding.dataContainer.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
        _binding = null
    }
}