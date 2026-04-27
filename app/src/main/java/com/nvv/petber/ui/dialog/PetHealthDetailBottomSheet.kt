package com.nvv.petber.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.data.model.PetHealthLog
import com.nvv.petber.databinding.DialogPetHealthDetailBinding
import kotlinx.datetime.LocalDate

class PetHealthDetailBottomSheet(
    private val log: PetHealthLog
) : BottomSheetDialogFragment() {

    private var _binding: DialogPetHealthDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPetHealthDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val displayDate = try {
            val date = LocalDate.parse(log.recordedAt)
            "${date.monthNumber}/${date.year}"
        } catch (_: Exception) { log.recordedAt }

        binding.tvDialogTitle.text = getString(R.string.health_detail_title, displayDate)
        binding.tvDetailWeight.text = log.weight?.let { "$it kg" } ?: getString(R.string.value_na)
        binding.tvDetailNeutered.text = when (log.isNeutered) {
            true  -> getString(R.string.value_neutered_yes)
            false -> getString(R.string.value_neutered_no)
            null  -> getString(R.string.value_na)
        }
        binding.tvDetailBodyCondition.text = log.bodyCondition ?: getString(R.string.value_na)
        binding.tvDetailClinicalStatus.text = log.clinicalStatus ?: getString(R.string.value_na)
        binding.tvDetailActivity.text = log.activityAndMentalState ?: getString(R.string.value_na)
        binding.tvDetailPreventive.text = log.preventiveStatus ?: getString(R.string.value_na)

        binding.tvRecordedAt.text = getString(R.string.recorded_at, log.recordedAt)

        binding.btnClose.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "PetHealthDetailBottomSheet"
        fun newInstance(log: PetHealthLog) = PetHealthDetailBottomSheet(log)
    }
}