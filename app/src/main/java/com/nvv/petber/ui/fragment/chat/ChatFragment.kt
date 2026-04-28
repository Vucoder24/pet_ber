package com.nvv.petber.ui.fragment.chat

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.nvv.petber.R
import com.nvv.petber.data.model.ConversationEntity
import com.nvv.petber.databinding.FragmentChatBinding
import com.nvv.petber.ui.activity.ChatDetailActivity
import com.nvv.petber.ui.adapter.ConversationAdapter
import com.nvv.petber.ui.dialog.SearchChatDialogFragment
import com.nvv.petber.viewmodel.ChatListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChatFragment : Fragment() {

    private val viewModel: ChatListViewModel by viewModels()
    private lateinit var adapter: ConversationAdapter
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = ConversationAdapter(
            onClick = {
                val intent = Intent(requireContext(), ChatDetailActivity::class.java).apply {
                    putExtra(ChatDetailActivity.CONVERSATION_ID, it.conversationId)
                    putExtra("other_user_id", it.otherUserId)
                    putExtra("other_name", it.otherUserName)
                    putExtra("other_avatar", it.otherUserAvatar)
                }
                startActivity(intent)
            },
            onLongClick = { showDeleteBottomSheet(it) }
        )
        binding.rvConversations.adapter = adapter
        binding.rvConversations.layoutManager = LinearLayoutManager(requireContext())

        // Load more
        binding.rvConversations.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (!recyclerView.canScrollVertically(1)) {
                    viewModel.loadConversations()
                }
            }
        })
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadConversations(isRefresh = true)
        }

        binding.edtSearch.setFocusable(false)
        binding.edtSearch.setOnClickListener {
            val searchDialog = SearchChatDialogFragment()
            searchDialog.show(childFragmentManager, "SearchChat")
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.conversations.collect { list ->
                        adapter.submitList(list)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.swipeRefresh.isRefreshing = isLoading
                    }
                }
            }
        }
    }

    private fun showDeleteBottomSheet(conversation: ConversationEntity) {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_chat_option, null)
        bottomSheetDialog.setContentView(view)

        view.findViewById<LinearLayout>(R.id.btnDeleteChat).apply {
            setOnClickListener {
                bottomSheetDialog.dismiss()
                showConfirmDeleteDialog(conversation.conversationId)
            }
        }
        bottomSheetDialog.show()
    }

    private fun showConfirmDeleteDialog(conversationId: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(R.string.dialog_delete_message)
            .setPositiveButton(R.string.action_delete) { _, _ ->
                viewModel.deleteConversation(conversationId)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}