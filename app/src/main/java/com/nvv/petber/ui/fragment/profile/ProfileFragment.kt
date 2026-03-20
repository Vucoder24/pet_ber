package com.nvv.petber.ui.fragment.profile

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentProfileBinding
import com.nvv.petber.ui.adapter.PetProfileAdapter
import com.nvv.petber.ui.adapter.PostAdapter
import com.nvv.petber.utils.DateTimeUtils
import com.nvv.petber.utils.loadAvatar
import com.nvv.petber.utils.loadImage
import com.nvv.petber.viewmodel.ProfileViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var historyPostAdapter: PostAdapter
    private lateinit var petProfileAdapter: PetProfileAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentProfileBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setupListener()
        observerData()
    }

    private fun initView() {
        // init adapter
        historyPostAdapter = PostAdapter(
            onLikeClick = { post ->

            },
            onCommentClick = { post ->

            },
            onShareClick = { post ->

            },
            onProfileClick = { user ->

            },
            onLoadMore = { }
        )
        petProfileAdapter = PetProfileAdapter(
            onClick = { pet ->

            },
            onAddClick = {

            }
        )
        binding.rvPets.apply {
            adapter = petProfileAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
        binding.rvPosts.apply {
            adapter = historyPostAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observerData() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvFullName.text = if (!it.fullName.isNullOrEmpty()) it.fullName
                else requireContext().getString(R.string.petber_user)

                binding.tvUserName.text = it.username
                binding.tvBio.text = if (!it.bio.isNullOrEmpty()) it.bio
                else requireContext().getString(R.string.add_bio)

                binding.tvAddress.text = if (!it.address.isNullOrEmpty()) it.address
                else requireContext().getString(R.string.add_address)

                binding.tvPhone.text = if (!it.phone.isNullOrEmpty()) it.phone
                else requireContext().getString(R.string.add_phone_number)

                binding.tvGender.text = if (!it.gender.isNullOrEmpty()) it.gender
                else requireContext().getString(R.string.add_gender)

                binding.tvBirthday.text =
                    if (!it.birthday.isNullOrEmpty()) DateTimeUtils.formatToDisplay(it.birthday)
                    else requireContext().getString(R.string.add_birthday)

                binding.tvHobbies.text = if (!it.hobbies.isNullOrEmpty()) it.hobbies
                else requireContext().getString(R.string.add_hobbies)

                binding.avatar.loadImage(it.coverUrl)
                binding.avatar.loadAvatar(it.avatarUrl)
            }
        }

        viewModel.pets.observe(viewLifecycleOwner) {
            Log.d("ProfileFragment", "pets loaded: $it")
            petProfileAdapter.submitList(it)
        }

        viewModel.posts.observe(viewLifecycleOwner) {
            Log.d("ProfileFragment", "Posts loaded: $it")
            historyPostAdapter.submitList(it)
        }

        viewModel.userStats.observe(viewLifecycleOwner) {
            binding.tvStats.text = it.postCount.toString() + requireContext().getString(
                R.string.posts
            ) + it.followerCount.toString() + requireContext().getString(
                R.string.followers
            ) + it.followingCount.toString() + requireContext().getString(R.string.following)
        }
    }

    private fun setupListener() {
        binding.apply {
            btnMore.setOnClickListener {

            }

            btnFollow.setOnClickListener {

            }

            btnMessage.setOnClickListener {

            }

            btnEditInformation.setOnClickListener {

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}