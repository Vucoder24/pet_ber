package com.nvv.petber.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.data.model.Post
import com.nvv.petber.databinding.LayoutTrashOptionsBinding
import com.nvv.petber.utils.ext.showConfirmDialog
import com.nvv.petber.viewmodel.RecentDeletedViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrashOptionsBottomSheet(
    private val post: Post,
    private val onActionSuccess: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutTrashOptionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: RecentDeletedViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutTrashOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pbLoading.visibility = View.GONE

        binding.btnRestorePost.setOnClickListener {
            viewModel.restorePost(post)
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            requireContext().showConfirmDialog(
                title = getString(R.string.confirm_permanently_delete_title),
                message = getString(R.string.confirm_permanently_delete_msg),
                positiveButtonText = getString(R.string.delete)
            ) {
                viewModel.hardDeletePost(post)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}