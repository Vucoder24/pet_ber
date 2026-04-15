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
import com.nvv.petber.databinding.FragmentPetSearchBinding
import com.nvv.petber.ui.adapter.SearchPetResultAdapter
import com.nvv.petber.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PetSearchFragment : Fragment() {
    private var _binding: FragmentPetSearchBinding? = null
    private val binding get() = _binding!!
    private val sharedViewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private lateinit var adapter: SearchPetResultAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentPetSearchBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = SearchPetResultAdapter { pet ->
            // click pet item
        }
        binding.rvResults.adapter = adapter
        // observe search results
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED){
                sharedViewModel.pets.collect { list ->
                    adapter.submitList(list)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}