package com.nvv.petber.ui.fragment.pet_profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.exoplayer.ExoPlayer
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentAllPetPostsBinding
import com.nvv.petber.ui.activity.PetProfileActivity
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.ui.dialog.CommentBottomSheetFragment
import com.nvv.petber.ui.dialog.PostOptionsBottomSheetFragment
import com.nvv.petber.viewmodel.PetProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AllPetPostsFragment : Fragment() {
    private var _binding: FragmentAllPetPostsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PetProfileViewModel by activityViewModels()

    @Inject
    lateinit var exoPlayer: ExoPlayer
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAllPetPostsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeData()
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            exoPlayer = exoPlayer,
            onLikeClick = { post ->
                viewModel.toggleLike(post)
            },
            onCommentClick = { post ->
                val bottomSheet = CommentBottomSheetFragment.newInstance(post.id, post.userId)
                bottomSheet.show(parentFragmentManager, "CommentBottomSheet")
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
                UserProfileActivity.start(requireContext(), it.userId)
            },
            onMoreOption = {
                val bottomSheet = PostOptionsBottomSheetFragment.newInstance(it)
                bottomSheet.show(parentFragmentManager, "PostOptionsBottomSheet")
            },
            onTaggedPetClick = { pet ->
                PetProfileActivity.start(requireContext(), pet)
            }
        )
        binding.rvAllPosts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = postAdapter
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.posts.collectLatest { posts ->
                postAdapter.submitPostData(posts, viewModel.isLoadMore.value)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
