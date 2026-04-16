package com.nvv.petber.ui.fragment.search.tab_fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentHashtagSearchBinding
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.utils.ext.addFeedScrollListener
import com.nvv.petber.viewmodel.HomeViewModel
import com.nvv.petber.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HashtagSearchFragment : Fragment() {
    private var _binding: FragmentHashtagSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val homeViewModel: HomeViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapterResults: PostAdapter
    @Inject
    lateinit var exoPlayer: ExoPlayer
    private var scrollListener: RecyclerView.OnScrollListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentHashtagSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapterResults = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post ->
                homeViewModel.toggleLike(post)
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
                homeViewModel.incrementShareCount(post.id)
            },
            onProfileClick = { post ->
                UserProfileActivity.start(requireContext(), post.userId)
            },
            onMoreOption = {
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(it)
                bottomSheet.show(childFragmentManager, "PostOptionsBottomSheet")
            }
        )
        val linearLayoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.apply {
            layoutManager = linearLayoutManager
            setHasFixedSize(false)
            adapter = adapterResults
            scrollListener = addFeedScrollListener(
                layoutManager = linearLayoutManager,
                headerCount = 1,
                preloadCount = 3,
                onPauseItem = { viewHolder ->
                    if (viewHolder is PostAdapter.PostViewHolder) {
                        viewHolder.pausePlayer()
                    }
                },
                onPreloadItem = { index ->
                    val currentList = adapterResults.currentList
                    val item = currentList.getOrNull(index)
                    if (item is PostAdapter.PostItem.Data) {
                        item.post.postMedia?.firstOrNull()?.mediaUrl?.let { url ->
                            com.bumptech.glide.Glide.with(requireContext())
                                .load(url)
                                .preload()
                        }
                    }
                }
            )
        }
        scrollListener?.let { binding.rvResults.addOnScrollListener(it) }
        // observe search results'
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.posts.collect { list ->
                    adapterResults.submitPostData(list, false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapterResults.isInitialized) {
            adapterResults.pauseAllPlayers()
        }

        binding.rvResults.adapter = null

        scrollListener?.let { binding.rvResults.removeOnScrollListener(it) }
        _binding = null
    }
}