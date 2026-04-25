package com.nvv.petber.ui.dialog

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.data.model.CommentUI
import com.nvv.petber.databinding.ViewBottomSheetCommentActionBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.toast

class CommentActionBottomSheetFragment(
    private val commentUI: CommentUI,
    private val isMine: Boolean,
    private val onReply: () -> Unit,
    private val onDelete: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: ViewBottomSheetCommentActionBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ViewBottomSheetCommentActionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isMine) {
            binding.btnDelete.gone()
        }

        binding.btnReply.setOnClickListener {
            onReply()
            dismiss()
        }

        binding.btnCopy.setOnClickListener {
            val clipboard =
                requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = android.content.ClipData.newPlainText("Comment", commentUI.comment.content)
            clipboard.setPrimaryClip(clip)
            requireContext().toast(R.string.copied_to_clipboard)
            dismiss()
        }

        binding.btnDelete.setOnClickListener {
            onDelete()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}