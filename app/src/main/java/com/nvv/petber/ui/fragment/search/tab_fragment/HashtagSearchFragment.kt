package com.nvv.petber.ui.fragment.search.tab_fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.nvv.petber.databinding.FragmentHashtagSearchBinding
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.viewmodel.SearchViewModel
import kotlinx.coroutines.launch


class HashtagSearchFragment : Fragment() {
    private var _binding: FragmentHashtagSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
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
                // Handle like click
            },
            onCommentClick = { post ->
                // Handle comment click
            },
            onShareClick = { post ->
                // Handle share click
            },
            onProfileClick = { post ->
                // Handle profile click
            },
            onLoadMore = {
            },
            onSaveClick = {

            }
        )
        binding.rvResults.adapter = adapter
        // observe search results
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.posts.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}