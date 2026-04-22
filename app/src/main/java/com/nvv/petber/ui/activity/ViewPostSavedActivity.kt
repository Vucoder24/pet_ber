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
import com.nvv.petber.databinding.ActivityViewPostSavedBinding
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.base.BaseActivity
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.SavedPostOptionsBottomSheet
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.SavedPostsViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ViewPostSavedActivity : BaseActivity() {

    private lateinit var binding: ActivityViewPostSavedBinding
    private val viewModel: SavedPostsViewModel by viewModels()

    private lateinit var postAdapter: PostAdapter
    @Inject lateinit var exoPlayer: ExoPlayer

    private lateinit var userId: String

    companion object {
        private const val EXTRA_USER_ID = "extra_user_id"

        fun start(context: Context, userId: String) {
            val intent = Intent(context, ViewPostSavedActivity::class.java).apply {
                putExtra(EXTRA_USER_ID, userId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityViewPostSavedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        userId = intent.getStringExtra(EXTRA_USER_ID) ?: ""
        if (userId.isEmpty()){
            toast(getString(R.string.error_get_arg))
            finish()
        }
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        binding.root.setOnRefreshListener {
            viewModel.refresh()
        }
        binding.btnBack.setOnClickListener { finish() }

        viewModel.init(userId)
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
                val bottomSheet = SavedPostOptionsBottomSheet {
                    viewModel.removePostLocal(post)
                    viewModel.unsavePost(post)
                }

                bottomSheet.show(supportFragmentManager, "SavedPostOptions")
            },
            onTaggedPetClick = { pet ->
                PetProfileActivity.start(this, pet)
            }
        )

        binding.rvSavedPosts.apply {
            layoutManager = LinearLayoutManager(this@ViewPostSavedActivity)
            adapter = postAdapter
            itemAnimator = null

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy <= 0) return

                    val lm = layoutManager as LinearLayoutManager
                    val visibleItemCount = lm.childCount
                    val totalItemCount = lm.itemCount
                    val firstVisibleItemPosition = lm.findFirstVisibleItemPosition()

                    val shouldLoadMore =
                        visibleItemCount + firstVisibleItemPosition >= totalItemCount - 3

                    if (shouldLoadMore &&
                        viewModel.isLoadingMore.value == false &&
                        viewModel.isLastPage.value == false
                    ) {
                        viewModel.loadMore()
                    }
                }
            })
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(this) { posts ->
            renderPosts(posts)
            binding.tvEmpty.visibility =
                if (posts.isEmpty() && viewModel.isLoading.value != true) View.VISIBLE else View.GONE
        }

        viewModel.isLoading.observe(this) { loading ->
            binding.root.isRefreshing = loading
            binding.progressInitial.visibility = if (loading) View.VISIBLE else View.GONE
            binding.rvSavedPosts.visibility =
                if (loading && viewModel.posts.value.isNullOrEmpty()) View.GONE else View.VISIBLE
            updateEmptyState()
        }

        viewModel.isLoadingMore.observe(this) { loadingMore ->
            renderPosts(viewModel.posts.value.orEmpty(), loadingMore)
        }
    }

    private fun updateEmptyState() {
        val isLoading = viewModel.isLoading.value == true
        val isEmpty = viewModel.posts.value.isNullOrEmpty()

        binding.tvEmpty.visibility = if (!isLoading && isEmpty) View.VISIBLE else View.GONE
    }

    private fun renderPosts(posts: List<com.nvv.petber.data.model.Post>, isLoadingMore: Boolean = false) {
        postAdapter.submitPostData(
            list = posts,
            isLoadingMore = isLoadingMore,
            showCreatePost = false
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer.release()
    }
}