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
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentHashtagSearchBinding
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.viewmodel.HomeViewModel
import com.nvv.petber.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HashtagSearchFragment : Fragment() {
    private var _binding: FragmentHashtagSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val homeViewModel: HomeViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: PostAdapter

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
        adapter = PostAdapter(
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
                // Handle profile click
            },
            onLoadMore = {
            },
            onMoreOption = {
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(it)
                bottomSheet.show(childFragmentManager, "PostOptionsBottomSheet")
            }
        )
        binding.rvResults.adapter = adapter
        // observe search results'
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.posts.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::adapter.isInitialized) {
            adapter.pauseAllPlayers()
        }
        _binding = null
    }
}