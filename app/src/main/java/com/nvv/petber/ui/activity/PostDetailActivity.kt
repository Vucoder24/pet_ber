package com.nvv.petber.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityPostDetailBinding
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.auth.login.LoginActivity
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.PostDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PostDetailActivity : BaseActivity() {
    private lateinit var binding: ActivityPostDetailBinding
    private val viewModel: PostDetailViewModel by viewModels()
    private lateinit var currentUserId: String
    private lateinit var adapter: PostAdapter

    @Inject
    lateinit var exoPlayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPostDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val postId = intent.getStringExtra(EXTRA_POST_ID)
        currentUserId = SharePrefUtils.getCurrentUserId(this)

        if (postId.isNullOrEmpty()) {
            finish()
            return
        }

        setupRecyclerView()
        observeData()
        setupFragmentResultListener(postId)

        viewModel.loadPostById(postId)
    }

    private fun setupFragmentResultListener(postId: String) {
        supportFragmentManager.setFragmentResultListener("refresh_key", this) { _, bundle ->
            val isUpdated = bundle.getBoolean("bundle_is_updated", false)
            if (isUpdated) {
                viewModel.loadPostById(postId)
            }
        }
    }

    private fun requireLogin(action: () -> Unit) {
        if (currentUserId.isNotEmpty()) {
            action()
        } else {
            navigateToLogin()
        }
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        toast(getString(R.string.please_login))
        finish()
    }

    private fun setupRecyclerView() {
        adapter = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post ->
                requireLogin {
                    viewModel.toggleLike(post)
                }
            },
            onCommentClick = { post ->
                requireLogin {
                    val bottomSheet = CommentBottomSheetFragment
                        .newInstance(post.id, post.userId)
                    bottomSheet.show(supportFragmentManager, "CommentBottomSheet")
                }
            },
            onShareClick = { post ->
                requireLogin {
                    sharePost(post.id)
                }
            },
            onProfileClick = {
                requireLogin {
                    UserProfileActivity.start(this, it.userId)
                }
            },
            onMoreOption = { post ->
                requireLogin {
                    val bottomSheet = PostOptionsBottomSheetFragment.newInstance(post)
                    bottomSheet.show(supportFragmentManager, "PostOptionsBottomSheet")
                }
            },
            onTaggedPetClick = { pet ->
                requireLogin {
                    PetProfileActivity.start(this, pet)
                }
            }
        )

        binding.rvPost.apply {
            layoutManager = LinearLayoutManager(this@PostDetailActivity)
            adapter = this@PostDetailActivity.adapter
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun observeData() {
        viewModel.post.observe(this){
            adapter.submitPostData(
                list = it?.let { listOf(it) } ?: emptyList(),
                isLoadingMore = false
            )
        }
        viewModel.isLoading.observe(this){
                binding.progressBar.visibility = if (it) View.VISIBLE else View.GONE
        }
        viewModel.error.observe(this) {
            toast(R.string.error_fetch_data)
        }
    }

    private fun sharePost(postId: String) {
        val link = "https://project-ilyyx.vercel.app/post/$postId"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, link)
        }
        startActivity(Intent.createChooser(intent, getString(R.string.share_post)))
        viewModel.incrementShareCount()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

    companion object {
        const val EXTRA_POST_ID = "postId"
    }
}