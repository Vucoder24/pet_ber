package com.nvv.petber.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.data.model.CommentUI
import com.nvv.petber.databinding.FragmentBottomSheetCommentBinding
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.CommentAdapter
import com.nvv.petber.utils.SharePrefUtils
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast
import com.nvv.petber.utils.ext.visible
import com.nvv.petber.viewmodel.CommentViewModel
import com.vanniktech.emoji.EmojiPopup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommentBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBottomSheetCommentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CommentViewModel by viewModels()
    private lateinit var adapter: CommentAdapter
    private var postId: String = ""
    private lateinit var currentUserId: String
    private var postAuthorId: String = ""
    private lateinit var emojiPopup: EmojiPopup

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBottomSheetCommentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postId = arguments?.getString(POST_ID) ?: return
        postAuthorId = arguments?.getString(POST_AUTHOR_ID) ?: ""
        currentUserId = SharePrefUtils.getCurrentUserId(requireContext())

        setupRecyclerView()
        observeData()
        setupActions()

        viewModel.loadComments(postId, currentUserId)
    }

    private fun setupRecyclerView() {
        adapter = CommentAdapter(
            postAuthorId = postAuthorId,
            onReplyClick = { uiModel ->
                handleReply(uiModel)
            },
            onLikeClick = { uiModel ->
                viewModel.toggleLike(uiModel.comment, currentUserId)
            },
            onToggleReplies = { uiModel ->
                viewModel.toggleExpand(uiModel.comment.id)
            },
            onLongClick = { uiModel ->
                showActionMenu(uiModel)
            },
            onProfileClick = { uModel ->
                UserProfileActivity.start(requireContext(), uModel.comment.userId)
            }
        )
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvComments.adapter = adapter
    }
    private fun handleReply(uiModel: CommentUI) {
        viewModel.setReplyingTo(uiModel.comment)
        binding.etComment.post {
            binding.etComment.requestFocus()
            showKeyboard()
        }
    }

    private fun showActionMenu(uiModel: CommentUI) {
        val isMine = uiModel.comment.userId == currentUserId
        val actionSheet = CommentActionBottomSheetFragment(
            commentUI = uiModel,
            isMine = isMine,
            onReply = { handleReply(uiModel) },
            onDelete = {
                viewModel.deleteComment(uiModel.comment.id)
            }
        )
        actionSheet.show(childFragmentManager, "CommentAction")
    }

    private fun showKeyboard() {
        dialog?.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        )
    }

    private fun setupActions() {
        emojiPopup = EmojiPopup(
            binding.root,
            binding.etComment,
            onEmojiPopupShownListener = {
                binding.btnEmoji.setImageResource(R.drawable.ic_keyboard)
            },
            onEmojiPopupDismissListener = {
                binding.btnEmoji.setImageResource(R.drawable.ic_emoji)
            },
            onEmojiBackspaceClickListener = {
                //
            }
        )

        binding.btnSend.isEnabled = false
        binding.btnSend.isClickable = false
        binding.btnSend.alpha = 0.5f

        binding.etComment.doAfterTextChanged { text ->
            val content = text?.toString()?.trim()
            val hasContent = !content.isNullOrEmpty()

            binding.btnSend.isEnabled = hasContent
            binding.btnSend.isClickable = hasContent
            binding.btnSend.alpha = if (hasContent) 1.0f else 0.5f
        }

        binding.btnSend.setOnClickListener {
            val content = binding.etComment.text.toString().trim()
            if (content.isNotEmpty()) {
                viewModel.postComment(postId, currentUserId, content)
                binding.etComment.text?.clear()
            }
        }

        binding.btnEmoji.setOnClickListener {
            emojiPopup.toggle()
        }

        binding.ivCancelReply.setOnClickListener {
            viewModel.setReplyingTo(null)
            binding.etComment.text?.clear()
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED){
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.flatComments)

                    binding.pbLoading.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    when {
                        state.isLoading -> {
                            binding.tvMessage.gone()
                            binding.rvComments.gone()
                        }
                        state.error != null && state.flatComments.isEmpty() -> {
                            binding.tvMessage.visible()
                            binding.rvComments.gone()
                            binding.tvMessage.setText(R.string.error_load_comments_failed)
                        }
                        state.flatComments.isEmpty() -> {
                            binding.tvMessage.visible()
                            binding.rvComments.gone()
                            binding.tvMessage.setText(R.string.no_comments_yet)
                        }
                        else -> {
                            binding.tvMessage.gone()
                            binding.rvComments.visible()
                        }
                    }

                    if (state.isSending) {
                        binding.btnSend.gone()
                        binding.progressSend.visible()
                    } else {
                        binding.btnSend.visible()
                        binding.progressSend.gone()
                    }
                    // Reply To bar status
                    if (state.replyingTo != null) {
                        binding.layoutReplyingTo.visible()
                        binding.tvReplyingToName.text =
                            getString(
                                R.string.replying_to_user,
                                state.replyingTo.users?.fullName ?: getString(R.string.petber_user)
                            )
                    } else {
                        binding.layoutReplyingTo.gone()
                    }

                    state.error?.let {
                        when (it) {
                            "DELETE_FAILED" -> {
                                requireContext().toast(R.string.error_action)
                                viewModel.clearError()
                            }
                            else -> {
                                requireContext().toast(R.string.error_add_cmt_failed)
                                viewModel.clearError()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        emojiPopup.dismiss()
        _binding = null
    }

    companion object {
        fun newInstance(postId: String, postAuthorId: String): CommentBottomSheetFragment {
            val args = Bundle().apply {
                putString(POST_ID, postId)
                putString(POST_AUTHOR_ID, postAuthorId)
            }
            return CommentBottomSheetFragment().apply { arguments = args }
        }

        const val POST_ID = "POST_ID"
        const val POST_AUTHOR_ID = "POST_AUTHOR_ID"
    }
}