package com.nvv.petber.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.data.model.User
import com.nvv.petber.databinding.ActivityUserProfileBinding
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
class UserProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserProfileBinding
    private val viewModel: UserProfileViewModel by viewModels()

    private lateinit var historyPostAdapter: PostAdapter
    private lateinit var petProfileAdapter: PetProfileAdapter
    private var userData: User? = null
    private lateinit var targetUserId: String

    @Inject
    lateinit var exoPlayer: ExoPlayer

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: String) {
            val intent = Intent(context, UserProfileActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        targetUserId = intent.getStringExtra(EXTRA_USER_ID) ?: return finish()

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }
        setupFragmentResultListener()
        viewModel.loadUserProfile(targetUserId)

        initView()
        setupListener()
        observerData()
    }

    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener("refresh_key", this) { _, bundle ->
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
                bottomSheet.show(supportFragmentManager, "CommentBottomSheet")
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
            onLoadMore = { },
            onMoreOption = { post ->
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(post)
                bottomSheet.show(supportFragmentManager, "PostOptionsBottomSheet")
            }
        )

        // Khởi tạo Adapter với isOwner = false để ẩn nút thêm Pet
        petProfileAdapter = PetProfileAdapter(
            isOwner = false,
            onClick = { pet -> PetProfileActivity.start(this, pet) },
            onAddClick = { }
        )

        binding.rvPets.apply {
            adapter = petProfileAdapter
            layoutManager =
                LinearLayoutManager(this@UserProfileActivity, LinearLayoutManager.HORIZONTAL, false)
        }

        binding.rvPosts.apply {
            adapter = historyPostAdapter
            layoutManager = LinearLayoutManager(this@UserProfileActivity)
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
        viewModel.user.observe(this) { user ->
            user?.let {
                userData = it
                bindUserToUI(it)
            }
        }

        viewModel.pets.observe(this) {
            petProfileAdapter.submitPets(it)
        }

        viewModel.posts.observe(this) { posts ->
            historyPostAdapter.submitList(posts)
        }

        viewModel.isFollowing.observe(this) { isFollowing ->
            updateFollowButtonUI(isFollowing)
        }

        viewModel.isLoading.observe(this) { loading ->
            if (loading && viewModel.user.value == null) {
                showShimmer()
            } else hideShimmer()
        }

        viewModel.isRefreshing.observe(this) { refreshing ->
            binding.root.isRefreshing = refreshing
        }
    }

    private fun updateFollowButtonUI(isFollowing: Boolean) {
        binding.btnFollow.apply {
            if (isFollowing) {
                text = getString(R.string.following)
                setBackgroundColor(
                    ContextCompat.getColor(
                        this@UserProfileActivity,
                        R.color.gray_light
                    )
                )
                setTextColor(ContextCompat.getColor(this@UserProfileActivity, R.color.black))
                setIconTintResource(R.color.black)
                icon = ContextCompat.getDrawable(this@UserProfileActivity, R.drawable.ic_following)
            } else {
                text = getString(R.string.follow)
                setBackgroundColor(ContextCompat.getColor(this@UserProfileActivity, R.color.bg_btn))
                setTextColor(ContextCompat.getColor(this@UserProfileActivity, R.color.white))
                setIconTintResource(R.color.white)
                icon = ContextCompat.getDrawable(this@UserProfileActivity, R.drawable.ic_follow)
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
            // Sự kiện làm mới trang
            root.setOnRefreshListener {
                viewModel.refreshProfile()
            }

            // Nút Follow
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
                            this@UserProfileActivity,
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
                            this@UserProfileActivity,
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
            btnBack.setOnClickListener { finish() }
        }
    }

    private fun handleViewList(userId: String, extraTab: Int) {
        val userName = userData?.username ?: return
        startActivity(ViewFollowsActivity.newIntent(this, userId, extraTab, userName))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::historyPostAdapter.isInitialized) {
            historyPostAdapter.pauseAllPlayers()
        }
        binding.rvPosts.adapter = null
        binding.dataContainer.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }
}