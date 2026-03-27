package com.nvv.petber.ui.fragment.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.tabs.TabLayoutMediator
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentSearchBinding
import com.nvv.petber.ui.adapter.SearchPagerAdapter
import com.nvv.petber.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SearchViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTabs()
        setupSearch()
    }

    private fun setupTabs() {
        val pagerAdapter = SearchPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when(position) {
                0 -> getString(R.string.users)
                1 -> getString(R.string.pets)
                else -> getString(R.string.hashtags)
            }
        }.attach()
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.onSearchChanged(text.toString().trim())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}