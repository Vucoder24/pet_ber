package com.nvv.petber.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.databinding.FragmentPetMoreBottomSheetBinding
import com.nvv.petber.utils.ext.showConfirmDialog

class PetMoreBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentPetMoreBottomSheetBinding? = null
    private val binding get() = _binding!!

    var onDeleteClick: (() -> Unit)? = null

    companion object {
        fun newInstance() = PetMoreBottomSheetFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPetMoreBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnDeletePet.setOnClickListener {
            dismiss()
            showConfirmDeleteDialog()
        }
    }

    private fun showConfirmDeleteDialog() {
        requireContext().showConfirmDialog(
            title = getString(R.string.confirm_delete_pet),
            message = getString(R.string.dep_confirm_delete_pet),
            positiveButtonText = getString(R.string.delete)
        ) {
            onDeleteClick?.invoke()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}