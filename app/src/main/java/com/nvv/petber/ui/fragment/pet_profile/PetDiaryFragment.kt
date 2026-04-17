package com.nvv.petber.ui.fragment.pet_profile

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.databinding.FragmentPetDiaryBinding
import com.nvv.petber.ui.activity.PostDetailActivity
import com.nvv.petber.ui.adapter.PetDiaryAdapter
import com.nvv.petber.viewmodel.PetProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PetDiaryFragment : Fragment() {
    private var _binding: FragmentPetDiaryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PetProfileViewModel by activityViewModels()
    private lateinit var diaryAdapter: PetDiaryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPetDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        diaryAdapter = PetDiaryAdapter(
            onViewPostClick = { post ->
                val intent = Intent(requireContext(), PostDetailActivity::class.java).apply {
                    putExtra(PostDetailActivity.EXTRA_POST_ID, post.id)
                }
                startActivity(intent)
            }
        )
        binding.rvDailyPet.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.diaryPosts.collectLatest { diaryList ->
                Log.d("PetDiaryFragment", "Received diary posts: ${diaryList.size} months")
                diaryAdapter.submitList(diaryList)
            }
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}