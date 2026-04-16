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
import com.nvv.petber.databinding.FragmentUserSearchBinding
import com.nvv.petber.ui.activity.MainActivity
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.SearchUserResultAdapter
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UserSearchFragment : Fragment() {
    private var _binding: FragmentUserSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: SearchUserResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentUserSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAndSetupListener()
        observeSearchResults()

    }

    private fun observeSearchResults() {
        // observe search results
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorEvent.collect { errorMessage ->
                    requireContext().toast(errorMessage)
                }
            }
        }
    }

    private fun initAndSetupListener() {
        adapter = SearchUserResultAdapter(
            onClick = { userSearch ->
                if (userSearch.user.id == SharePrefUtils.getCurrentUserId(requireContext())) {
                    (requireActivity() as MainActivity?)?.selectProfileTab()
                } else {
                    UserProfileActivity.start(requireContext(), userSearch.user.id)
                }
            },
            onFollowClick = { userSearch ->
                viewModel.toggleFollow(
                    userSearch.user,
                    userSearch.isFollowing,
                    requireContext()
                )
            }
        )
        binding.rvResults.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}