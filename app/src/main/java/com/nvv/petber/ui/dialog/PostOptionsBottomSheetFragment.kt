package com.nvv.petber.ui.dialog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.databinding.LayoutPostOptionsBinding
import com.nvv.petber.ui.activity.CreateEditPostActivity
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.ActionState
import com.nvv.petber.viewmodel.PostOptionsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@AndroidEntryPoint
class PostOptionsBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: LayoutPostOptionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PostOptionsViewModel by viewModels()
    private var currentUserId: String? = null
    private var post: Post? = null

    private val editPostLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == AppCompatActivity.RESULT_OK) {
            val isUpdated = result.data?.getBooleanExtra("is_updated", false) ?: false
            if (isUpdated) {
                parentFragmentManager.setFragmentResult("refresh_key", Bundle().apply {
                    putBoolean("bundle_is_updated", true)
                })
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutPostOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentUserId = SharePrefUtils.getCurrentUserId(requireContext())
        val postJson = arguments?.getString(ARG_POST_JSON) ?: ""
        if (postJson.isNotEmpty()) {
            post = Json.decodeFromString<Post>(postJson)

        }
        if (post == null) {
            requireContext().toast(getString(R.string.error_get_arg))
            dismiss()
            return
        }
        post?.let {
            viewModel.loadInitData(it)
        }
        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        post?.let { p ->
            if (p.userId == currentUserId) {
                binding.btnBlock.gone()
                binding.btnHide.gone()
                binding.btnFollow.gone()
                binding.btnEditPost.visible()
            } else {
                binding.btnBlock.visible()
                binding.btnHide.visible()
                binding.btnFollow.visible()
                binding.btnEditPost.gone()
            }

            binding.tvFollow.text = getString(
                R.string.follow_user, p.users?.fullName ?: R.string.petber_user
            )
            binding.tvBlock.text = getString(
                R.string.block_user, p.users?.fullName ?: R.string.petber_user
            )

            binding.btnSave.setOnClickListener { viewModel.toggleSavePost(p) }

            binding.btnHide.setOnClickListener { viewModel.hidePost(p.id) }

            binding.btnCopyLink.setOnClickListener {
                copyToClipboard("https://project-ilyyx.vercel.app/post/${p.id}")
                viewModel.incrementShareCount(p.id)
                dismiss()
            }

            binding.btnFollow.setOnClickListener {
                viewModel.toggleFollowUser(p)
            }

            binding.btnEditPost.setOnClickListener {
                val intent = Intent(requireContext(), CreateEditPostActivity::class.java).apply {
                    putExtra(CreateEditPostActivity.IS_EDIT_MODE, true)
                    putExtra(CreateEditPostActivity.EXTRA_POST_JSON, Json.encodeToString(p))
                }
                editPostLauncher.launch(intent)
            }


            binding.btnBlock.setOnClickListener { viewModel.blockUser(p.userId) }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loadError.collect { error ->
                    error?.let {
                        requireContext().toast(it)
                        dismiss()
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.actionState.collect { state ->
                    when (state) {
                        is ActionState.Success -> {
                            requireContext().toast(state.message)
                            viewModel.resetState()
                            dismiss()
                        }

                        is ActionState.Error -> {
                            requireContext().toast(state.message)
                            viewModel.resetState()
                            dismiss()
                        }

                        else -> {}
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.pbLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.layoutContent.visibility =
                            if (isLoading) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.isSavedRemote.collect { isSaved ->
                        isSaved?.let { saved ->
                            updateSaveUI(saved)
                        }
                    }
                }
                launch {
                    viewModel.isFollowing.collect { following ->
                        following?.let { isFollowing ->
                            updateFollowUI(isFollowing, post?.users?.fullName)
                        }
                    }
                }
            }
        }
    }

    private fun updateFollowUI(isFollowing: Boolean, userName: String?) {
        val defaultName = userName ?: getString(R.string.petber_user)

        if (isFollowing) {
            binding.tvFollow.text = getString(R.string.cancel_follow_user, defaultName)
            binding.ivFollow.setImageResource(R.drawable.ic_cancel_follow)
        } else {
            binding.tvFollow.text = getString(R.string.follow_user, defaultName)
            binding.ivFollow.setImageResource(R.drawable.ic_followw)
        }
    }

    private fun updateSaveUI(isSaved: Boolean) {
        binding.tvSave.text = getString(
            if (isSaved) R.string.post_unsaved
            else R.string.post_saved
        )

        binding.depTvSave.text =
            getString(
                if (isSaved) R.string.dep_remove_post
                else R.string.dep_save_post
            )

        binding.ivSave.setImageResource(
            if (isSaved) R.drawable.ic_bookmark_2
            else R.drawable.ic_bookmark
        )
    }

    private fun copyToClipboard(text: String) {
        val clipboard =
            requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(getString(R.string.post_link), text)
        clipboard.setPrimaryClip(clip)
            requireContext().toast(getString(R.string.copied_to_clipboard))
    }

    companion object {
        private const val ARG_POST_JSON = "arg_post_json"

        fun newInstance(post: Post): PostOptionsBottomSheetFragment {
            return PostOptionsBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_POST_JSON, Json.encodeToString(post))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}