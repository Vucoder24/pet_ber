package com.nvv.petber.ui.fragment.profile

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.data.model.User
import com.nvv.petber.databinding.FragmentProfileBinding
import com.nvv.petber.ui.activity.CreateEditPetActivity
import com.nvv.petber.ui.activity.CreateStoryActivity
import com.nvv.petber.ui.activity.CropImageActivity
import com.nvv.petber.ui.activity.EditProfileActivity
import com.nvv.petber.ui.activity.MediaPickerActivity
import com.nvv.petber.ui.activity.MediaPreviewActivity
import com.nvv.petber.ui.activity.PetProfileActivity
import com.nvv.petber.ui.activity.SettingsActivity
import com.nvv.petber.ui.activity.ViewFollowsActivity
import com.nvv.petber.ui.adapter.MediaItem
import com.nvv.petber.ui.adapter.PetProfileAdapter
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.utils.DateTimeUtils
import com.nvv.petber.utils.PermissionUtils
import com.nvv.petber.utils.ext.formatSocialCount
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.loadAvatar
import com.nvv.petber.utils.ext.loadImage
import com.nvv.petber.utils.ext.showAvatarOptionDialog
import com.nvv.petber.utils.ext.showCoverOptionDialog
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.ProfileViewModel
import com.nvv.petber.viewmodel.UpdateUserState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var historyPostAdapter: PostAdapter
    private lateinit var petProfileAdapter: PetProfileAdapter
    private var pendingMediaAction: String = ""

    private var cropTarget: String? = null
    private var userData: User? = null

    @Inject
    lateinit var exoPlayer: ExoPlayer

    private val cropLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uriString = result.data?.getStringExtra(CropImageActivity.EXTRA_RESULT_URI)
                    ?: return@registerForActivityResult
                val uri = uriString.toUri()

                when (cropTarget) {
                    CropImageActivity.TARGET_AVATAR -> viewModel.updateAvatar(uri)
                    CropImageActivity.TARGET_COVER -> viewModel.updateCover(uri)
                }
                cropTarget = null
            }
        }

    private val createPetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.syncPets()
            Log.d("Sync", "Sync pets")
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.all { it.value }
            if (granted) {
                when (pendingMediaAction) {
                    "story" -> openMediaPicker()
                    "avatar" -> openMediaPickerForAvatar()
                    "cover" -> openMediaPickerForCover()
                }
            } else {
                requireContext().toast(getString(R.string.permission_question))
            }
        }

    private val storyPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val medias =
                    result.data?.getParcelableArrayListExtra<MediaItem>(
                        MediaPickerActivity.EXTRA_RESULT_MEDIAS
                    )
                val uris = medias?.map { it.uri }

                if (!uris.isNullOrEmpty()) {
                    openCreateStory(uris.first())
                }
            }
        }

    private val avatarPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val medias =
                    result.data?.getParcelableArrayListExtra<MediaItem>(
                        MediaPickerActivity.EXTRA_RESULT_MEDIAS
                    )
                medias?.firstOrNull()?.uri?.let { uri ->
                    launchCrop(uri, CropImageActivity.TARGET_AVATAR)
                }
            }
        }

    private val coverPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                @Suppress("DEPRECATION")
                val medias =
                    result.data?.getParcelableArrayListExtra<MediaItem>(
                        MediaPickerActivity.EXTRA_RESULT_MEDIAS
                    )
                medias?.firstOrNull()?.uri?.let { uri ->
                    launchCrop(uri, CropImageActivity.TARGET_COVER)
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars =
                insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        initView()
        setupListener()
        observerData()
        observerUiState()
        setupFragmentResultListeners()
    }

    private fun setupFragmentResultListeners() {
        childFragmentManager.setFragmentResultListener(
            "refresh_key",
            viewLifecycleOwner
        ) { _, bundle ->
            val isUpdated = bundle.getBoolean("bundle_is_updated", false)
            if (isUpdated) {
                viewModel.refreshProfile(true)
            }
        }

        childFragmentManager.setFragmentResultListener(
            "post_deleted_key",
            viewLifecycleOwner
        ) { _, bundle ->
            val deletedPostId = bundle.getString("bundle_post_id")
            if (deletedPostId != null) {
                viewModel.removePostById(deletedPostId)
            }
        }
    }

    private fun observerUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is UpdateUserState.Loading -> {
                            if (state.style == "avatar") {
                                requireContext().toast(getString(R.string.updating_avatar))
                            } else if (state.style == "cover") {
                                requireContext().toast(getString(R.string.updating_cover))
                            }
                        }

                        is UpdateUserState.Success -> {
                            requireContext().toast(getString(R.string.update_user_success))
                            viewModel.resetState()
                        }

                        is UpdateUserState.Error -> {
                            requireContext().toast(state.message)
                            Log.e("ProfileFragment", "Update Error: ${state.message}")
                            viewModel.resetState()
                        }

                        is UpdateUserState.Idle -> {}
                    }
                }
            }
        }
    }

    private fun launchCrop(sourceUri: Uri, target: String) {
        cropTarget = target
        val intent = Intent(requireContext(), CropImageActivity::class.java).apply {
            putExtra(CropImageActivity.EXTRA_SOURCE_URI, sourceUri)
            putExtra(CropImageActivity.EXTRA_TARGET_TYPE, target)
        }
        cropLauncher.launch(intent)
    }

    private fun initView() {
        // init adapter
        historyPostAdapter = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                val bottomSheet = CommentBottomSheetFragment
                    .newInstance(post.id, post.userId)
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
            onProfileClick = { user ->
                binding.dataContainer.smoothScrollTo(0, 0)
            },
            onMoreOption = { post ->
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(post)
                bottomSheet.show(childFragmentManager, "PostOptionsBottomSheet")
            },
            onTaggedPetClick = { pet ->
                PetProfileActivity.start(requireContext(), pet)
            }
        )
        petProfileAdapter = PetProfileAdapter(
            isOwner = true,
            onClick = { pet ->
                PetProfileActivity.start(requireContext(), pet)
            },
            onAddClick = {
                val intent = Intent(requireContext(), CreateEditPetActivity::class.java)
                createPetLauncher.launch(intent)
            }
        )
        binding.rvPets.apply {
            adapter = petProfileAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        binding.rvPosts.apply {
            adapter = historyPostAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }

        binding.dataContainer.setOnScrollChangeListener { _, _, _, _, _ ->
            checkVideoVisibility()
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
                bindUserToUI(user)
            }
        }

        viewModel.pets.observe(viewLifecycleOwner) {
            petProfileAdapter.submitPets(it)
        }

        viewModel.posts.observe(viewLifecycleOwner) {
            historyPostAdapter.submitPostData(it, false)
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

    @SuppressLint("SetTextI18n")
    private fun bindUserToUI(user: User) {
        user.let {
            binding.tvFullName.text = if (!it.fullName.isNullOrEmpty()) it.fullName
            else requireContext().getString(R.string.petber_user)

            binding.tvFriendsCount.text = it.friendsCount.formatSocialCount()
            binding.tvPetFollowingCount.text = it.petFollowingCount.formatSocialCount()
            binding.tvFollowerCount.text = it.followerCount.formatSocialCount()
            binding.tvFollowingCount.text = it.followingCount.formatSocialCount()

            binding.tvUserName.text = "@${it.username}"
            binding.tvBio.text = if (!it.bio.isNullOrEmpty()) it.bio
            else requireContext().getString(R.string.add_bio)

            binding.tvAddress.text = if (!it.address.isNullOrEmpty()) it.address
            else requireContext().getString(R.string.add_address)

            binding.tvPhone.text = if (!it.phone.isNullOrEmpty()) it.phone
            else requireContext().getString(R.string.add_phone_number)

            binding.tvGender.text = if (!it.gender.isNullOrEmpty()) it.gender
            else requireContext().getString(R.string.add_gender)

            binding.tvBirthday.text =
                if (!it.birthday.isNullOrEmpty()) DateTimeUtils.formatToDisplay(it.birthday)
                else requireContext().getString(R.string.add_birthday)

            binding.tvHobbies.text = if (!it.hobbies.isNullOrEmpty()) it.hobbies
            else requireContext().getString(R.string.add_hobbies)

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
            btnMore.setOnClickListener {
                startActivity(
                    Intent(requireContext(), SettingsActivity::class.java)
                )
            }

            btnAddStory.setOnClickListener {
                handleAddStoryClick()
            }

            btnEditProfile.setOnClickListener {
                startActivity(
                    Intent(requireContext(), EditProfileActivity::class.java).apply {
                        putExtra(EditProfileActivity.USER_DATA, userData)
                    }
                )
            }

            btnEditInformation.setOnClickListener {
                startActivity(
                    Intent(requireContext(), EditProfileActivity::class.java).apply {
                        putExtra(EditProfileActivity.USER_DATA, userData)
                    }
                )
            }

            root.setOnRefreshListener { viewModel.refreshProfile(true) }

            imgCover.setOnClickListener {
                requireContext().showCoverOptionDialog(
                    onViewCover = {
                        val currentCoverUrl = viewModel.user.value?.coverUrl
                        if (!currentCoverUrl.isNullOrEmpty()) {
                            val mediaItem = MediaItem(
                                uri = currentCoverUrl.toUri(),
                                isVideo = false,
                                duration = 0L
                            )
                            startActivity(
                                Intent(requireContext(), MediaPreviewActivity::class.java).apply {
                                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                                }
                            )
                        } else {
                            requireContext().toast(getString(R.string.no_cover_found))
                        }
                    },
                    onChooseCover = {
                        if (PermissionUtils.hasMediaPermissions(requireContext())) {
                            openMediaPickerForCover()
                        } else {
                            pendingMediaAction = "cover"
                            val denied = PermissionUtils.getDeniedPermissions(requireContext())
                            permissionLauncher.launch(denied)
                        }
                    }
                )
            }

            avatar.setOnClickListener {
                requireContext().showAvatarOptionDialog(
                    onViewAvatar = {
                        val currentAvatarUrl = viewModel.user.value?.avatarUrl
                        if (!currentAvatarUrl.isNullOrEmpty()) {
                            val mediaItem = MediaItem(
                                uri = currentAvatarUrl.toUri(),
                                isVideo = false,
                                duration = 0L
                            )
                            startActivity(
                                Intent(requireContext(), MediaPreviewActivity::class.java).apply {
                                    putExtra(MediaPreviewActivity.EXTRA_MEDIA, mediaItem)
                                }
                            )
                        } else {
                            requireContext().toast(getString(R.string.no_avatar_found))
                        }
                    },
                    onChooseAvatar = {
                        if (PermissionUtils.hasMediaPermissions(requireContext())) {
                            openMediaPickerForAvatar()
                        } else {
                            pendingMediaAction = "avatar"
                            val denied = PermissionUtils.getDeniedPermissions(requireContext())
                            permissionLauncher.launch(denied)
                        }
                    }
                )
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

            itemPetFollowing.setOnClickListener { openFollowScreen(0) }
            itemFollowers.setOnClickListener { openFollowScreen(1) }
            itemFollowing.setOnClickListener { openFollowScreen(2) }
            itemFriends.setOnClickListener { openFollowScreen(3) }
        }
    }

    private fun openFollowScreen(extraTab: Int) {
        val user = userData ?: return
        startActivity(
            ViewFollowsActivity.newIntent(
                requireContext(),
                user.id,
                extraTab,
                user.username.toString()
            )
        )
    }

    private fun handleAddStoryClick() {
        if (PermissionUtils.hasMediaPermissions(requireContext())) {
            openMediaPicker()
        } else {
            pendingMediaAction = "story"
            val denied = PermissionUtils.getDeniedPermissions(requireContext())
            permissionLauncher.launch(denied)
        }
    }

    private fun openMediaPickerForAvatar() {
        val intent = Intent(requireContext(), MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_SINGLE)
            putExtra(MediaPickerActivity.EXTRA_MEDIA_KIND, MediaPickerActivity.MEDIA_KIND_IMAGES)
        }
        avatarPickerLauncher.launch(intent)
    }

    private fun openMediaPickerForCover() {
        val intent = Intent(requireContext(), MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_SINGLE)
            putExtra(MediaPickerActivity.EXTRA_MEDIA_KIND, MediaPickerActivity.MEDIA_KIND_IMAGES)
        }
        coverPickerLauncher.launch(intent)
    }

    private fun openMediaPicker() {
        val intent = Intent(requireContext(), MediaPickerActivity::class.java).apply {
            putExtra(MediaPickerActivity.EXTRA_MODE, MediaPickerActivity.MODE_SINGLE)
            putExtra(MediaPickerActivity.EXTRA_MEDIA_KIND, MediaPickerActivity.MEDIA_KIND_ALL)
        }
        storyPickerLauncher.launch(intent)
    }

    private fun openCreateStory(uri: Uri) {
        val intent = Intent(requireContext(), CreateStoryActivity::class.java)
        intent.putExtra("media_uri", uri)
        startActivity(intent)
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