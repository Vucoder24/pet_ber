package com.nvv.petber.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.R
import com.nvv.petber.data.model.FilterType
import com.nvv.petber.data.model.SearchFilter
import com.nvv.petber.databinding.LayoutSearchFilterBinding
import com.nvv.petber.viewmodel.SearchViewModel
import kotlinx.coroutines.launch

class SearchFilterBottomSheet(
    private val initialFilter: SearchFilter,
    private val onApply: (SearchFilter) -> Unit,
) : BottomSheetDialogFragment() {

    private var _binding: LayoutSearchFilterBinding? = null
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutSearchFilterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeSpeciesData()

        binding.btnApply.setOnClickListener {
            onApply(collectFilterData())
            dismiss()
        }
    }

    private fun observeSpeciesData() {
        if (initialFilter.type != FilterType.PET) return

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.isSpeciesLoading.collect { isLoading ->
                        binding.pbSpecies.visibility = if (isLoading) View.VISIBLE else View.GONE
                        binding.autoCompleteSpecies.isEnabled = !isLoading
                        binding.btnApply.isEnabled = !isLoading
                    }
                }

                launch {
                    viewModel.speciesList.collect { list ->
                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            list
                        )
                        binding.autoCompleteSpecies.setAdapter(adapter)
                        binding.autoCompleteSpecies.setText(initialFilter.petSpecies, false)
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.layoutFilterUser.visibility =
            if (initialFilter.type == FilterType.USER) View.VISIBLE else View.GONE
        binding.layoutFilterPet.visibility =
            if (initialFilter.type == FilterType.PET) View.VISIBLE else View.GONE
        binding.layoutFilterPost.visibility =
            if (initialFilter.type == FilterType.POST) View.VISIBLE else View.GONE

        when (initialFilter.type) {
            FilterType.USER -> {
                binding.etFilterPhone.setText(initialFilter.userPhoneNumber)
                if (initialFilter.userGender == getString(R.string.male)) binding.rbUserMale.isChecked = true
                else if (initialFilter.userGender == getString(R.string.female)) binding.rbUserFemale.isChecked = true
            }

            FilterType.POST -> {
                binding.rbPostRecent.isChecked = true
            }

            else -> {}
        }
    }

    private fun collectFilterData(): SearchFilter {
        return initialFilter.copy().apply {
            when (type) {
                FilterType.USER -> {
                    userPhoneNumber =
                        binding.etFilterPhone.text.toString().trim().takeIf { it.isNotEmpty() }
                    userGender = when {
                        binding.rbUserMale.isChecked -> requireContext().getString(R.string.male)
                        binding.rbUserFemale.isChecked -> requireContext().getString(R.string.female)
                        else -> null
                    }
                }

                FilterType.POST -> {
                    postSortBy = "created_at"
                }

                FilterType.PET -> {
                    petSpecies = binding.autoCompleteSpecies.text.toString().trim()
                        .takeIf { it.isNotEmpty() }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}