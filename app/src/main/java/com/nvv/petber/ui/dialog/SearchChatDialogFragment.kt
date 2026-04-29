package com.nvv.petber.ui.dialog

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.nvv.petber.databinding.DialogSearchChatBinding
import com.nvv.petber.ui.activity.ChatDetailActivity
import com.nvv.petber.ui.adapter.SearchUserChatAdapter
import com.nvv.petber.viewmodel.ChatListViewModel
import kotlinx.coroutines.launch

class SearchChatDialogFragment : DialogFragment() {

    private var _binding: DialogSearchChatBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ChatListViewModel by viewModels({ requireParentFragment() })
    private lateinit var searchAdapter: SearchUserChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogSearchChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        searchAdapter = SearchUserChatAdapter { conversation ->
            val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
                putExtra(ChatDetailActivity.CONVERSATION_ID, conversation.conversationId)
                putExtra("other_user_id", conversation.otherUserId)
                putExtra("other_name", conversation.otherUserName)
                putExtra("other_avatar", conversation.otherUserAvatar)
            }
            startActivity(intent)
            dismiss()
        }

        binding.rvSearchResults.apply {
            adapter = searchAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        binding.btnBack.setOnClickListener { dismiss() }

        binding.edtSearch.addTextChangedListener {
            val query = it.toString()
            if (query.isNotEmpty()) {
                viewModel.search(query)
            }
        }

        lifecycleScope.launch {
            viewModel.searchResults.collect {
                searchAdapter.submitList(it)
            }
        }

        binding.edtSearch.requestFocus()
    }

    override fun onDismiss(dialog: DialogInterface) {
        viewModel.clearSearch()
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}