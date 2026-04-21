package com.nvv.petber.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.databinding.LayoutPostOptionsBinding
import com.nvv.petber.utils.ext.gone
import com.nvv.petber.utils.ext.visible

class SavedPostOptionsBottomSheet(
    private val onUnsaveClick: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: LayoutPostOptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutPostOptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvSave.text = getString(R.string.post_unsaved)
        binding.depTvSave.text =
            getString(R.string.dep_remove_post)
        binding.ivSave.setImageResource(R.drawable.ic_bookmark_2)

        binding.pbLoading.gone()
        binding.btnFollow.gone()
        binding.btnHide.gone()
        binding.btnBlock.gone()
        binding.btnEditPost.gone()
        binding.btnCopyLink.gone()

        binding.btnSave.visible()

        binding.btnSave.setOnClickListener {
            onUnsaveClick.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}