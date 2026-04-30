package com.nvv.petber.ui.dialog

import android.app.DatePickerDialog
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
import java.util.Calendar

class SearchFilterBottomSheet(
    private val initialFilter: SearchFilter,
    private val onApply: (SearchFilter) -> Unit,
) : BottomSheetDialogFragment() {

    private var _binding: LayoutSearchFilterBinding? = null
    private val viewModel: SearchViewModel by viewModels({ requireParentFragment() })
    private val binding get() = _binding!!
    private var selectedDateFrom: String? = initialFilter.postDateFrom
    private var selectedDateTo: String? = initialFilter.postDateTo

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
                binding.etFilterAddress.setText(initialFilter.userAddress)
                if (initialFilter.userGender == getString(R.string.male)) binding.rbUserMale.isChecked =
                    true
                else if (initialFilter.userGender == getString(R.string.female)) binding.rbUserFemale.isChecked =
                    true
            }

            FilterType.POST -> {
                binding.cbPostRecent.isChecked = initialFilter.postSortBy != null
                updateDateFromDisplay()
                updateDateToDisplay()

                // Date picker listeners
                binding.btnPickDateFrom.setOnClickListener { showDatePicker(isFrom = true) }
                binding.tvDateFrom.setOnClickListener { showDatePicker(isFrom = true) }

                binding.btnPickDateTo.setOnClickListener { showDatePicker(isFrom = false) }
                binding.tvDateTo.setOnClickListener { showDatePicker(isFrom = false) }

                binding.tvClearDates.setOnClickListener {
                    selectedDateFrom = null
                    selectedDateTo = null
                    updateDateFromDisplay()
                    updateDateToDisplay()
                }
            }

            else -> {}
        }
    }

    private fun showDatePicker(isFrom: Boolean) {
        val calendar = Calendar.getInstance()

        val existing = if (isFrom) selectedDateFrom else selectedDateTo
        existing?.let {
            try {
                val parts = it.split("-")
                calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
            } catch (_: Exception) {
            }
        }

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                // Format: yyyy-MM-dd
                val formatted = "%04d-%02d-%02d".format(year, month + 1, day)
                if (isFrom) {
                    selectedDateFrom = formatted
                    updateDateFromDisplay()
                } else {
                    selectedDateTo = formatted
                    updateDateToDisplay()
                }
                updateClearButtonVisibility()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun String.toDisplayDate(): String {
        return try {
            val parts = this.split("-")
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } catch (_: Exception) {
            this
        }
    }

    private fun updateDateFromDisplay() {
        binding.tvDateFrom.text = selectedDateFrom?.toDisplayDate() ?: ""
        binding.tvDateFrom.hint =
            if (selectedDateFrom == null) getString(R.string.select_date) else ""
    }

    private fun updateDateToDisplay() {
        binding.tvDateTo.text = selectedDateTo?.toDisplayDate() ?: ""
        binding.tvDateTo.hint = if (selectedDateTo == null) getString(R.string.select_date) else ""
    }

    private fun updateClearButtonVisibility() {
        binding.tvClearDates.visibility =
            if (selectedDateFrom != null || selectedDateTo != null) View.VISIBLE else View.GONE
    }

    private fun collectFilterData(): SearchFilter {
        return initialFilter.copy().apply {
            when (type) {
                FilterType.USER -> {
                    userAddress =
                        binding.etFilterAddress.text.toString().trim().takeIf { it.isNotEmpty() }
                    userGender = when {
                        binding.rbUserMale.isChecked -> requireContext().getString(R.string.male)
                        binding.rbUserFemale.isChecked -> requireContext().getString(R.string.female)
                        else -> null
                    }
                }

                FilterType.POST -> {
                    postSortBy = "created_at"
                    postDateFrom = selectedDateFrom
                    postDateTo = selectedDateTo
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