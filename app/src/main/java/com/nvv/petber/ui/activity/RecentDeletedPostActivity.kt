package com.nvv.petber.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.databinding.ActivityRecentDeletedPostBinding
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.TrashOptionsBottomSheet
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.RecentDeletedViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class RecentDeletedPostActivity : BaseActivity() {
    private lateinit var binding: ActivityRecentDeletedPostBinding
    private val viewModel: RecentDeletedViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter
    @Inject
    lateinit var exoPlayer: ExoPlayer
    private var userId: String = ""

    companion object{
        const val EXTRA_USER_ID = "EXTRA_USER_ID"
        fun start(context: Context, userId: String) {
            val intent = Intent(context, RecentDeletedPostActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRecentDeletedPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        if (userId.isEmpty()){
            toast(R.string.error_get_arg)
            finish()
        }

        setupUI()
        observeViewModel()
        viewModel.init(userId)
    }
    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        binding.root.setOnRefreshListener {
            viewModel.refresh()
        }

        postAdapter = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                val bottomSheet = CommentBottomSheetFragment
                    .newInstance(post.id, post.userId)
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
            onProfileClick = { user ->
                UserProfileActivity.start(this, user.id)
            },
            onMoreOption = { post ->
                val bottomSheet = TrashOptionsBottomSheet(
                    post = post,
                    onActionSuccess = {
                        viewModel.refresh()
                    }
                )
                bottomSheet.show(supportFragmentManager, "TrashOptions")
            },
            onTaggedPetClick = { pet ->
                PetProfileActivity.start(this, pet)
            }
        )

        binding.rvRecentDeleted.apply {
            layoutManager = LinearLayoutManager(this@RecentDeletedPostActivity)
            adapter = postAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0 && !binding.rvRecentDeleted.canScrollVertically(1)) {
                        viewModel.loadMore()
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(this) { posts ->
            postAdapter.submitPostData(
                posts,
                viewModel.isLoadingMore.value == true,
                false
            )
            binding.tvEmpty.visibility =
                if (posts.isEmpty() && viewModel.isLoading.value != true) View.VISIBLE else View.GONE
            binding.root.isRefreshing = false
        }

        viewModel.isLoading.observe(this) { loading ->
            if (!loading) binding.root.isRefreshing = false
            binding.progressInitial.visibility = if (loading) View.VISIBLE else View.GONE
            binding.rvRecentDeleted.visibility =
                if (loading && viewModel.posts.value.isNullOrEmpty()) View.GONE else View.VISIBLE

            if (loading) binding.tvEmpty.gone()
        }
        viewModel.errorState.observe(this) { message ->
            message?.let {
                toast(it)
                viewModel.clearError()
            }
        }
        viewModel.successState.observe(this) { message ->
            message?.let {
                toast(it)
                viewModel.clearSuccess()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }

}