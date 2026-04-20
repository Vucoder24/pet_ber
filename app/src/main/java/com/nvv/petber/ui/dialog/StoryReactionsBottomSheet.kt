package com.nvv.petber.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nvv.petber.databinding.LayoutBottomSheetReactionsBinding
import com.nvv.petber.ui.activity.UserProfileActivity
import com.nvv.petber.ui.adapter.StoryReactionAdapter
import com.nvv.petber.viewmodel.ViewStoryViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoryReactionsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: LayoutBottomSheetReactionsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ViewStoryViewModel by viewModels({ requireParentFragment() })

    var onDismissCallback: (() -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = LayoutBottomSheetReactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = StoryReactionAdapter { userId ->
            UserProfileActivity.start(requireContext(), userId)
        }

        binding.rvReactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReactions.adapter = adapter

        val groupedDetails = viewModel.uiState.value.reactionSummary.groupedDetails

        if (groupedDetails.isEmpty()) {
            binding.rvReactions.visibility = View.GONE
            binding.tvEmpty.visibility = View.VISIBLE
        } else {
            binding.rvReactions.visibility = View.VISIBLE
            binding.tvEmpty.visibility = View.GONE
            adapter.submitList(groupedDetails)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback?.invoke()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): StoryReactionsBottomSheet {
            return StoryReactionsBottomSheet()
        }
    }
}